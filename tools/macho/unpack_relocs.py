#!/usr/bin/env python3
"""Linux-host ELF prep (x86_64 and aarch64): unpack Android packed relocations so glibc's ld.so applies them.
- DT_ANDROID_RELA/RELASZ (APS2, 0x60000011/12) and DT_ANDROID_REL/RELSZ (0x6000000f/10, rejected: x86_64 uses RELA)
- DT_RELR/RELRSZ/RELRENT (36/35/37): glibc supports RELR only with a GLIBC_ABI_DT_RELR verneed, which our prep strips.
All decoded relocations are merged with any existing DT_RELA table into one new RELA table placed in a new PT_LOAD
(the PT_NOTE program header is repurposed). Android tags are retagged in place to DT_RELA/DT_RELASZ when no DT_RELA
exists; otherwise DT_RELA/RELASZ are repointed and the packed tags left as ignored OS tags. Prints a one-line summary."""
import struct, sys

DT_NULL, DT_RELA, DT_RELASZ, DT_RELAENT, DT_RELRSZ, DT_RELR, DT_RELRENT = 0, 7, 8, 9, 35, 36, 37
DT_RELACOUNT = 0x6ffffff9
DT_ANDROID_REL, DT_ANDROID_RELSZ, DT_ANDROID_RELA, DT_ANDROID_RELASZ = 0x6000000f, 0x60000010, 0x60000011, 0x60000012
DT_ANDROID_RELR, DT_ANDROID_RELRSZ, DT_ANDROID_RELRENT = 0x6fffe000, 0x6fffe001, 0x6fffe003
IGNORED_TAG = 0x60000013
PT_LOAD, PT_DYNAMIC, PT_NOTE = 1, 2, 4
RELATIVE_BY_MACHINE = {62: 8, 183: 1027}  # EM_X86_64 -> R_X86_64_RELATIVE, EM_AARCH64 -> R_AARCH64_RELATIVE

def sleb(buf, pos):
    r = 0; sh = 0
    while True:
        b = buf[pos]; pos += 1
        r |= (b & 0x7f) << sh; sh += 7
        if not (b & 0x80):
            if b & 0x40: r -= 1 << sh
            return r, pos

def aps2(buf):
    if buf[:4] != b'APS2': raise ValueError('not APS2')
    p = 4
    n, p = sleb(buf, p); off, p = sleb(buf, p)
    info = 0; addend = 0; out = []
    while len(out) < n:
        gsize, p = sleb(buf, p); gflags, p = sleb(buf, p)
        by_info, by_delta, by_addend, has_addend = gflags & 1, gflags & 2, gflags & 4, gflags & 8
        if by_delta: gdelta, p = sleb(buf, p)
        if by_info: info, p = sleb(buf, p)
        if has_addend and by_addend: a, p = sleb(buf, p); addend += a
        elif not has_addend: addend = 0
        for _ in range(gsize):
            if by_delta: off += gdelta
            else: d, p = sleb(buf, p); off += d
            if not by_info: info, p = sleb(buf, p)
            if has_addend and not by_addend: a, p = sleb(buf, p); addend += a
            out.append((off & 0xffffffffffffffff, info & 0xffffffffffffffff, addend))
    return out

