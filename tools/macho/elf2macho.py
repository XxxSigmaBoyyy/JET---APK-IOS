#!/usr/bin/env python3
"""Pass 30 (iOS-relevant prototype): statically convert an arm64 Android ELF .so into a Mach-O arm64 MH_DYLIB.

iOS enforces W^X and forbids runtime codegen, so app native code must arrive as a *pre-linked* Mach-O image that dyld maps,
rebases and binds like any framework. The ELF's machine code is already final arm64; we re-wrap it without touching
instructions by keeping every ELF vaddr as the Mach-O vmaddr (so all PC-relative ADRP/ADR/BL encodings stay valid):

  ELF PT_LOAD R-X                 -> __TEXT segment r-x (Mach-O header written over the unused ELF ehdr area)
  ELF PT_LOAD RW                  -> __DATA segment rw- (vmsize > filesize = bss zero-fill)
  R_AARCH64_RELATIVE (RELA/RELR/APS2, after prep) -> addend written into the slot + rebase opcode (dyld adds the slide)
  GLOB_DAT/JUMP_SLOT/ABS64 vs import -> bind opcode (eager; ordinal 1 = our bionic shim dylib)
  ABS64/GLOB_DAT vs own symbol    -> slot := S+A, rebase
  DT_INIT_ARRAY / DT_FINI_ARRAY   -> S_MOD_INIT_FUNC_POINTERS / S_MOD_TERM_FUNC_POINTERS sections
  exported dynsyms                -> export trie + nlist (leading '_' added, Darwin C convention)
  symbol versioning               -> dropped (Mach-O has none; same policy as the glibc-host prep)

Refused (exit 2): TLS (PT_TLS / TLSDESC / TPREL / DTPMOD), IRELATIVE / IFUNC, TEXTREL, and images whose first
PT_LOAD does not cover va 0 (header must land in __TEXT). Needs macOS/Xcode for: code signing, a real
libSystem.B.dylib / .tbd (or our own libjetbionic.dylib built with cctools), chained fixups, and on-device dlopen.
"""
import sys, struct, io, os, subprocess, tempfile, shutil
from elftools.elf.elffile import ELFFile
from elftools.elf.dynamic import DynamicSection
from elftools.elf.sections import SymbolTableSection

HERE = os.path.dirname(os.path.abspath(__file__))
UNPACK = os.path.join(HERE, 'unpack_relocs.py')
PAGE = 0x4000
R_ABS64, R_GLOB_DAT, R_JUMP_SLOT, R_RELATIVE, R_IRELATIVE = 257, 1025, 1026, 1027, 1032
R_TLS = {1028, 1029, 1030, 1031}
DT = dict(NULL=0, RELA=7, RELASZ=8, JMPREL=23, PLTRELSZ=2, INIT_ARRAY=25, FINI_ARRAY=26,
          INIT_ARRAYSZ=27, FINI_ARRAYSZ=28, INIT=12, FINI=13, TEXTREL=22, FLAGS=30)

def uleb(v):
    o = bytearray()
    while True:
        b = v & 0x7f; v >>= 7
        if v: o.append(b | 0x80)
        else: o.append(b); return bytes(o)
def sleb(v):
    o = bytearray()
    while True:
        b = v & 0x7f; v >>= 7
        if (v == 0 and not (b & 0x40)) or (v == -1 and (b & 0x40)): o.append(b); return bytes(o)
        o.append(b | 0x80)
def uleb5(v):
    o = bytearray()
    for i in range(5):
        b = v & 0x7f; v >>= 7
        o.append(b | (0x80 if i < 4 else 0))
    return bytes(o)

MH_MAGIC_64, CPU_ARM64, MH_DYLIB = 0xfeedfacf, 0x0100000c, 6
MH_DYLDLINK, MH_TWOLEVEL = 0x4, 0x80
LC_SEGMENT_64, LC_SYMTAB, LC_DYSYMTAB, LC_LOAD_DYLIB, LC_ID_DYLIB = 0x19, 0x2, 0xb, 0xc, 0xd
LC_DYLD_INFO_ONLY, LC_BUILD_VERSION = 0x80000022, 0x32
S_REGULAR, S_MOD_INIT, S_MOD_TERM = 0x0, 0x9, 0xa
S_ATTR_CODE = 0x80000400
N_EXT, N_SECT, N_UNDF = 0x1, 0xe, 0x0
VM_R, VM_W, VM_X = 1, 2, 4
PLATFORM_IOS = 2
REBASE_SET_TYPE, REBASE_SET_SEG, REBASE_DO, REBASE_DONE = 0x10, 0x20, 0x50, 0x00
BIND_SET_ORD, BIND_SET_SYM, BIND_SET_TYPE, BIND_SET_SEG, BIND_SET_ADD, BIND_DO, BIND_DONE = 0x10, 0x40, 0x50, 0x70, 0x60, 0x90, 0x00

