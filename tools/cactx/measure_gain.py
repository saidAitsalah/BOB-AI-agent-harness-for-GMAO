#!/usr/bin/env python3
"""
measure_gain.py — Quantifie le gain du blackboard sur la base des
coefficients empiriques mesurés sur les 27 runs réels.

Coefficients issus de runs_v2.csv :
  - tokens_entrée ≈ 10.7K × (nb appels d'outils) + 64.4K
  - condition « avec MCP » : 8.25 read_file + 2.75 analyze_dependencies
  - condition « sans MCP » : 13.50 read_file
"""
import json, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).parent))
import blackboard as bb

# --- Coefficients empiriques (mesurés, non supposés)
TOK_PER_CALL = 10_700       # régression sur 27 runs
BASE_TOK     = 64_400
COST_IN_PER_MTOK  = 5.00    # $/1M tokens entrée (Claude Opus 4.8, cf. tarif Copilot)
COST_OUT_PER_MTOK = 25.00

ROOT = Path("demo")
COMPONENTS = ["MaintenanceOrderService", "TechnicianLookupService",
              "MaintenanceOrderResource", "TechnicianDao"]


def payload_tokens(obj) -> int:
    """Approximation : 1 token ≈ 4 caractères pour du JSON."""
    return len(json.dumps(obj, ensure_ascii=False)) // 4


def simulate_readfile_path(root: Path, comp: str):
    """Ce que Bob doit lire SANS blackboard pour reconstituer le même contexte."""
    ctx = bb.query_context(root, comp)
    files = {ctx["file"]}
    for c in ctx.get("called_by", []):
        files.add(c["file"])
    # les dépendances sortantes -> il faut les localiser puis les lire
    for d in ctx.get("depends_on", []):
        r = bb.query_context(root, d)
        if "file" in r:
            files.add(r["file"])
    total_chars = 0
    for f in files:
        p = root / f
        if p.exists():
            total_chars += len(p.read_text(encoding="utf-8", errors="replace"))
    # + un glob pour localiser, + un grep pour les appelants
    n_calls = len(files) + 2
    return n_calls, total_chars // 4, sorted(files)


print("=" * 78)
print("GAIN DU BLACKBOARD — mesuré sur la codebase de démonstration")
print("=" * 78)
print(f"{'Composant':26}{'sans BB':>18}{'avec BB':>18}{'gain étapes':>14}")
print(f"{'':26}{'appels / tokens':>18}{'appels / tokens':>18}")
print("-" * 78)

tot_before_calls = tot_after_calls = 0
rows = []
for comp in COMPONENTS:
    n_calls, rf_tok, files = simulate_readfile_path(ROOT, comp)
    bb_ctx = bb.query_context(ROOT, comp)
    bb_tok = payload_tokens(bb_ctx)
    tot_before_calls += n_calls
    tot_after_calls += 1
    rows.append((comp, n_calls, rf_tok, 1, bb_tok, files))
    print(f"{comp:26}{n_calls:>6} / {rf_tok:>7}{1:>8} / {bb_tok:>7}"
          f"{n_calls - 1:>+13}")

print("-" * 78)
print(f"{'TOTAL':26}{tot_before_calls:>6} appels{tot_after_calls:>16} appel(s)"
      f"{tot_before_calls - tot_after_calls:>+13}")

# --- Extrapolation avec le coefficient empirique
print()
print("=" * 78)
print("EXTRAPOLATION sur le coût réel (coefficient empirique : 10,7K tok/appel)")
print("=" * 78)

# scénario T-03 réel : 23.25 appels d'outils en moyenne (avec MCP)
BASELINE_CALLS = 23.25
# le blackboard remplace les read_file (8.25) + analyze_deps (2.75) = 11 appels
# par 1 seul appel get_component_context
REPLACED = 11.0
NEW_CALLS = BASELINE_CALLS - REPLACED + 1

tok_before = TOK_PER_CALL * BASELINE_CALLS + BASE_TOK
tok_after  = TOK_PER_CALL * NEW_CALLS + BASE_TOK
cost_before = tok_before / 1e6 * COST_IN_PER_MTOK
cost_after  = tok_after / 1e6 * COST_IN_PER_MTOK

print(f"{'':32}{'actuel':>14}{'avec BB':>14}{'écart':>14}")
print("-" * 78)
print(f"{'Appels d''outils / run':32}{BASELINE_CALLS:>14.2f}{NEW_CALLS:>14.2f}"
      f"{NEW_CALLS - BASELINE_CALLS:>+14.2f}")
print(f"{'Tokens entrée / run':32}{tok_before/1000:>13.0f}K{tok_after/1000:>13.0f}K"
      f"{(tok_after-tok_before)/1000:>+13.0f}K")
print(f"{'Coût entrée / run ($)':32}{cost_before:>14.3f}{cost_after:>14.3f}"
      f"{cost_after-cost_before:>+14.3f}")
print(f"{'Réduction relative':32}{'':>14}{'':>14}"
      f"{100*(tok_after-tok_before)/tok_before:>+13.1f}%")

print()
print("⚠️  LIMITES DE CETTE EXTRAPOLATION")
print("   • Le coefficient 10,7K tok/appel vient d'une régression à R≈0,70 sur")
print("     27 runs — il capture une tendance, pas une loi exacte.")
print("   • Elle suppose que l'agent accepte réellement de remplacer 11 appels")
print("     par 1. Rien ne le garantit : il peut appeler le blackboard PUIS")
print("     lire les fichiers quand même. Seule une mesure réelle le dira.")
print("   • Le gain est un plafond théorique, pas un résultat.")

json.dump({
    "components": [{"component": r[0], "calls_before": r[1], "tokens_before": r[2],
                    "calls_after": r[3], "tokens_after": r[4], "files_read": r[5]}
                   for r in rows],
    "extrapolation": {
        "baseline_calls": BASELINE_CALLS, "new_calls": NEW_CALLS,
        "tokens_before": tok_before, "tokens_after": tok_after,
        "cost_before_usd": cost_before, "cost_after_usd": cost_after,
        "reduction_pct": 100*(tok_after-tok_before)/tok_before,
    },
    "caveats": [
        "Coefficient empirique R~0.70, tendance et non loi exacte",
        "Suppose que l'agent substitue effectivement les appels",
        "Plafond theorique, a valider par une campagne T-07",
    ],
}, open("gain_report.json", "w"), indent=2, ensure_ascii=False)
print("\n-> gain_report.json")