def main(path):
    data = bytearray(open(path, 'rb').read())
    R_RELATIVE = RELATIVE_BY_MACHINE.get(struct.unpack_from('<H', data, 18)[0])
    if R_RELATIVE is None: print('unpack_relocs: unsupported e_machine in ' + path); return
    if data[:4] != b'\x7fELF' or data[4] != 2 or struct.unpack_from('<H', data, 18)[0] != 62: return
    phoff, = struct.unpack_from('<Q', data, 32); phentsize, phnum = struct.unpack_from('<HH', data, 54)
    phdrs = [list(struct.unpack_from('<IIQQQQQQ', data, phoff + i * phentsize)) for i in range(phnum)]
    loads = [ph for ph in phdrs if ph[0] == PT_LOAD]
    def v2o(va):
        for ph in loads:
            if ph[3] <= va < ph[3] + ph[5]: return va - ph[3] + ph[2]
        raise ValueError('vaddr %x not in file' % va)
    dyn = next(ph for ph in phdrs if ph[0] == PT_DYNAMIC)
    ents = []
    for i in range(dyn[5] // 16):
        t, v = struct.unpack_from('<qQ', data, dyn[2] + i * 16)
        ents.append([t, v, dyn[2] + i * 16])
        if t == DT_NULL: break
    tags = {e[0]: e for e in ents}
    relocs = []; what = []
    if DT_ANDROID_REL in tags: print('unpack_relocs: DT_ANDROID_REL on x86_64 unsupported'); return
    if DT_ANDROID_RELA in tags:
        o = v2o(tags[DT_ANDROID_RELA][1]); sz = tags[DT_ANDROID_RELASZ][1]
        r = aps2(bytes(data[o:o + sz])); relocs += r; what.append('APS2=%d' % len(r))
    for rt, rs in ((DT_RELR, DT_RELRSZ), (DT_ANDROID_RELR, DT_ANDROID_RELRSZ)):
        if rt in tags:
            o = v2o(tags[rt][1]); sz = tags[rs][1]; base = 0; n0 = len(relocs)
            for k in range(sz // 8):
                w, = struct.unpack_from('<Q', data, o + k * 8)
                addrs = []
                if not (w & 1): addrs = [w]; base = w + 8
                else:
                    for i in range(63):
                        if (w >> (i + 1)) & 1: addrs.append(base + i * 8)
                    base += 63 * 8
                for a in addrs:
                    implicit, = struct.unpack_from('<Q', data, v2o(a))
                    relocs.append((a, R_RELATIVE, implicit))
            what.append('RELR=%d' % (len(relocs) - n0))
    if not relocs: return
    if DT_RELA in tags:
        o = v2o(tags[DT_RELA][1]); sz = tags[DT_RELASZ][1]
        old = [struct.unpack_from('<QQq', data, o + k * 24) for k in range(sz // 24)]
        relocs = old + relocs; what.append('RELA=%d' % len(old))
    rel = [r for r in relocs if (r[1] & 0xffffffff) == R_RELATIVE]
    other = [r for r in relocs if (r[1] & 0xffffffff) != R_RELATIVE]
    relocs = rel + other
    table = b''.join(struct.pack('<QQq', o, i, a) for o, i, a in relocs)
    # new PT_LOAD from PT_NOTE slot, placed after the highest vaddr; keep LOADs ascending
    note_i = next((i for i, ph in enumerate(phdrs) if ph[0] == PT_NOTE), None)
    if note_i is None: print('unpack_relocs: no spare phdr (PT_NOTE) in ' + path); return
    page = 0x1000
    end_va = max(ph[3] + ph[6] for ph in loads)  # p_memsz (bss included)
    new_off = (len(data) + page - 1) & ~(page - 1)
    new_va = ((end_va + page - 1) & ~(page - 1)) + (new_off % page)
    new_va = (new_va + page - 1) & ~(page - 1)
    newph = [PT_LOAD, 4, new_off, new_va, new_va, len(table), len(table), page]
    del phdrs[note_i]
    last_load = max(i for i, ph in enumerate(phdrs) if ph[0] == PT_LOAD)
    phdrs.insert(last_load + 1, newph)
    for i, ph in enumerate(phdrs): struct.pack_into('<IIQQQQQQ', data, phoff + i * phentsize, *ph)
    # dynamic tags
    def put(e, t, v): e[0], e[1] = t, v; struct.pack_into('<qQ', data, e[2], t, v)
    if DT_RELA in tags:
        put(tags[DT_RELA], DT_RELA, new_va); put(tags[DT_RELASZ], DT_RELASZ, len(table))
        for t in (DT_ANDROID_RELA, DT_ANDROID_RELASZ):
            if t in tags: put(tags[t], IGNORED_TAG, 0)
    elif DT_ANDROID_RELA in tags:
        put(tags[DT_ANDROID_RELA], DT_RELA, new_va); put(tags[DT_ANDROID_RELASZ], DT_RELASZ, len(table))
    else:
        # RELR-only: reuse RELR/RELRSZ slots
        rt = DT_RELR if DT_RELR in tags else DT_ANDROID_RELR; rs = DT_RELRSZ if rt == DT_RELR else DT_ANDROID_RELRSZ
        put(tags[rt], DT_RELA, new_va); put(tags[rs], DT_RELASZ, len(table)); tags.pop(rt); tags.pop(rs)
    for t in (DT_RELR, DT_RELRSZ, DT_RELRENT, DT_ANDROID_RELR, DT_ANDROID_RELRSZ, DT_ANDROID_RELRENT):
        if t in tags and tags[t][0] == t: put(tags[t], IGNORED_TAG, 0)
    if DT_RELAENT in tags: put(tags[DT_RELAENT], DT_RELAENT, 24)
    else:
        print('unpack_relocs: no DT_RELAENT slot (defaulting to 24 is glibc behaviour)')
    if DT_RELACOUNT in tags: put(tags[DT_RELACOUNT], DT_RELACOUNT, len(rel))
    data += b'\0' * (new_off - len(data)) + table
    open(path, 'wb').write(data)
    print('unpacked %s -> RELA %d (%d relative) at 0x%x' % (','.join(what), len(relocs), len(rel), new_va))

if __name__ == '__main__':
    for p in sys.argv[1:]: main(p)