class Refuse(Exception): pass

def ensure_unpacked(path):
    """Apply the same packed-reloc unpack used by the glibc-host prep so we see plain RELA."""
    data = open(path, 'rb').read()
    # quick check for ANDROID_RELA / RELR tags
    if b'APS2' in data[:min(len(data), 0x40000)] or True:
        tmp = tempfile.NamedTemporaryFile(delete=False, suffix='.so')
        tmp.write(data); tmp.close()
        try:
            subprocess.check_output([sys.executable, UNPACK, tmp.name], stderr=subprocess.STDOUT)
            return tmp.name
        except subprocess.CalledProcessError:
            os.unlink(tmp.name); return path
    return path

def main(elf_path, out_path, shim_install='@rpath/libjetbionic.dylib', install_name=None):
    work = ensure_unpacked(elf_path)
    try:
        convert(work, out_path, shim_install, install_name or ('@rpath/' + os.path.basename(out_path)))
    finally:
        if work != elf_path: os.unlink(work)

def convert(elf_path, out_path, shim_install, install_name):
    raw = bytearray(open(elf_path, 'rb').read())
    elf = ELFFile(io.BytesIO(raw))
    if elf.get_machine_arch() != 'AArch64': raise Refuse('not arm64 ELF')
    for seg in elf.iter_segments():
        if seg['p_type'] == 'PT_TLS': raise Refuse('PT_TLS present (TLS out of scope for this prototype)')
        if seg['p_type'] == 'PT_DYNAMIC':
            pass
    dyn = next(s for s in elf.iter_sections() if isinstance(s, DynamicSection))
    tags = {}
    for t in dyn.iter_tags():
        tags[t.entry.d_tag] = t.entry.d_val
        if t.entry.d_tag == 'DT_TEXTREL' or (t.entry.d_tag == 'DT_FLAGS' and t.entry.d_val & 4):
            raise Refuse('TEXTREL (W^X-hostile)')
    loads = sorted([dict(off=s['p_offset'], va=s['p_vaddr'], filesz=s['p_filesz'],
                         memsz=s['p_memsz'], flags=s['p_flags'])
                    for s in elf.iter_segments() if s['p_type'] == 'PT_LOAD'], key=lambda s: s['va'])
    if not loads or loads[0]['va'] != 0:
        raise Refuse('first PT_LOAD does not cover va 0 (header must land in __TEXT)')
    text_segs = [s for s in loads if s['flags'] & 1]   # PF_X
    data_segs = [s for s in loads if not (s['flags'] & 1)]
    tlo, thi = 0, max(s['va'] + s['memsz'] for s in text_segs)
    dlo = min(s['va'] for s in data_segs) if data_segs else thi
    dhi = max(s['va'] + s['memsz'] for s in data_segs) if data_segs else thi
    if any(s['va'] > dlo for s in text_segs): raise Refuse('RX segment after RW (unsupported layout)')
    # Android NDK images are 4 KiB-aligned; arm64 Darwin pages are 16 KiB. Keep every vaddr (code is untouched), and
    # move the __TEXT/__DATA split down to the 16 KiB boundary below the first RW byte. That is only legal when the
    # bytes of the RX segment that end up in RW __DATA are not code (typically .rodata/.eh_frame/.note tail).
    split = dlo // PAGE * PAGE
    if split < thi:
        code = [sec.name for sec in elf.iter_sections()
                if sec['sh_flags'] & 4 and sec['sh_size'] and sec['sh_addr'] < thi and sec['sh_addr'] + sec['sh_size'] > split]
        if code:
            raise Refuse('code %s shares a 16 KiB page with RW data at 0x%x (needs the interpreter route)' % (code, split))
        print('note: 16K split at 0x%x; RX tail 0x%x..0x%x (non-code) moves into __DATA' % (split, split, thi), file=sys.stderr)
        thi = split
    dlo = split if data_segs else thi

    dynsym = next(s for s in elf.iter_sections() if s.name == '.dynsym')
    syms = list(dynsym.iter_symbols())

    relatives, binds, import_names = [], [], {}
    for sec in elf.iter_sections():
        if sec.name in ('.rela.dyn', '.rela.plt') and hasattr(sec, 'iter_relocations'):
            for r in sec.iter_relocations():
                typ, off, add, si = r['r_info_type'], r['r_offset'], r['r_addend'], r['r_info_sym']
                if typ in R_TLS: raise Refuse('TLS reloc type %d at 0x%x' % (typ, off))
                if typ == R_IRELATIVE: raise Refuse('IRELATIVE/IFUNC at 0x%x (runtime codegen)' % off)
                if typ == R_RELATIVE:
                    relatives.append((off, add))
                elif typ in (R_GLOB_DAT, R_JUMP_SLOT, R_ABS64):
                    nm = syms[si].name
                    if not nm:
                        relatives.append((off, add)); continue
                    if syms[si].entry.st_shndx == 'SHN_UNDEF':
                        binds.append((off, nm, add)); import_names.setdefault(nm, None)
                    else:
                        # own symbol: materialize S+A and rebase
                        relatives.append((off, syms[si].entry.st_value + add))
                else:
                    raise Refuse('unsupported reloc type %d at 0x%x' % (typ, off))

    exports = []
    for sym in syms:
        st = sym.entry.st_info
        if sym.name and st.bind in ('STB_GLOBAL', 'STB_WEAK') and sym.entry.st_shndx != 'SHN_UNDEF':
            exports.append((sym.name, sym.entry.st_value))

    init_va = tags.get('DT_INIT_ARRAY', 0); init_sz = tags.get('DT_INIT_ARRAYSZ', 0)
    fini_va = tags.get('DT_FINI_ARRAY', 0); fini_sz = tags.get('DT_FINI_ARRAYSZ', 0)

    def to_seg(va):
        if tlo <= va < thi: return 0, va - tlo
        if dlo <= va < dhi: return 1, va - dlo
        raise Refuse('reloc va 0x%x outside loads' % va)

    # Materialize RELATIVE addends into the image (dyld rebase then adds the slide on top)
    def poke(va, val):
        for s in loads:
            if s['va'] <= va < s['va'] + s['filesz']:
                o = s['off'] + (va - s['va']); struct.pack_into('<Q', raw, o, val & ((1<<64)-1)); return
        # bss / beyond filesz: will live in the data_buf we synthesize
    for va, add in relatives:
        poke(va, add)

    # ---- opcodes ----
    rb = bytearray([REBASE_SET_TYPE | 1])
    for va, _ in sorted(relatives):
        seg, off = to_seg(va)
        rb += bytes([REBASE_SET_SEG | seg]) + uleb(off)
        rb += bytes([REBASE_DO]) + uleb(1)
    rb += bytes([REBASE_DONE])
    bd = bytearray([BIND_SET_ORD | 1, BIND_SET_TYPE | 1])
    last_add = 0
    for va, nm, add in sorted(binds):
        seg, off = to_seg(va)
        bd += bytes([BIND_SET_SYM]) + nm.encode() + b'\0'
        if add != last_add: bd += bytes([BIND_SET_ADD]) + sleb(add); last_add = add
        bd += bytes([BIND_SET_SEG | seg]) + uleb(off) + bytes([BIND_DO])
    bd += bytes([BIND_DONE])

    # ---- export trie (flat root -> each name as one edge) ----
    # Darwin C convention: leading underscore on every C symbol.
    def build_export_trie(exps):
        if not exps: return b''
        terms, names = {}, []
        for nm, off in exps:
            dnm = '_' + nm
            payload = uleb(0) + uleb(off)  # KIND_REGULAR
            term = uleb(len(payload)) + payload + bytes([0])
            terms[dnm] = term; names.append(dnm)
        names.sort()
        root = bytearray(uleb(0)) + uleb(len(names))
        slots = []
        for nm in names:
            root += nm.encode() + b'\0'; slots.append(len(root)); root += b'\0'*5
        blob = bytearray(root); offs = {}
        for nm in names:
            offs[nm] = len(blob); blob += terms[nm]
        for i, nm in enumerate(names):
            struct.pack_into('5s', blob, slots[i], uleb5(offs[nm]))
        return bytes(blob)
    export_trie = build_export_trie(exports)

    # ---- nlist ----
    strtab = bytearray(b'\0')
    def addstr(s):
        o = len(strtab); strtab.extend(s.encode()+b'\0'); return o
    nlist = []
    for nm, va in sorted(exports):
        nlist.append((addstr('_'+nm), N_SECT|N_EXT, 1, 0, va))
    n_defs = len(nlist)
    for nm in sorted(import_names):
        nlist.append((addstr('_'+nm), N_UNDF|N_EXT, 0, 0, 0))
    n_undf = len(import_names)

    # ---- segment buffers (va-keyed) ----
    def fill(buf, base, lo, hi):
        for sg in loads:
            a, b = max(lo, sg['va']), min(hi, sg['va'] + sg['filesz'])
            if a < b: buf[a-base:b-base] = raw[sg['off'] + a - sg['va']:sg['off'] + b - sg['va']]
    text_buf = bytearray(thi - tlo); fill(text_buf, tlo, tlo, thi)
    data_buf = bytearray(dhi - dlo) if data_segs else bytearray(); fill(data_buf, dlo, dlo, dhi)
    # trailing bss is zero-fill: don't store it in the file
    filled = max([min(dhi, sg['va'] + sg['filesz']) for sg in loads if sg['va'] + sg['filesz'] > dlo] or [dlo])
    data_buf = data_buf[:max(0, filled - dlo)]

    # ---- load-command size ----
    def pad8(b): return b + b'\0'*((8 - len(b)%8)%8)
    idname = pad8(install_name.encode()+b'\0')
    shimname = pad8(shim_install.encode()+b'\0')
    # Non-overlapping __DATA sections: special arrays first, then the leftover as __data pieces.
    reserved = []
    if init_sz: reserved.append(('__mod_init_func', init_va, init_sz, S_MOD_INIT))
    if fini_sz: reserved.append(('__mod_term_func', fini_va, fini_sz, S_MOD_TERM))
    reserved.sort(key=lambda r: r[1])  # by va
    data_sects = []
    cursor = dlo
    for name, va, sz, fl in reserved:
        if cursor < va: data_sects.append(('__data', cursor, va - cursor, S_REGULAR))
        data_sects.append((name, va, sz, fl)); cursor = va + sz
    if cursor < dhi: data_sects.append(('__data', cursor, dhi - cursor, S_REGULAR))
    if not data_sects and data_segs: data_sects.append(('__data', dlo, dhi - dlo, S_REGULAR))
    # sections past the file-backed part are zero-fill (bss); split the one that straddles it
    S_ZEROFILL = 0x1; fend = dlo + len(data_buf); out_sects = []
    for name, va, sz, fl in data_sects:
        if va + sz <= fend: out_sects.append((name, va, sz, fl))
        elif va >= fend: out_sects.append(('__bss' if fl == S_REGULAR else name, va, sz, S_ZEROFILL if fl == S_REGULAR else fl))
        else:
            out_sects.append((name, va, fend - va, fl)); out_sects.append(('__bss', fend, va + sz - fend, S_ZEROFILL))
    data_sects = out_sects
    ndata_sects = len(data_sects)
    def scsz(n): return 72 + n*80
    ncmds_size = scsz(1) + scsz(ndata_sects) + scsz(0) + 48 + 24 + 80 + 24+len(idname) + 24+len(shimname) + 24
    ncmds = 9
    hdr_size = 32 + ncmds_size
    if hdr_size > len(text_buf):
        raise Refuse('header (%d) larger than __TEXT (%d)' % (hdr_size, len(text_buf)))

    text_fileoff = 0; text_filesize = len(text_buf)
    data_fileoff = (text_filesize + PAGE - 1) // PAGE * PAGE
    data_filesize = len(data_buf)
    linkedit_fileoff = (data_fileoff + data_filesize + PAGE - 1) // PAGE * PAGE

    le = bytearray()
    rebase_off = linkedit_fileoff + len(le); le += rb
    bind_off   = linkedit_fileoff + len(le); le += bd
    export_off = linkedit_fileoff + len(le); le += export_trie
    while len(le) % 8: le += b'\0'
    symtab_off = linkedit_fileoff + len(le)
    for strx, typ, sect, desc, val in nlist:
        le += struct.pack('<IBBHQ', strx, typ, sect, desc, val)
    strtab_off = linkedit_fileoff + len(le); le += strtab
    while len(le) % 8: le += b'\0'
    linkedit_filesize = len(le)

    out = io.BytesIO()
    out.write(struct.pack('<IiiIIIII', MH_MAGIC_64, CPU_ARM64, 0, MH_DYLIB, ncmds, ncmds_size, MH_DYLDLINK|MH_TWOLEVEL, 0))
    text_vmsize = (len(text_buf)+PAGE-1)//PAGE*PAGE
    out.write(struct.pack('<II16sQQQQiiII', LC_SEGMENT_64, scsz(1), b'__TEXT',
                          tlo, text_vmsize, text_fileoff, text_filesize, VM_R|VM_X, VM_R|VM_X, 1, 0))
    out.write(struct.pack('<16s16sQQIIIIIIII', b'__text', b'__TEXT', tlo+hdr_size, len(text_buf)-hdr_size,
                          hdr_size, 2, 0, 0, S_REGULAR|S_ATTR_CODE, 0, 0, 0))
    data_vmsize = max((dhi - dlo + PAGE-1)//PAGE*PAGE, PAGE if data_segs else 0)
    out.write(struct.pack('<II16sQQQQiiII', LC_SEGMENT_64, scsz(ndata_sects), b'__DATA',
                          dlo, data_vmsize, data_fileoff, data_filesize, VM_R|VM_W, VM_R|VM_W, ndata_sects, 0))
    for i, (name, va, sz, fl) in enumerate(data_sects):
        # fileoff for ZEROFILL-beyond-filesize regions still points into the file span we wrote
        fo = 0 if fl == 0x1 else data_fileoff + (va - dlo)
        out.write(struct.pack('<16s16sQQIIIIIIII', name.encode(), b'__DATA', va, sz,
                              fo, 3, 0, 0, fl, 0, 0, 0))
    out.write(struct.pack('<II16sQQQQiiII', LC_SEGMENT_64, scsz(0), b'__LINKEDIT',
                          dlo + data_vmsize, (linkedit_filesize+PAGE-1)//PAGE*PAGE,
                          linkedit_fileoff, linkedit_filesize, VM_R, VM_R, 0, 0))
    out.write(struct.pack('<IIIIIIIIIIII', LC_DYLD_INFO_ONLY, 48,
                          rebase_off, len(rb), bind_off, len(bd), 0, 0, 0, 0, export_off, len(export_trie)))
    out.write(struct.pack('<IIIIII', LC_SYMTAB, 24, symtab_off, len(nlist), strtab_off, len(strtab)))
    out.write(struct.pack('<II'+'I'*18, LC_DYSYMTAB, 80, 0,0, 0,n_defs, n_defs,n_undf, 0,0, 0,0, 0,0, 0,0, 0,0, 0,0))
    out.write(struct.pack('<IIIIII', LC_ID_DYLIB, 24+len(idname), 24, 0, 0x10000, 0x10000)); out.write(idname)
    out.write(struct.pack('<IIIIII', LC_LOAD_DYLIB, 24+len(shimname), 24, 0, 0x10000, 0x10000)); out.write(shimname)
    out.write(struct.pack('<IIIIII', LC_BUILD_VERSION, 24, PLATFORM_IOS, 14<<16, 14<<16, 0))
    assert out.tell() == hdr_size, (out.tell(), hdr_size)

    buf = bytearray(text_buf); buf[:hdr_size] = out.getvalue()
    final = io.BytesIO(); final.write(buf)
    final.write(b'\0'*(data_fileoff - final.tell()))
    final.write(data_buf)
    final.write(b'\0'*(linkedit_fileoff - final.tell()))
    final.write(le)
    open(out_path, 'wb').write(final.getvalue())
    print('wrote %s: %d B  text_loads=%d data_loads=%d  rebase=%d bind=%d import=%d export=%d  init=%dB fini=%dB' % (
        out_path, len(final.getvalue()), len(text_segs), len(data_segs),
        len(relatives), len(binds), len(import_names), len(exports), init_sz, fini_sz))

if __name__ == '__main__':
    try:
        main(*sys.argv[1:])
    except Refuse as e:
        print('REFUSED: %s' % e, file=sys.stderr); sys.exit(2)
