#!/usr/bin/env python3
"""
blackboard.py — Shared Context Store pour BorneFlow.

Principe de conception, dérivé des mesures réelles du benchmark :
  - La fenêtre de contexte n'est PAS saturée (36K/270K = 13 %).
  - 88 % des tokens d'entrée sont des lectures de cache.
  - Chaque appel d'outil coûte ~10,7K tokens d'entrée (re-envoi du contexte
    accumulé à chaque tour de la boucle agentique).

=> Le levier n'est donc PAS de réduire la taille du contexte, mais de
   RÉDUIRE LE NOMBRE D'ÉTAPES. Une requête au blackboard doit répondre en
   UN appel à ce qui prendrait 8 à 13 `read_file`.

Ce module construit un index SQLite de la codebase (graphe de dépendances,
symboles, métadonnées) et l'invalide au hash de fichier.

Usage :
    python blackboard.py index /chemin/vers/borneflow
    python blackboard.py query MaintenanceOrderService
    python blackboard.py stats
"""
import sqlite3, hashlib, json, os, re, sys, time
from pathlib import Path

DB_PATH = Path(".bob") / "blackboard.db"

SCHEMA = """
CREATE TABLE IF NOT EXISTS files (
    id        INTEGER PRIMARY KEY,
    path      TEXT UNIQUE NOT NULL,
    sha256    TEXT NOT NULL,
    mtime     REAL NOT NULL,
    loc       INTEGER NOT NULL,
    package   TEXT,
    layer     TEXT,               -- mbo | dao | service | resource | config | other
    indexed_at REAL NOT NULL
);
CREATE TABLE IF NOT EXISTS symbols (
    id       INTEGER PRIMARY KEY,
    file_id  INTEGER NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    name     TEXT NOT NULL,
    kind     TEXT NOT NULL,       -- class | method | field
    line     INTEGER NOT NULL,
    signature TEXT,
    visibility TEXT
);
CREATE TABLE IF NOT EXISTS deps (
    id        INTEGER PRIMARY KEY,
    from_file INTEGER NOT NULL REFERENCES files(id) ON DELETE CASCADE,
    to_symbol TEXT NOT NULL,      -- nom de classe cible
    kind      TEXT NOT NULL,      -- import | call | extends | implements
    line      INTEGER
);
CREATE INDEX IF NOT EXISTS idx_deps_from ON deps(from_file);
CREATE INDEX IF NOT EXISTS idx_deps_to   ON deps(to_symbol);
CREATE INDEX IF NOT EXISTS idx_sym_file  ON symbols(file_id);
CREATE INDEX IF NOT EXISTS idx_sym_name  ON symbols(name);
CREATE TABLE IF NOT EXISTS meta (
    key TEXT PRIMARY KEY, value TEXT
);
"""

LAYER_RULES = [
    (r"Mbo\.java$",        "mbo"),
    (r"Dao\.java$",        "dao"),
    (r"Service\.java$",    "service"),
    (r"Resource\.java$",   "resource"),
    (r"\.properties$",     "config"),
    (r"\.xml$",            "config"),
]


def connect(root: Path) -> sqlite3.Connection:
    db = root / DB_PATH
    db.parent.mkdir(parents=True, exist_ok=True)
    con = sqlite3.connect(db)
    con.row_factory = sqlite3.Row
    con.executescript(SCHEMA)
    return con


def sha(p: Path) -> str:
    h = hashlib.sha256()
    with open(p, "rb") as f:
        for chunk in iter(lambda: f.read(8192), b""):
            h.update(chunk)
    return h.hexdigest()


def layer_of(path: str) -> str:
    for pat, lay in LAYER_RULES:
        if re.search(pat, path):
            return lay
    return "other"


# --- Parsing Java volontairement simple : regex, pas d'AST.
#     Suffisant pour BorneFlow ; à remplacer par javalang/tree-sitter
#     sur une vraie codebase Maximo.
RE_PKG     = re.compile(r"^\s*package\s+([\w.]+)\s*;", re.M)
RE_IMPORT  = re.compile(r"^\s*import\s+(?:static\s+)?([\w.]+)\s*;", re.M)
RE_CLASS   = re.compile(r"^\s*(?:public|protected|private)?\s*(?:final\s+|abstract\s+)?"
                        r"class\s+(\w+)(?:\s+extends\s+(\w+))?(?:\s+implements\s+([\w,\s]+))?", re.M)
