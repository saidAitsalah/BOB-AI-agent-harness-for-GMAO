# Mode Code - Règles non évidentes

Ce fichier contient les règles de codage spécifiques au projet, découvertes par lecture du code source.

## Application du cycle de vie des MBO

### L'ordre d'appel des hooks est obligatoire
Les MBO DOIVENT appeler `add() → validate() → save()` dans cet ordre strict. La méthode `BfMbo.save()` de base appelle `validate()` automatiquement, mais si vous surchargez `save()`, vous DEVEZ appeler `super.save()` en premier, sinon la validation sera contournée.

**Critique** : ne jamais appeler `save()` directement sans garantir que `validate()` s'exécute. Ceci n'est PAS vérifié par le compilateur.

### Pattern d'accès aux attributs
Les MBO utilisent des méthodes génériques `getAttribute(String)` / `setAttribute(String, Object)` plutôt que des getters/setters typés. Ceci imite le système d'attributs dynamiques de Maximo. Lors de la lecture d'un attribut, toujours caster vers le type attendu et gérer le cas `null`.

## Pattern de transformation DAO vers Service

### Les records sont des projections en lecture seule
Les DAO renvoient des types `record` Java (par exemple `StationRecord`, `OrderRecord`). Ils ne sont JAMAIS convertis en MBO à l'intérieur du DAO - cette transformation a lieu dans la couche Service.

**Pourquoi** : cela garde les MBO indépendants du schéma de base de données. Si vous ajoutez une méthode DAO, elle doit renvoyer un `record`, pas un MBO.

### PreparedStatement est non négociable
TOUTES les requêtes SQL DOIVENT utiliser `PreparedStatement` avec des placeholders `?`. La concaténation de chaînes comme `"WHERE id = '" + id + "'"` est une violation de sécurité CRITIQUE et fera échouer la revue de code.

**Pattern à suivre** : voir `ChargingStationDao.findByExternalId()` comme implémentation de référence.

## Spécificités de la gestion des exceptions

### Position du paramètre exception pour SLF4J
Lors de la journalisation d'une exception, celle-ci DOIT être le DERNIER paramètre : `log.error("Failed for id={}", id, exception)`. Ne jamais concaténer `e.getMessage()` dans la chaîne du message - cela fait perdre la pile d'appels.

### Codes d'erreur BorneflowException
Tous les codes d'erreur suivent le format `BF_<DOMAINE>_<RAISON>` :
- `BF_STATION_INVALID` - échec de validation d'une borne
- `BF_STATION_INVALID_TRANSITION` - transition d'état illégale
- `BF_MO_INVALID` - échec de validation d'un ordre de maintenance
- `BF_TECH_NOT_FOUND` - technicien introuvable
- `BF_DAO_ERROR` - erreur d'accès à la base de données

## Patterns de sécurité

### Contexte d'autorisation requis
Chaque méthode de Resource REST (`@GET`, `@POST`, `@PUT`, `@DELETE`) DOIT appeler `authz.can*()` avant d'exécuter la logique métier. Ceci n'est PAS imposé par le framework - vous devez y penser vous-même.

### Liste noire des données journalisées
Ne JAMAIS journaliser ces champs en clair :
- `matricule` (identifiant employé du technicien)
- adresses email
- noms complets de personnes
- jetons de session
- jetons d'authentification

Seuls les identifiants techniques comme `id` ou `externalId` peuvent être journalisés pour les assets.

## Patterns Java 17 utilisés

### Expressions switch pour les machines à états
La logique de transition d'état utilise des expressions switch (voir `ChargingStationMbo.isTransitionAllowed()`). Utiliser ce pattern plutôt que des chaînes if/else pour la validation d'état.

### Records pour les DTO
Tous les types renvoyés par les DAO sont des classes `record`. Ne jamais créer de classes DTO mutables avec getters/setters pour les projections de base de données.

### Text blocks pour le SQL
Le SQL multi-lignes utilise des text blocks (`"""..."""`). C'est le standard pour tout le SQL du projet.

## Patterns de tests

### Nommage des méthodes de test
Les tests suivent le pattern `should<Comportement>_when<Condition>` :
- `shouldRejectSave_whenExternalIdMissing()`
- `shouldAllowTransitionCommissionedToOperational()`

PAS `testSave()` ni `saveTest()`.

### Pattern d'initialisation des tests
Utiliser `@BeforeEach` pour créer et initialiser les MBO avec `add()` déjà appelé. Cela garantit que les tests démarrent avec des objets correctement initialisés.

## Violations intentionnelles (NE PAS CORRIGER)

### Préfixe Bf manquant
`ChargingStationMbo`, `ChargingStationMboSet`, `MaintenanceOrderMbo` n'ont volontairement pas le préfixe `Bf`. C'est prévu pour les tests de benchmark - signaler mais ne pas renommer sans accord explicite.

### Secrets en dur
Les lignes 14-15 de `application.properties` contiennent des secrets en dur. C'est INTENTIONNEL pour les benchmarks de scan de sécurité. Signaler mais ne pas supprimer.
