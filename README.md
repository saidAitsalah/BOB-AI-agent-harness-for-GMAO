# BorneFlow

## Présentation

BorneFlow est une base de code Java synthétique reproduisant l'architecture
d'une application Maximo de gestion de la maintenance d'actifs. Elle a été
conçue pour servir de terrain d'expérimentation à l'évaluation d'un agent de
développement assisté par intelligence artificielle (IBM Bob), dans le cadre
d'un projet de migration vers MAS 9.2.

Le dépôt ne contient pas seulement le code applicatif : il regroupe également
l'ensemble de l'outillage développé pour donner à l'agent un accès structuré
au contexte du projet, ainsi que les journaux d'exécution issus des campagnes
de mesure.

Deux propriétés du dépôt méritent d'être soulignées avant toute prise en
main. D'abord, la base de code est **synthétique et privée** : elle n'a
jamais été publiée, ce qui garantit qu'aucun modèle de langage n'a pu
l'observer pendant son entraînement. Ensuite, elle contient **sept défauts
injectés délibérément**, dont la nature et la localisation sont connues,
et qui constituent la vérité terrain des évaluations conduites sur ce
dépôt.

---

## Structure du dépôt

```
borneflow/
├── .bob/                          Configuration de l'agent
│   ├── mcp.json                   Déclaration des serveurs MCP
│   ├── custom_modes.yaml          Modes personnalisés (rôles de l'agent)
│   ├── rules/                     Conventions de projet communes à tous les modes
│   ├── rules-<mode>/               Conventions propres à un mode spécifique
│   └── blackboard.db              Index du Context Store (généré, non versionné)
│
├── src/main/java/com/borneflow/
│   ├── mbo/                       Objets métier (règles et transitions d'état)
│   ├── dao/                       Accès aux données
│   ├── service/                   Orchestration des traitements
│   └── resource/                  Points d'entrée exposés
│
├── src/main/resources/
│   └── application.properties     Configuration de l'application
│
├── tools/
│   ├── cactx/                     Context Store (index structurel du code)
│   │   ├── blackboard.py          Indexation et requêtes
│   │   ├── mcp_blackboard_server.py   Serveur MCP associé
│   │   ├── measure_gain.py        Mesure du gain d'invocations
│   │   └── demo/                  Base de démonstration réduite
│   │
│   └── bornflow-rag/               Base de connaissance documentaire
│       ├── corpus/                 Spécifications fonctionnelles sources (.docx)
│       ├── ingestion.py            Extraction, vectorisation, indexation
│       ├── mcp_server.py           Serveur MCP associé
│       └── chroma_db_bornflow/     Base vectorielle (générée, non versionnée)
│
└── runs/
    └── prompts/                    Journaux d'exécution exportés (.json)
```

---

## Les sept défauts injectés

Ces défauts constituent la référence utilisée pour évaluer la capacité de
détection de l'agent. Leur emplacement ne doit pas être modifié sans mettre
à jour les protocoles d'évaluation qui s'y réfèrent.

| Défaut | Catégorie | Emplacement |
|---|---|---|
| Injection SQL par concaténation | Sécurité | `TechnicianDao.findByMatricule()` |
| Matricule journalisé en clair | RGPD | `TechnicianLookupService` |
| Secrets codés en dur | Sécurité | `application.properties` |
| Contrôle d'accès manquant | Sécurité | `MaintenanceOrderResource.deleteOrder()` |
| Désérialisation non contrôlée | Sécurité | `MaintenanceOrderService.importFromPayload()` |
| Effet de bord non couvert par un test | Conception | `MaintenanceOrderMbo.reassignTechnician()` |
| Absorption silencieuse d'exception | Robustesse | `MaintenanceOrderService.tryFindOrCreate()` |

---

## Prérequis

- Java 17 ou supérieur, avec Maven
- Python 3.10 ou supérieur
- Node.js (requis par le serveur MCP GitHub)
- Git

Chaque outil Python fonctionne dans son propre environnement virtuel, afin
d'isoler ses dépendances de celles des autres composants du dépôt.

---

## Installation

### 1. Le code applicatif

```bash
cd borneflow
mvn clean install
```

### 2. Le Context Store

```bash
cd tools/cactx
python -m venv .venv
.venv/Scripts/activate          # ou : source .venv/bin/activate sous Linux/macOS
pip install mcp
```

### 3. La base de connaissance documentaire