RE_METHOD  = re.compile(r"^\s*(public|protected|private)\s+(?:static\s+)?(?:final\s+)?"
                        r"([\w<>\[\],\s]+?)\s+(\w+)\s*\(([^)]*)\)", re.M)


def index_file(con, root: Path, p: Path):
    rel = str(p.relative_to(root)).replace("\\", "/")
    digest = sha(p)
    cur = con.execute("SELECT id, sha256 FROM files WHERE path=?", (rel,))
    row = cur.fetchone()
    if row and row["sha256"] == digest:
        return row["id"], False                      # inchangé -> pas de re-parse

    try:
        text = p.read_text(encoding="utf-8", errors="replace")
    except Exception:
        return None, False
    loc = text.count("\n") + 1
    pkg_m = RE_PKG.search(text)
    pkg = pkg_m.group(1) if pkg_m else None

    if row:
        fid = row["id"]
        con.execute("UPDATE files SET sha256=?, mtime=?, loc=?, package=?, layer=?, indexed_at=? WHERE id=?",
                    (digest, p.stat().st_mtime, loc, pkg, layer_of(rel), time.time(), fid))
        con.execute("DELETE FROM symbols WHERE file_id=?", (fid,))
        con.execute("DELETE FROM deps WHERE from_file=?", (fid,))
    else:
        cur = con.execute(
            "INSERT INTO files(path,sha256,mtime,loc,package,layer,indexed_at) VALUES(?,?,?,?,?,?,?)",
            (rel, digest, p.stat().st_mtime, loc, pkg, layer_of(rel), time.time()))
        fid = cur.lastrowid

    if p.suffix == ".java":
        for m in RE_CLASS.finditer(text):
            line = text[:m.start()].count("\n") + 1
            con.execute("INSERT INTO symbols(file_id,name,kind,line,signature,visibility) VALUES(?,?,?,?,?,?)",
                        (fid, m.group(1), "class", line, m.group(0).strip()[:200], "public"))
            if m.group(2):
                con.execute("INSERT INTO deps(from_file,to_symbol,kind,line) VALUES(?,?,?,?)",
                            (fid, m.group(2), "extends", line))
            if m.group(3):
                for itf in m.group(3).split(","):
                    con.execute("INSERT INTO deps(from_file,to_symbol,kind,line) VALUES(?,?,?,?)",
                                (fid, itf.strip(), "implements", line))
        for m in RE_METHOD.finditer(text):
            line = text[:m.start()].count("\n") + 1
            con.execute("INSERT INTO symbols(file_id,name,kind,line,signature,visibility) VALUES(?,?,?,?,?,?)",
                        (fid, m.group(3), "method", line, m.group(0).strip()[:200], m.group(1)))
        for m in RE_IMPORT.finditer(text):
            fq = m.group(1)
            simple = fq.rsplit(".", 1)[-1]
            if not fq.startswith(("java.", "javax.")):
                line = text[:m.start()].count("\n") + 1
                con.execute("INSERT INTO deps(from_file,to_symbol,kind,line) VALUES(?,?,?,?)",
                            (fid, simple, "import", line))
        # appels : ClassName.method( ou new ClassName(
        for m in re.finditer(r"\b([A-Z]\w+)\s*(?:\.\w+\s*\(|\()", text):
            tgt = m.group(1)
            line = text[:m.start()].count("\n") + 1
            con.execute("INSERT INTO deps(from_file,to_symbol,kind,line) VALUES(?,?,?,?)",
                        (fid, tgt, "call", line))
    return fid, True


def index_all(root: Path, patterns=("**/*.java", "**/*.properties", "**/*.xml")):
    con = connect(root)
    seen, changed = set(), 0
    for pat in patterns:
        for p in root.glob(pat):
            if any(x in p.parts for x in (".git", "target", "build", "node_modules", ".bob")):
                continue
            fid, did = index_file(con, root, p)
            if fid: seen.add(fid)
            if did: changed += 1
    # purge des fichiers disparus
    if seen:
        q = ",".join("?" * len(seen))
        con.execute(f"DELETE FROM files WHERE id NOT IN ({q})", tuple(seen))
    con.execute("INSERT OR REPLACE INTO meta(key,value) VALUES('last_index',?)", (str(time.time()),))
    con.commit()
    n = con.execute("SELECT COUNT(*) c FROM files").fetchone()["c"]
    s = con.execute("SELECT COUNT(*) c FROM symbols").fetchone()["c"]
    d = con.execute("SELECT COUNT(*) c FROM deps").fetchone()["c"]
    con.close()
    return dict(files=n, symbols=s, deps=d, reparsed=changed)


