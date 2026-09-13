# Mode Ask - Contexte documentaire non évident

Ce fichier contient les patterns de documentation spécifiques au projet, découverts par lecture du code source.

## Aspects contre-intuitifs de la structure du projet

### Nature POC synthétique
Ceci n'est PAS du code de production - il s'agit d'une base de code de benchmark construite délibérément pour évaluer des outils d'IA de codage. Elle contient des défauts INTENTIONNELS destinés à tester les capacités de détection.

### Simulation des patterns Maximo
Le projet imite l'architecture IBM Maximo SANS utiliser les bibliothèques Maximo réelles (qui sont propriétaires). Cela signifie que :
- les classes MBO simulent le comportement de `psdi.mbo.Mbo` sans en hériter
- il n'y a pas de runtime Maximo réel - uniquement les patterns
- une base H2 en mémoire remplace une base de données d'entreprise réelle

### Emplacements de la documentation

**Documentation principale** : le répertoire `docs/` contient :
- `architecture.md` - explication détaillée de l'architecture couche par couche
- `conventions.md` - résumé synthétique des conventions de codage

**Règles applicables** : le répertoire `.bob/rules/` contient :
- `error-handling.md` - patterns d'exceptions, codes d'erreur
- `naming-conventions.md` - nommage des classes par couche
- `mbo-access-patterns.md` - cycle de vie des MBO, patterns DAO
- `security-rules.md` - exigences de sécurité

### Les défauts intentionnels sont documentés
Le README indique explicitement que des défauts sont introduits volontairement pour l'évaluation en benchmark. Lors de la détection d'un problème :
- les violations de sécurité dans `application.properties` sont INTENTIONNELLES
- le préfixe `Bf` manquant sur certaines classes est INTENTIONNEL
- la couverture de tests incomplète est INTENTIONNELLE

## Responsabilités des couches (non évidentes)

### La couche MBO porte la logique métier
Contrairement aux architectures orientées services classiques, les MBO contiennent une part importante de la logique métier (validations, transitions d'état). Les Services orchestrent mais ne dupliquent pas la logique des MBO.

### Les DAO renvoient des records, pas des entités
Les DAO renvoient des types `record` immuables pour les opérations de lecture. La couche Service les transforme en MBO lorsque nécessaire. Ce n'est PAS le pattern JPA/Hibernate standard.

### Pas de framework d'injection de dépendances
Bien qu'il s'agisse de Java, il n'y a PAS de Spring/CDI. Les dépendances sont transmises via les constructeurs. Cela imite l'approche de Maximo.

## Organisation des tests

### Les tests reflètent la structure du code source
Les classes de test se trouvent dans `src/test/java/` et reflètent la structure de packages de `src/main/java/`. PAS dans un module de test séparé.

### La couverture partielle est intentionnelle
La couverture de tests est volontairement incomplète afin de permettre aux outils d'IA de proposer des tests manquants. Ne pas supposer que des tests manquants sont un oubli.

## Scripts d'automatisation

### Les scripts sont uniquement de la documentation
Les fichiers du répertoire `src/main/java/com/borneflow/automation/` sont des scripts JavaScript/Python illustrant CE QUE ferait l'automatisation. Ils ne s'exécutent pas réellement dans ce POC.

## Langue des règles
L'ensemble des fichiers de règles de `.bob/` est rédigé en français (contexte projet : entreprise française CGI).
