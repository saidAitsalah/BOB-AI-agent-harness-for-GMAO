# Shared Context Store (Blackboard) — implémentation et protocole d'évaluation
### BorneFlow · serveur MCP stateful `borneflow-ctx`

---

## 1. Ce que tes données disent — et pourquoi ça change la conception

La recommandation initiale était : *« Bob garde toute la mémoire de la codebase dans sa fenêtre de contexte → utilisez un blackboard pour réduire la taille du contexte. »*

**Tes 27 runs réfutent cette prémisse :**

| Mesure | Valeur observée | Conséquence |
|---|---|---|
| Fenêtre de contexte occupée | **36K / 270K → 13 %** (max 60K = 22 %) | La fenêtre n'est **pas** saturée |
| Part du cache dans les tokens d'entrée | **88 %** (mode agent : 257K/293K) | Le prompt caching amortit **déjà** le contexte stable |
| Corrélation `nb_appels_outils` ↔ `tokens_entrée` | **0,695** | Le nombre d'étapes est un prédicteur fort |
| Régression | **tokens ≈ 10,7K × (nb appels) + 64,4K** | ← **le vrai mécanisme** |

**Diagnostic corrigé :** l'inflation ne vient pas d'une fenêtre pleine, mais de la **boucle agentique** — chaque appel d'outil re-transmet la conversation accumulée au modèle, à ~10,7K tokens le tour.

**Conséquence de conception :** un blackboard qui réduirait « la taille du contexte » n'apporterait rien (13 % d'occupation). Un blackboard qui **réduit le nombre d'étapes** attaque le vrai levier. Toute l'implémentation est construite pour cet objectif : **un petit nombre d'outils à très forte densité d'information**, pas beaucoup d'outils granulaires.

---

## 2. Résultat de la mesure sur codebase de démonstration

```
Composant                       sans BB          avec BB      gain
                          appels / tokens  appels / tokens
──────────────────────────────────────────────────────────────────
MaintenanceOrderService       6 /     311      1 /     242      +5
TechnicianLookupService       4 /     194      1 /     170      +3
MaintenanceOrderResource      4 /     234      1 /     204      +3
TechnicianDao                 4 /     194      1 /     210      +3
──────────────────────────────────────────────────────────────────
TOTAL                        18 appels         4 appels        +14
```

**Observation contre-intuitive à ne pas masquer :** sur `TechnicianDao`, la charge utile du blackboard (210 tokens) est **plus grosse** que la lecture directe des fichiers (194 tokens). Les fichiers de démonstration sont minuscules.

**C'est précisément la preuve que le levier n'est pas la taille de la charge utile, mais le nombre d'appels.** 18 → 4 appels, à 10,7K tokens l'appel, voilà le gain. Sur une vraie codebase Maximo (fichiers de 500 à 2000 lignes), l'écart de charge utile jouerait aussi en faveur du blackboard — mais il reste secondaire.

### Extrapolation sur un run T-03 réel

| | Actuel | Avec blackboard | Écart |
|---|---|---|---|
| Appels d'outils / run | 23,25 | 13,25 | **−10,00** |
| Tokens entrée / run | 313K | 206K | **−107K** |
| Coût entrée / run | $1,566 | $1,031 | **−$0,535** |
| Réduction relative | | | **−34,2 %** |

⚠️ **Ce chiffre est un plafond théorique, pas un résultat.** Trois réserves à écrire noir sur blanc dans le rapport :
1. Le coefficient 10,7K provient d'une régression à R ≈ 0,70 sur 27 runs — une tendance, pas une loi.
2. L'extrapolation **suppose que l'agent substitue réellement** 11 appels par 1. Rien ne le garantit : Bob peut interroger le blackboard **puis lire les fichiers quand même**. C'est un comportement fréquent.
3. Seule une campagne mesurée (T-07) peut transformer ce plafond en résultat.

---

## 3. Architecture

```
.bob/blackboard.db          ← SQLite, index de la codebase
   ├── files      (path, sha256, mtime, loc, package, layer)
   ├── symbols    (file_id, name, kind, line, signature, visibility)
   ├── deps       (from_file, to_symbol, kind, line)
   └── meta       (last_index)

mcp_blackboard_server.py    ← serveur MCP « borneflow-ctx »
   ├── get_component_context(component)  ← LE gain : 1 appel remplace ~11
   ├── get_impact_set(component, depth)  ← transitivité
   ├── refresh_index()                   ← invalidation par SHA-256
   └── blackboard_stats()                ← diagnostic
```

**Choix techniques et leurs raisons :**

| Choix | Raison |
|---|---|
| **SQLite plutôt que JSON** | Requêtes indexées sur le graphe de dépendances ; un JSON obligerait à tout charger en mémoire pour chaque requête — reproduisant le problème qu'on cherche à résoudre. |
| **Invalidation par SHA-256** | Le `mtime` seul produit des faux positifs (checkout git, touch). Seuls les fichiers réellement modifiés sont re-parsés. |
| **4 outils seulement** | Chaque outil MCP déclaré consomme du contexte dans **chaque** appel. Multiplier les outils granulaires irait contre l'objectif. |
| **Descriptions Purpose/Usage/Limitations** | Conforme à la grille de qualité des descriptions MCP. Chaque outil déclare explicitement ce qu'il ne fait **pas**. |
| **Parsing par regex** | Suffisant pour BorneFlow. **À remplacer par `javalang` ou `tree-sitter` sur une vraie codebase Maximo** — les regex ne gèrent ni les génériques imbriqués, ni les classes internes, ni les annotations complexes. |

---

## 4. Installation