# ===================== REQUÊTE — le cœur du gain =====================
def query_context(root: Path, component: str, include_snippets=False) -> dict:
    """
    Répond en UNE fois à ce qui nécessiterait 8 à 13 `read_file`.

    Renvoie : fichier cible, couche, symboles publics, dépendances sortantes,
    appelants entrants, et fichiers de test associés.
    """
    con = connect(root)
    out = {"component": component}

    f = con.execute(
        "SELECT * FROM files WHERE path LIKE ? ORDER BY LENGTH(path) LIMIT 1",
        (f"%{component}%",)).fetchone()
    if not f:
        con.close()
        return {"component": component, "error": "not found in blackboard index"}

    out["file"] = f["path"]
    out["layer"] = f["layer"]
    out["loc"] = f["loc"]
    out["package"] = f["package"]

    out["public_api"] = [
        {"name": r["name"], "kind": r["kind"], "line": r["line"], "signature": r["signature"]}
        for r in con.execute(
            "SELECT * FROM symbols WHERE file_id=? AND visibility='public' ORDER BY line", (f["id"],))
    ]

    out["depends_on"] = sorted({
        r["to_symbol"] for r in con.execute(
            "SELECT DISTINCT to_symbol FROM deps WHERE from_file=? AND kind IN ('import','extends','implements','call')",
            (f["id"],))
        if r["to_symbol"] not in ("String", "List", "Map", "Integer", "Override", "Exception")
    })

    cls = Path(f["path"]).stem
    out["called_by"] = [
        {"file": r["path"], "layer": r["layer"], "kind": r["kind"], "line": r["line"]}
        for r in con.execute(
            "SELECT DISTINCT fi.path, fi.layer, d.kind, d.line FROM deps d "
            "JOIN files fi ON fi.id=d.from_file WHERE d.to_symbol=? AND d.from_file<>? ORDER BY fi.path",
            (cls, f["id"]))
    ]

    out["tests"] = [r["path"] for r in con.execute(
        "SELECT path FROM files WHERE path LIKE '%test%' AND path LIKE ?", (f"%{cls}%",))]
    out["test_coverage_hint"] = "aucun test détecté" if not out["tests"] else f"{len(out['tests'])} fichier(s)"

    out["impact_summary"] = (
        f"{cls} ({f['layer']}) : {len(out['public_api'])} méthodes publiques, "
        f"{len(out['depends_on'])} dépendances sortantes, "
        f"{len(out['called_by'])} appelant(s) — {out['test_coverage_hint']}."
    )
    con.close()
    return out


def stats(root: Path) -> dict:
    con = connect(root)
    r = {
        "files": con.execute("SELECT COUNT(*) c FROM files").fetchone()["c"],
        "symbols": con.execute("SELECT COUNT(*) c FROM symbols").fetchone()["c"],
        "deps": con.execute("SELECT COUNT(*) c FROM deps").fetchone()["c"],
        "by_layer": {r["layer"]: r["c"] for r in con.execute(
            "SELECT layer, COUNT(*) c FROM files GROUP BY layer")},
    }
    m = con.execute("SELECT value FROM meta WHERE key='last_index'").fetchone()
    r["last_index"] = time.ctime(float(m["value"])) if m else None
    con.close()
    return r


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__); sys.exit(1)
    cmd = sys.argv[1]
    root = Path(sys.argv[2] if len(sys.argv) > 2 and cmd == "index" else ".").resolve()
    if cmd == "index":
        print(json.dumps(index_all(root), indent=2))
    elif cmd == "query":
        print(json.dumps(query_context(Path("."), sys.argv[2]), indent=2, ensure_ascii=False))
    elif cmd == "stats":
        print(json.dumps(stats(Path(".")), indent=2))
    else:
        print(__doc__)
