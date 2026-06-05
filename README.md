# BorneFlow — POC synthétique pour évaluation IBM Bob

> Codebase fictif construit dans le cadre d'un projet de fin d'études CGI pour évaluer IBM Bob et d'autres outils d'IA agentique sur des tâches du cycle de développement logiciel.

---

## ⚠️ Important — Nature de ce projet

Ce projet est **entièrement synthétique** et fictif.

- ❌ **Aucun code réel** appartenant à la SNCF, à CGI ou à un autre client n'a été utilisé pour le concevoir.
- ❌ **Aucune donnée réelle** n'est manipulée. Les données métier sont inventées (matricules fictifs, identifiants de bornes inventés).
- ✅ Le projet reproduit **uniquement la structure et les patterns publics** d'IBM Maximo, tels que documentés publiquement par IBM (architecture MBO, accès aux données, automation scripts, intégration OSLC).
- ✅ Le **domaine métier est inventé** : gestion d'une flotte de bornes de recharge électrique pour véhicules. Aucune ressemblance avec un projet existant chez CGI ou ses clients.

Ce projet a été validé par le responsable sécurité de CGI avant son démarrage.

---

## 🎯 Objectif

Servir de **terrain d'évaluation reproductible** pour benchmarker IBM Bob (et un ou deux autres outils agentiques) sur des tâches représentatives du cycle de développement de customisations Maximo-like :

1. **Analyse d'impact** avant modification d'un composant
2. **Génération de code** cohérente avec les patterns du projet
3. **Revue de sécurité** pré-commit (détection de vulnérabilités)
4. **Traçabilité** des modifications

---

## 🏗️ Architecture

Le projet imite l'architecture en couches typique d'une customisation Maximo :

```
┌─────────────────────────────────────────────────────────┐
│  API REST (style OSLC) — JAX-RS / Jersey                │
│  → ChargingStationResource, MaintenanceOrderResource    │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  Service Layer — orchestration métier                   │
│  → ChargingStationService, MaintenanceOrderService, ... │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  MBO (Maximo Business Objects) — modèle métier          │
│  → ChargingStationMbo, MaintenanceOrderMbo, ...         │
└────────────────────┬────────────────────────────────────┘
                     │
┌────────────────────▼────────────────────────────────────┐
│  DAO — accès aux données (JDBC sur H2 embarqué)         │
│  → ChargingStationDao, MaintenanceOrderDao, ...         │
└─────────────────────────────────────────────────────────┘

  ┌──────────────────────────────────┐
  │  Automation Scripts (JS / Python)│  ← équivalent Maximo Automation Scripts
  └──────────────────────────────────┘
```

Voir `docs/architecture.md` pour le détail des choix de conception.

---

## 📁 Structure du projet

```
borneflow/
├── pom.xml                          # Configuration Maven, Java 17
├── README.md                        # Ce fichier
├── docs/
│   ├── architecture.md              # Documentation de l'architecture
│   └── conventions.md               # Conventions de codage (résumé)
├── src/main/java/com/borneflow/
│   ├── mbo/                         # Business Objects
│   ├── service/                     # Couche service
│   ├── dao/                         # Accès aux données
│   ├── api/                         # API REST
│   ├── bean/                        # App beans
│   ├── automation/                  # Automation scripts (JS/Python)
│   └── exception/                   # Exceptions custom
├── src/main/resources/
│   ├── application.properties
│   └── logback.xml
├── src/test/java/                   # Tests unitaires (couverture partielle)
└── .bob/
    └── rules/                       # Custom rules pour IBM Bob
```

---

## 🚀 Démarrage

### Prérequis
- Java 17+
- Maven 3.8+

### Compilation et tests
```bash
mvn clean compile
mvn test
```

### Lancement (placeholder — pas d'entrypoint réel pour le POC)
Ce projet est destiné à être ouvert dans un IDE pour les démonstrations Bob/Cursor/Claude Code. Il n'a pas vocation à être exécuté en production.

---

## 🎓 Pour les outils d'IA agentique (Bob, Cursor, Claude Code)

Avant toute interaction sur ce codebase, l'outil doit :

1. Lire le présent README pour comprendre la nature synthétique du projet
2. Consulter `docs/conventions.md` pour les patterns à respecter
3. Pour Bob spécifiquement : charger les règles dans `.bob/rules/`

Les tâches de benchmark sont décrites dans le document de protocole de benchmark (livré séparément).

---

## 🔍 Notes pour l'évaluateur du benchmark

Ce projet contient des défauts volontaires destinés à mesurer la capacité des outils d'IA à les détecter. La liste exhaustive et la localisation précise de ces défauts sont conservées **hors du dépôt** (dans le carnet de bord du benchmark) pour ne pas biaiser l'évaluation.

Les catégories couvertes incluent : sécurité (injection, secrets, contrôles d'accès, données personnelles), qualité (gestion d'erreurs, code mort, complexité), cohérence (conventions de nommage) et tests (couverture intentionnellement incomplète).

---

## 📄 Licence

Ce projet est un livrable interne de PFE. Aucune redistribution publique.

---

*Projet généré dans le cadre d'un PFE CGI — version 1.0.0-SNAPSHOT*
