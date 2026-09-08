#!/usr/bin/env python3
"""
mcp_blackboard_server.py — Serveur MCP « borneflow-ctx » (stateful).
VERSION 3 — journalisation défensive dès la première ligne exécutable.

Contexte du correctif : les logs Bob montrent un échec IMMÉDIAT et
identique ("MCP error -32000: Connection closed") sur 8 tentatives de
connexion réparties sur plusieurs heures, alors que la configuration
mcp.json est structurellement identique à celle du serveur "borneflow"
qui, lui, se connecte systématiquement. Le spawn du process n'est donc
pas en cause : le crash a lieu À L'INTÉRIEUR du script, probablement
avant même d'atteindre asyncio.run(main()) — d'où l'échec du correctif
v2, qui n'enveloppait que la boucle principale.

Ce fichier enveloppe désormais TOUT — y compris les imports — dans un
bloc défensif qui écrit la trace complète dans un fichier journal
d'emplacement absolu, avant de laisser le process se terminer.

Lancement :
    python mcp_blackboard_server.py --root /chemin/vers/borneflow

Déclaration dans .bob/mcp.json (inchangée) :
    {
      "mcpServers": {
        "borneflow-ctx": {
          "command": "C:/.../python.exe",
          "args": ["mcp_blackboard_server.py", "--root", "C:/.../borneflow"],
          "cwd": "C:/.../borneflow/tools/cactx"
        }
      }
    }
"""
import sys
import os
import traceback
from pathlib import Path
from datetime import datetime

# ---------------------------------------------------------------------
# Journal de crash — chemin ABSOLU, calculé en tout premier, avant tout
# autre import qui pourrait échouer. C'est la seule garantie qu'un échec
# précoce (import mcp, import blackboard, erreur d'argparse) soit
# capturé quelque part.
# ---------------------------------------------------------------------
CRASH_LOG = Path(__file__).resolve().parent / "blackboard_mcp_crash.log"


def _log_crash(stage: str, exc: BaseException) -> None:
    """N'écrit jamais sur stdout : cela corromprait le flux JSON-RPC
    si un client MCP était en train d'y lire."""
    try:
        with open(CRASH_LOG, "a", encoding="utf-8") as f:
            f.write("\n" + "=" * 78 + "\n")
            f.write(f"[{datetime.now().isoformat()}] Échec à l'étape : {stage}\n")
            f.write(f"cwd au lancement : {os.getcwd()}\n")
            f.write(f"sys.executable   : {sys.executable}\n")
            f.write(f"sys.path[0]      : {sys.path[0] if sys.path else '(vide)'}\n")
            f.write(f"argv             : {sys.argv}\n")
            f.write("-" * 78 + "\n")
            traceback.print_exc(file=f)
    except Exception:
        # Si même l'écriture du log échoue (ex. dossier en lecture
        # seule), on ne peut plus rien faire de mieux que sortir
        # proprement en code d'erreur.
        pass


# ---------------------------------------------------------------------
# Étape 1 — imports tiers. C'est le suspect n°1 : si le paquet `mcp`
# installé expose une API différente de celle attendue (Server,
# stdio_server, Tool, TextContent ont bougé entre versions du SDK),
# l'ImportError/AttributeError se produit ICI, avant tout le reste.
# ---------------------------------------------------------------------
try:
    import argparse
    import asyncio
    import json

    import blackboard as bb

    from mcp.server import Server
    from mcp.server.stdio import stdio_server
    from mcp.types import Tool, TextContent
except BaseException as e:  # BaseException : capte aussi SystemExit éventuel
    _log_crash("imports (mcp / blackboard)", e)
    sys.exit(1)

ROOT = Path(".").resolve()


# ---------------------------------------------------------------------
# Étape 2 — construction de l'objet Server et déclaration des outils.
# Suspect n°2 : si la classe Tool/TextContent du SDK installé exige des
# champs différents (validation pydantic stricte selon la version),
# l'erreur se produit à la CRÉATION des objets Tool, potentiellement
# dès l'appel de list_tools() par le client — donc dans call_tool/
# list_tools eux-mêmes, protégés séparément plus bas.
# ---------------------------------------------------------------------
try:
    app = Server("borneflow-ctx")
except BaseException as e:
    _log_crash("Server(\"borneflow-ctx\")", e)
    sys.exit(1)


