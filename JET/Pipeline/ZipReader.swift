import Compression
import Foundation

/// Minimal read-only ZIP reader (central directory + stored/deflate entries) for inspecting APKs.
/// ZIP64 archives are not supported; APKs above 4 GB are rejected.
struct ZipEntry {
    let name: String
    let method: UInt16
    let compressedSize: Int
    let uncompressedSize: Int
    let localHeaderOffset: Int
}

enum ZipError: LocalizedError {
    case notZip, unsupported, truncated, tooLarge
    var errorDescription: String? {
        switch self {
        case .notZip: return "The file is not a ZIP/APK archive."
        case .unsupported: return "Unsupported ZIP feature (ZIP64 or encrypted entry)."
        case .truncated: return "The archive is truncated."
        case .tooLarge: return "The entry is too large to extract."
        }
    }
}

enum ZipReader {
    private static func u16(_ b: [UInt8], _ o: Int) -> Int { Int(b[o]) | Int(b[o + 1]) << 8 }
    private static func u32(_ b: [UInt8], _ o: Int) -> Int { u16(b, o) | u16(b, o + 2) << 16 }

    private static func read(_ h: FileHandle, at offset: Int, count: Int) throws -> [UInt8] {
        try h.seek(toOffset: UInt64(offset))
        let d = try h.read(upToCount: count) ?? Data()
        guard d.count == count else { throw ZipError.truncated }
        return [UInt8](d)
    }

    static func entries(of url: URL) throws -> [ZipEntry] {
        let h = try FileHandle(forReadingFrom: url)
        defer { try? h.close() }
        let size = Int(try h.seekToEnd())
        guard size >= 22 else { throw ZipError.notZip }
        let tailLen = min(size, 22 + 65_535)
        let tail = try read(h, at: size - tailLen, count: tailLen)
        // End of central directory record: PK\5\6, searched backwards (a comment may follow it).
        var eocd = -1
        var i = tail.count - 22
        while i >= 0 {
            if tail[i] == 0x50, tail[i + 1] == 0x4B, tail[i + 2] == 0x05, tail[i + 3] == 0x06 { eocd = i; break }
            i -= 1
        }
        guard eocd >= 0 else { throw ZipError.notZip }
        let count = u16(tail, eocd + 10)
        let cdSize = u32(tail, eocd + 12)
        let cdOffset = u32(tail, eocd + 16)
        guard count != 0xFFFF, cdOffset != 0xFFFF_FFFF else { throw ZipError.unsupported }
        guard cdOffset + cdSize <= size else { throw ZipError.truncated }
        let cd = try read(h, at: cdOffset, count: cdSize)
        var out: [ZipEntry] = []
        out.reserveCapacity(count)
        var p = 0
        while p + 46 <= cd.count, out.count < count {
            guard u32(cd, p) == 0x0201_4B50 else { throw ZipError.notZip }
            let method = UInt16(u16(cd, p + 10))
            let csize = u32(cd, p + 20), usize = u32(cd, p + 24)
            let nameLen = u16(cd, p + 28), extraLen = u16(cd, p + 30), commentLen = u16(cd, p + 32)
            let lho = u32(cd, p + 42)
            guard p + 46 + nameLen <= cd.count else { throw ZipError.truncated }
            let name = String(decoding: cd[(p + 46)..<(p + 46 + nameLen)], as: UTF8.self)
            out.append(ZipEntry(name: name, method: method, compressedSize: csize, uncompressedSize: usize, localHeaderOffset: lho))
            p += 46 + nameLen + extraLen + commentLen
        }
        return out
    }

    /// Extracts one entry (method 0 = stored, 8 = deflate) up to `maxSize` bytes.
    static func extract(_ e: ZipEntry, from url: URL, maxSize: Int = 8 << 20) throws -> Data {
        guard e.uncompressedSize <= maxSize, e.compressedSize <= maxSize else { throw ZipError.tooLarge }
        let h = try FileHandle(forReadingFrom: url)
        defer { try? h.close() }
        let lh = try read(h, at: e.localHeaderOffset, count: 30)
        guard u32(lh, 0) == 0x0403_4B50 else { throw ZipError.notZip }
        let dataStart = e.localHeaderOffset + 30 + u16(lh, 26) + u16(lh, 28)
        let raw = try read(h, at: dataStart, count: e.compressedSize)
        switch e.method {
        case 0:
            return Data(raw)
        case 8:
            guard e.uncompressedSize > 0 else { return Data() }
            guard !raw.isEmpty else { throw ZipError.truncated }
            var dst = [UInt8](repeating: 0, count: e.uncompressedSize)
            let n = raw.withUnsafeBufferPointer { src in
                dst.withUnsafeMutableBufferPointer { d in
                    // COMPRESSION_ZLIB is raw DEFLATE (RFC 1951), which is what ZIP stores.
                    compression_decode_buffer(d.baseAddress!, d.count, src.baseAddress!, src.count, nil, COMPRESSION_ZLIB)
                }
            }
            guard n == e.uncompressedSize else { throw ZipError.truncated }
            return Data(dst)
        default:
            throw ZipError.unsupported
        }
    }
}
