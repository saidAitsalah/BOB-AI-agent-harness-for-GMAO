# Conventions de codage BorneFlow

> Résumé des conventions à respecter sur le projet. Les règles détaillées et opposables aux outils d'IA sont dans `.bob/rules/`.

## 1. Nommage

### Préfixe métier
Toutes les classes custom doivent être préfixées **`Bf`** (pour BorneFlow), afin de les distinguer immédiatement du code standard ou des classes tierces.

✅ `BfChargingStationMbo`, `BfMaintenanceOrderService`
❌ `ChargingStationMbo`, `MaintenanceService`

> **Note** : pour ce POC, le préfixe `Bf` est **partiellement appliqué** : certaines classes utilisent le préfixe, d'autres non. C'est volontaire pour permettre aux outils de détecter les violations.

### Suffixes par couche
- MBO : `Mbo` (`...Mbo`, `...MboSet`)
- Service : `Service`
- DAO : `Dao`
- API REST : `Resource`
- App Beans : `AppBean`
- Exceptions : `Exception`

## 2. Structure des MBO

Tout MBO custom doit :
- Étendre la classe de base `BfMbo` (simulation Maximo `Mbo`)
- Surcharger `add()` pour initialiser les valeurs par défaut
- Surcharger `save()` pour valider avant persistance
- Surcharger `validate()` pour les règles métier
- Gérer correctement les transactions (rollback en cas d'erreur)

## 3. Gestion des erreurs

- **Jamais** `catch(Exception e)` vide ou silencieux
- Toujours logger l'exception avec son contexte (id de l'entité, opération en cours)
- Jeter une `BorneflowException` typée (jamais une `RuntimeException` brute)
- Si une exception est rattrapée pour ne pas être propagée, **documenter pourquoi**

## 4. Accès aux données

- **Toujours passer par un DAO**, jamais de SQL direct dans la couche Service ou MBO
- **Toutes les requêtes paramétrées** (`PreparedStatement`, jamais de concaténation)
- Fermer les ressources (`try-with-resources`)
- Pas de `SELECT *` — sélectionner explicitement les colonnes

## 5. Java 17 — fonctionnalités modernes

Privilégier les constructions modernes du langage :
- `record` pour les classes de données immuables
- `switch` expressions au lieu des `switch` statements traditionnels
- `var` pour les variables locales quand le type est évident
- `Optional` pour les retours qui peuvent être absents
- Text blocks pour les chaînes multi-lignes

Proscrire le style Java 8 ancien (getter/setter verbeux pour des DTO, `if/else` qui devraient être des `switch` expressions).

## 6. Logging

- **Toujours utiliser SLF4J** via `LoggerFactory.getLogger(...)`
- **Jamais** `System.out.println` ou `e.printStackTrace()`
- **Jamais logger de donnée personnelle ou sensible** : matricules, mots de passe, tokens, emails, noms personnels
- Niveaux : `ERROR` pour les anomalies, `WARN` pour les situations dégradées, `INFO` pour les événements métier, `DEBUG` pour le détail technique

## 7. Tests

- Tout service public doit avoir au moins un test unitaire couvrant le cas nominal et un cas d'erreur
- Utiliser JUnit 5 (`@Test`, `@BeforeEach`, etc.)
- Utiliser Mockito pour mocker les dépendances
- Nommer les tests `should<Comportement>_when<Condition>` (ex: `shouldRejectOrder_whenStationIsOffline`)

## 8. Documentation

- Javadoc obligatoire sur les classes publiques et méthodes publiques des services
- Commentaire en haut de fichier pour les classes complexes
- README à jour à chaque ajout de fonctionnalité majeure

---

*Pour le détail opposable aux outils Bob, voir `.bob/rules/*.md`.*
