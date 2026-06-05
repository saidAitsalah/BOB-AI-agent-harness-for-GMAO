# Architecture BorneFlow

## Vue d'ensemble

BorneFlow est un codebase synthétique qui reproduit volontairement les **patterns architecturaux publics d'IBM Maximo**, sans contenir de code propriétaire d'aucun client.

Le but est d'offrir aux outils d'IA agentique (Bob, Cursor, Claude Code) un terrain de test représentatif d'une customisation Maximo type, pour évaluer leur capacité à :
- Analyser l'impact d'une modification
- Générer du code conforme aux patterns
- Détecter des vulnérabilités de sécurité
- Produire de la documentation et des tests

## Domaine métier fictif

Le domaine modélisé est la **gestion d'une flotte de bornes de recharge électrique**. Aucun lien avec un projet réel.

### Entités métier

| Entité BorneFlow | Équivalent Maximo standard | Description |
|---|---|---|
| `ChargingStation` | `Asset` | Une borne de recharge physique |
| `MaintenanceOrder` | `Work Order` | Une opération de maintenance planifiée |
| `Technician` | `Person` / `Labor` | Un technicien de maintenance |

## Architecture en couches

L'architecture suit le modèle classique d'une customisation Maximo, en couches superposées avec responsabilités séparées.

### Couche 1 — API REST (style OSLC)

**Localisation** : `src/main/java/com/borneflow/api/`

Expose les ressources métier via JAX-RS (Jersey), dans un style proche de l'API OSLC native de Maximo. Les ressources sont identifiées par URI et accessibles via les verbes HTTP standard (GET, POST, PUT, DELETE).

**Convention** : les classes sont suffixées `Resource` (équivalent du suffixe Maximo `MboService` côté API).

### Couche 2 — Service

**Localisation** : `src/main/java/com/borneflow/service/`

Orchestration métier. C'est ici que la logique business est implémentée : validations, calculs, déclenchement de notifications, coordination de plusieurs MBOs.

**Convention** : les classes sont suffixées `Service`.

### Couche 3 — MBO (Maximo Business Objects)

**Localisation** : `src/main/java/com/borneflow/mbo/`

Modèle métier riche. Chaque MBO encapsule les données d'une entité ET son comportement (validations, transitions d'état, hooks de cycle de vie). Cette approche reflète le pattern Maximo où la logique métier vit principalement dans les MBOs.

**Hooks Maximo-like simulés** :
- `add()` : appelé lors de la création
- `save()` : appelé lors de la persistance
- `delete()` : appelé lors de la suppression
- `validate()` : appelé pour validation pré-save

**Convention** : les classes sont suffixées `Mbo`. Les collections sont suffixées `MboSet`.

### Couche 4 — DAO (Data Access Object)

**Localisation** : `src/main/java/com/borneflow/dao/`

Accès bas niveau aux données via JDBC. La base utilisée est H2 en mémoire (purement pour la démo locale ; aucune donnée n'est persistée).

**Convention** : les classes sont suffixées `Dao`. **Toutes les requêtes doivent être paramétrées** (cf. `.bob/rules/security-rules.md`).

### Couche transverse — App Beans

**Localisation** : `src/main/java/com/borneflow/bean/`

Composants liés à la présentation, à l'image des App Beans de Maximo. Coordonnent l'affichage d'écrans complexes.

### Couche transverse — Automation Scripts

**Localisation** : `src/main/java/com/borneflow/automation/`

Scripts d'automatisation déclenchés sur événement (statut changé, sauvegarde, etc.), à l'image des Automation Scripts Maximo. Pour le POC, ils sont écrits en JavaScript et Python (langages typiquement supportés par Maximo).

### Couche transverse — Exceptions

**Localisation** : `src/main/java/com/borneflow/exception/`

Hiérarchie d'exceptions custom. **Toute exception métier doit être un `BorneflowException` ou un de ses descendants** (cf. `.bob/rules/error-handling.md`).

## Flux typique d'une opération

```
[Client HTTP]
     │
     ▼
[Resource REST]  ←── validation des params HTTP
     │
     ▼
[Service]  ←── orchestration, règles métier
     │
     ▼
[MBO]  ←── validation métier, état
     │
     ▼
[DAO]  ←── persistance (requête SQL paramétrée)
     │
     ▼
[H2 in-memory DB]
```

## Stack technique

| Composant | Technologie | Version |
|---|---|---|
| Langage | Java | 17 |
| Build | Maven | 3.8+ |
| Logging | SLF4J + Logback | 2.x / 1.5.x |
| API REST | JAX-RS / Jersey | 3.1.x |
| Tests | JUnit 5 + Mockito | 5.10 / 5.12 |
| Base de données (démo) | H2 in-memory | 2.2 |

## Choix de conception assumés

### Pourquoi Java 17 ?

Maximo Application Suite (MAS 8/9) cible Java 11+, et la migration vers Java 17 est en cours dans l'écosystème IBM. Utiliser Java 17 permet de tester la capacité des outils agentiques à exploiter les fonctionnalités modernes du langage (records, switch expressions, var, pattern matching).

### Pourquoi pas de Spring ?

Maximo n'utilise pas Spring nativement. Le projet reste donc volontairement minimaliste, sans framework d'injection de dépendances lourd, pour rester représentatif.

### Pourquoi H2 et pas une vraie BDD ?

Le POC est destiné à un PC isolé, sans accès réseau ni base externe. H2 en mémoire permet aux DAO d'être testables sans dépendance externe. **Aucune donnée n'est persistée** entre deux exécutions.

## Limites volontaires

- Le projet **ne compile pas l'intégralité** d'une vraie customisation Maximo (qui nécessiterait les classes propriétaires `psdi.mbo.*` d'IBM, non redistribuables). Les MBOs `BorneFlow` simulent ces patterns sans dépendre des JARs propriétaires.
- Les automation scripts sont des **fichiers texte** documentant la logique attendue, sans intégration runtime (pas de moteur d'exécution dans le POC).
- La couverture de tests est **volontairement incomplète** pour permettre aux outils de proposer des tests manquants.

---

*Pour les conventions de codage détaillées, voir `docs/conventions.md` et `.bob/rules/`.*