```bash
cd tools/bornflow-rag
python -m venv .venv
.venv/Scripts/activate
pip install python-docx langchain-core langchain-chroma langchain-huggingface sentence-transformers chromadb "mcp==1.27.2"
```

La version du paquet `mcp` doit être alignée sur celle utilisée par
l'agent : l'interface de déclaration des outils a changé de manière
incompatible entre les versions majeures du SDK.

---

## Lancer les tests

### Tests unitaires du code Java

```bash
cd borneflow
mvn test
```

Les résultats attendus servent de référence : toute modification apportée
par l'agent doit être validée par une exécution complète de cette suite
avant d'être considérée comme correcte.

### Vérification du Context Store

Indexation initiale de la base de code :

```bash
cd tools/cactx
python blackboard.py index /chemin/vers/borneflow
```

Contrôle du résultat :

```bash
python blackboard.py stats
python blackboard.py query MaintenanceOrderService
```

Une requête sur un composant existant doit retourner son fichier, ses
symboles publics, ses dépendances et ses appelants. L'absence de résultat
signale un problème d'indexation à corriger avant toute utilisation par
l'agent.

### Vérification de la base documentaire

Indexation des spécifications fonctionnelles :

```bash
cd tools/bornflow-rag
python ingestion.py
```

Contrôle du résultat :

```bash
python -c "from mcp_server import VECTORSTORE; print(VECTORSTORE._collection.count())"
```

Le nombre retourné doit correspondre au nombre de règles de gestion
présentes dans les documents sources.

### Test manuel d'un serveur MCP

Chaque serveur peut être lancé isolément pour vérifier son démarrage avant
de le déclarer à l'agent :

```bash
python mcp_blackboard_server.py --root /chemin/vers/borneflow
```

Un serveur fonctionnel reste silencieux et ne rend pas la main : il attend
une connexion sur son entrée standard. Ce comportement est normal et ne
constitue pas un blocage. Toute exception survenant au démarrage est
consignée dans un fichier `*_crash.log` situé aux côtés du serveur, sans
jamais être écrite sur la sortie standard, celle-ci étant réservée au
protocole d'échange avec l'agent.

---

## Déclaration des serveurs auprès de l'agent

Les serveurs sont déclarés dans `.bob/mcp.json`. Deux règles doivent être
respectées pour éviter les échecs de connexion les plus fréquents :

- **utiliser des chemins absolus**, aussi bien pour l'interpréteur Python
  que pour le script du serveur ;
- **ne pas renseigner de champ `cwd`** : un chemin relatif combiné à un
  répertoire de travail non maîtrisé est la cause la plus courante
  d'échec de connexion.

```json
{
  "mcpServers": {
    "borneflow-ctx": {
      "command": "/chemin/absolu/vers/.venv/Scripts/python.exe",
      "args": ["/chemin/absolu/vers/tools/cactx/mcp_blackboard_server.py",
               "--root", "/chemin/absolu/vers/borneflow"]
    }
  }
}
```

Après toute modification de ce fichier, l'agent doit être entièrement
redémarré : les connexions aux serveurs sont établies au démarrage et ne
sont pas relues en cours de session.

---

## Journaux d'exécution

Le dossier `runs/prompts/` contient les exports au format JSON des
exécutions conduites sur ce dépôt. Chaque fichier documente, pour une
exécution donnée, la configuration active, la séquence complète des appels
d'outils et la décomposition du coût. Ces journaux constituent la matière
brute de toute analyse quantitative ; ils ne doivent pas être modifiés
après export.

---


Les fichiers `blackboard.db` et `chroma_db_bornflow/` sont des artefacts
reconstructibles à partir du code source et des documents de spécification.
Ils ne doivent pas être versionnés et figurent à ce titre dans le fichier
`.gitignore` du dépôt.

Les serveurs MCP `jira` et `github`, lorsqu'ils sont déclarés, nécessitent
des éléments d'authentification. Ceux-ci doivent être fournis par variable
d'environnement et ne jamais être inscrits en clair dans `mcp.json`, ce
fichier étant suivi par le gestionnaire de versions.

L'analyse de dépendances effectuée par le Context Store repose sur une
analyse lexicale du code, et non sur une analyse syntaxique complète. Les
génériques imbriqués, les classes internes et les mécanismes de réflexion
ne sont pas capturés : le graphe de dépendances produit constitue une borne
inférieure de l'impact réel d'une modification.