@app.list_tools()
async def list_tools():
    try:
        return [
            Tool(
                name="get_component_context",
                description=(
                    "Purpose: return the complete impact-analysis context for one "
                    "component in a single call — file location, layer, public API, "
                    "outgoing dependencies, incoming callers, and associated test files.\n"
                    "Usage: call this ONCE per component before any migration or "
                    "refactor analysis.\n"
                    "Limitations: reads a pre-built index, not live file content. "
                    "Call refresh_index first if the codebase changed."
                ),
                inputSchema={
                    "type": "object",
                    "properties": {
                        "component": {
                            "type": "string",
                            "description": "Simple class name, e.g. 'MaintenanceOrderService'.",
                        }
                    },
                    "required": ["component"],
                },
            ),
            Tool(
                name="get_impact_set",
                description=(
                    "Purpose: return the transitive set of components affected by a "
                    "change, in a single call.\n"
                    "Usage: use to assess blast radius before a change.\n"
                    "Limitations: reflection and dependency injection are not captured; "
                    "treat the result as a lower bound."
                ),
                inputSchema={
                    "type": "object",
                    "properties": {
                        "component": {"type": "string"},
                        "depth": {"type": "integer", "minimum": 1, "maximum": 3, "default": 2},
                    },
                    "required": ["component"],
                },
            ),
            Tool(
                name="refresh_index",
                description=(
                    "Purpose: re-index the codebase. Only changed files (SHA-256) are "
                    "re-parsed.\nUsage: call once per session, or after code changes.\n"
                    "Limitations: reads the working tree only, not git history."
                ),
                inputSchema={"type": "object", "properties": {}},
            ),
            Tool(
                name="blackboard_stats",
                description=(
                    "Purpose: report file/symbol/dependency counts and last index time.\n"
                    "Usage: diagnostic only, to verify the index is fresh.\n"
                    "Limitations: metadata only, no code content."
                ),
                inputSchema={"type": "object", "properties": {}},
            ),
        ]
    except BaseException as e:
        _log_crash("list_tools()", e)
        raise


def _impact_set(root: Path, component: str, depth: int) -> dict:
    seen, frontier, levels = set(), [component], {}
    for d in range(1, depth + 1):
        nxt = []
        for c in frontier:
            ctx = bb.query_context(root, c)
            if "error" in ctx:
                continue
            for caller in ctx.get("called_by", []):
                name = Path(caller["file"]).stem
                if name not in seen and name != component:
                    seen.add(name)
                    nxt.append(name)
                    levels.setdefault(d, []).append(
                        {"component": name, "layer": caller["layer"], "via": caller["kind"]})
        frontier = nxt
        if not frontier:
            break
    total = sum(len(v) for v in levels.values())
    return {
        "component": component,
        "depth": depth,
        "impact_by_level": levels,
        "total_affected": total,
        "summary": f"Modifier {component} affecte {total} composant(s) sur {len(levels)} niveau(x).",
        "caveat": "Borne inferieure : reflexion et injection de dependances non capturees.",
    }


@app.call_tool()
async def call_tool(name: str, arguments: dict):
    try:
        if name == "get_component_context":
            res = bb.query_context(ROOT, arguments["component"])
        elif name == "get_impact_set":
            res = _impact_set(ROOT, arguments["component"], arguments.get("depth", 2))
        elif name == "refresh_index":
            res = bb.index_all(ROOT)
        elif name == "blackboard_stats":
            res = bb.stats(ROOT)
        else:
            res = {"error": f"unknown tool: {name}"}
    except BaseException as e:
        _log_crash(f"call_tool({name})", e)
        res = {"error": str(e), "tool": name}
    # ensure_ascii=True : évite tout risque d'encodage sur un flux
    # stdout Windows dont le codepage ne serait pas UTF-8.
    return [TextContent(type="text", text=json.dumps(res, indent=2, ensure_ascii=True))]


async def main():
    async with stdio_server() as (r, w):
        await app.run(r, w, app.create_initialization_options())


if __name__ == "__main__":
    try:
        ap = argparse.ArgumentParser()
        ap.add_argument("--root", default=".")
        a = ap.parse_args()
        ROOT = Path(a.root).resolve()
    except BaseException as e:
        _log_crash("argparse", e)
        sys.exit(1)

    for stream_name in ("stdout", "stderr"):
        stream = getattr(sys, stream_name, None)
        if stream is not None and hasattr(stream, "reconfigure"):
            try:
                stream.reconfigure(encoding="utf-8")
            except Exception:
                pass

    try:
        asyncio.run(main())
    except BaseException as e:
        _log_crash("asyncio.run(main())", e)
        sys.exit(1)