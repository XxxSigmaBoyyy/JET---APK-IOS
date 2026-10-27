#!/usr/bin/env python3
"""Semantic diff of two JET bundles (Python apk2bundle vs Java jet.apk.ApkBundler).
Compares app.properties, values.xml entries, the file list and every XML file (namespace-aware tree compare).
Usage: bundle_diff.py A B  -> prints mismatches, exit 1 if any."""
import os, sys
from lxml import etree

def props(d):
    r = {}
    p = os.path.join(d, "app.properties")
    for ln in open(p, encoding="utf-8"):
        ln = ln.rstrip("\n")
        if not ln or "=" not in ln: continue
        i = 0
        while i < len(ln):
            if ln[i] == "\\": i += 2; continue
            if ln[i] == "=": break
            i += 1
        r[ln[:i].replace("\\", "")] = ln[i+1:]
    return r

def files(d):
    out = set()
    for root, ds, fs in os.walk(d):
        rel = os.path.relpath(root, d)
        if rel.split(os.sep)[0] in ("fwcolor", "fwdrawable"): continue
        fs = [f for f in fs if not (rel == "." and f == "icon-1024.png")]  # iOS packaging icon (build-time only)
        for f in fs: out.add(os.path.normpath(os.path.join(rel, f)))
    return out

def canon(e):
    if not isinstance(e.tag, str): return None
    kids = [c for c in (canon(k) for k in e) if c is not None]
    t = (e.text or "").strip()
    return (e.tag, tuple(sorted(e.attrib.items())), t, tuple(kids))

def xml(p):
    return canon(etree.parse(p, etree.XMLParser(recover=True, huge_tree=True)).getroot())

def diff(a, b, out):
    n = 0
    pa, pb = props(a), props(b)
    for k in sorted(set(pa) | set(pb)):
        if pa.get(k) != pb.get(k): out.append("app.properties %s: %r != %r" % (k, pa.get(k), pb.get(k))); n += 1
    va, vb = xml(os.path.join(a, "values.xml")), xml(os.path.join(b, "values.xml"))
    def ent(v): return {(c[0], dict(c[1]).get("id"), dict(c[1]).get("name")): c for c in v[3]}
    ea, eb = ent(va), ent(vb)
    for k in sorted(set(ea) | set(eb), key=str):
        x, y = ea.get(k), eb.get(k)
        if x != y:
            # vector-in-layer file names may differ only by path; compare kind + attrs except vector path
            out.append("values %s:\n   py  %s\n   jv  %s" % (k, str(x)[:400], str(y)[:400])); n += 1
    fa, fb = files(a), files(b)
    for f in sorted(fa ^ fb): out.append("file only in %s: %s" % ("py" if f in fa else "java", f)); n += 1
    for f in sorted(fa & fb):
        if f.endswith(".xml") and f not in ("values.xml",):
            try:
                if xml(os.path.join(a, f)) != xml(os.path.join(b, f)): out.append("xml differs: " + f); n += 1
            except Exception as ex: out.append("xml parse %s: %s" % (f, ex)); n += 1
        elif not f.endswith(".xml") and f != "app.properties":
            if open(os.path.join(a, f), "rb").read() != open(os.path.join(b, f), "rb").read():
                if f == "fwattrs.properties":
                    if sorted(open(os.path.join(a, f)).read().split()) == sorted(open(os.path.join(b, f)).read().split()): continue
                out.append("bytes differ: " + f); n += 1
    return n

if __name__ == "__main__":
    out = []
    n = diff(sys.argv[1], sys.argv[2], out)
    print("\n".join(out[:int(os.environ.get("MAXLINES", "60"))]))
    print("%d mismatches" % n)
    sys.exit(1 if n else 0)