```bash
# 1. Dépendances
pip install mcp

# 2. Indexation initiale (à la racine de BorneFlow)
python blackboard.py index .

# 3. Vérification
python blackboard.py stats
python blackboard.py query MaintenanceOrderService

# 4. Déclaration dans .bob/mcp.json — SANS TOUCHER aux 3 serveurs existants
```

```json
{
  "mcpServers": {
    "borneflow-ctx": {
      "command": "python",
      "args": ["mcp_blackboard_server.py", "--root", "."]
    }
  }
}
```

**Ajoute `.bob/blackboard.db` à `.gitignore`** — c'est un artefact dérivé, reconstructible par `refresh_index`.

---

## 5. ⛔ Intégrité expérimentale — à lire avant de brancher

**Ne remplace pas tes 3 serveurs MCP existants. N'y touche pas du tout.**

Tes 27 runs ont été exécutés avec la configuration actuelle. Modifier les serveurs MCP maintenant changerait **l'artefact sous test** : tous tes résultats deviendraient incomparables, et la Contribution 2 s'effondrerait — exactement comme le confond `agent`/`ask` que tu viens de corriger au prix de 4 runs supplémentaires.

`borneflow-ctx` est un **serveur additionnel**, à évaluer comme une **condition expérimentale déclarée a priori**.

### Protocole T-07 — Blackboard

| | |
|---|---|
| **Question** | Un contexte pré-indexé réduit-il le nombre d'étapes agentiques, et à quel coût de qualité ? |
| **Design** | Apparié sur les 4 variantes T-03 (mêmes composants, mode `agent`) |
| **Conditions** | (a) MCP actuel — les runs `ref` du 13/07 servent de référence<br>(b) MCP + `borneflow-ctx` — nouveaux runs |
| **Variable manipulée** | Disponibilité du blackboard. **Rien d'autre.** |
| **Variables gelées** | `.bob/rules/`, mode `agent`, prompts T-03 identiques, version du modèle |
| **Coût** | 4 runs ≈ $2,50 |

**Métriques primaires** (déjà extraites automatiquement par `extract_metrics.py`) :
- `n_tool_calls` — **l'hypothèse est ici** : baisse attendue de ~23 à ~13
- `input_tokens`, `cost_usd`, `ratio_CP`

**Métrique de validité indispensable :**
- **Taux de substitution effective** = 1 − (`read_file` avec BB ÷ `read_file` sans BB).
  Si Bob interroge le blackboard **et** lit les fichiers quand même, la substitution échoue et le gain est nul, voire négatif (un appel de plus). **C'est le risque principal.**

**Métriques de qualité — non négociables :**
- DDR contre la vérité terrain. Le blackboard ne renvoie **pas** les corps de méthode : si Bob ne lit plus `TechnicianDao.findByMatricule()`, **il ne verra plus l'injection SQL**.
- **C'est le vrai risque de ce design, et il faut le mesurer, pas l'espérer.** Un gain de 34 % de tokens qui ferait chuter la DDR de 4/4 à 1/4 serait un mauvais échange.

**Analyse :** identique au protocole existant — deltas appariés, IC bootstrap BCa, permutation exacte. À n = 4, p minimale = 0,125 : tendances, pas significativité.

---

## 6. Ce que ce travail apporte au mémoire

**Trois usages, par ordre de valeur :**

1. **Un résultat négatif documenté sur la prémisse.** Tu as mesuré que la fenêtre de contexte est occupée à 13 % et que le cache absorbe 88 % de l'entrée. La motivation habituelle du context engineering (« la fenêtre déborde ») **ne tient pas sur un agent commercial moderne avec prompt caching**. C'est une observation empirique publiable, et elle contredit une croyance répandue.

2. **Une identification du vrai mécanisme.** Le coût vient de la boucle agentique (10,7K tokens/appel), pas du volume de contexte. Cette régression est une contribution mesurable, extraite de tes propres données.

3. **Un artefact d'ingénierie évalué.** Le blackboard existe, il est testé, son gain théorique est chiffré et ses limites sont énoncées. Qu'il soit mesuré en T-07 ou laissé en perspectives, il démontre une capacité de conception.

**Positionnement par rapport à la littérature CA-MCP :** ces travaux comparent *MCP traditionnel vs MCP optimisé* et rapportent des gains (−67,8 % de latence, −60 % d'appels LLM). Ton T-07 poserait la même question sur un agent commercial et une codebase non contaminée. Une réplication — ou une divergence expliquée — vaut publication dans les deux cas.

⚠️ **Sur la formulation « CA-MCP prouve que… » :** un article unique ne *prouve* pas. Écris « des travaux sur le MCP context-aware rapportent que… ». Et vérifie la référence exacte (arXiv:2601.11595) avant citation — même prudence que pour l'épisode « CAID », ce framework inexistant cité dans le document de takeaways.

---

## 7. Fichiers livrés

| Fichier | Rôle |
|---|---|
| `blackboard.py` | Store SQLite + indexeur + requêtes. Utilisable en CLI. |
| `mcp_blackboard_server.py` | Serveur MCP `borneflow-ctx`, 4 outils. |
| `measure_gain.py` | Quantification du gain avec les coefficients empiriques. |
| `gain_report.json` | Résultats chiffrés + réserves. |
| `demo/` | Codebase de démonstration (reconstituée d'après tes 7 défauts) pour tester sans BorneFlow. |

**Prérequis avant T-07 :** la notation DDR des 27 runs existants doit être faite **d'abord**. Sans référence de qualité, tu ne pourras pas dire si le blackboard dégrade la détection — et c'est la seule question qui décide de son adoption.
