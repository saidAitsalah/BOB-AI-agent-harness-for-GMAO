# Règle BorneFlow — Sécurité

## Principes généraux

Le projet BorneFlow simule un contexte de gestion d'actifs critiques. Les règles de sécurité ne sont pas négociables, même dans le code de POC.

## Règles opposables

### S1 — Pas de données personnelles dans les logs

Sont considérées comme données personnelles à ne **jamais** logger en clair :
- Matricule technicien (`matricule`)
- Email personnel
- Nom complet d'une personne physique
- Identifiant de session utilisateur
- Tout token d'authentification

✅ **Acceptable** : logger un identifiant technique anonyme (id base, externalId d'asset).

❌ **Interdit** :
```java
log.info("Recherche pour matricule={}", matricule); // matricule en clair
log.debug("Token reçu : {}", authToken);            // token en clair
```

✅ **Correct** :
```java
log.debug("Recherche technicien par matricule");    // pas la valeur
log.info("Authentification réussie pour userId={}", userId); // id technique opaque
```

### S2 — Pas de secrets dans les fichiers de configuration versionnés

Sont considérés comme des secrets :
- Mots de passe de base de données
- Tokens d'API (Stripe, Slack, IBM Cloud, etc.)
- Clés privées
- Mots de passe SMTP

Ces secrets doivent être injectés via :
- Variables d'environnement
- Coffre-fort de secrets (HashiCorp Vault, AWS Secrets Manager, IBM Cloud Secrets Manager)
- Fichier de configuration **hors du dépôt Git**, monté à l'exécution

❌ **Interdit** dans `application.properties` versionné :
```properties
borneflow.db.password=REDACTED_PASSWORD
borneflow.integration.api.token=sk_live_a1b2c3...
```

✅ **Correct** :
```properties
borneflow.db.password=${BF_DB_PASSWORD}
borneflow.integration.api.token=${BF_API_TOKEN}
```

### S3 — Requêtes SQL paramétrées

Toute requête SQL utilisant une valeur d'entrée doit utiliser `PreparedStatement` avec paramètres liés. La concaténation de chaîne avec une valeur d'entrée pour construire un SQL est une **vulnérabilité d'injection** (cf. règle complète dans `mbo-access-patterns.md`).

### S4 — Contrôle d'accès sur toute méthode d'API

Toute méthode publique d'une ressource REST (`@GET`, `@POST`, `@PUT`, `@DELETE`) doit vérifier les droits d'accès via `AuthorizationContext` **avant** d'exécuter son traitement.

❌ **Interdit** :
```java
@DELETE
@Path("/{id}")
public Response deleteOrder(@PathParam("id") long id) {
    // Pas de vérification d'autorisation !
    service.delete(id);
    return Response.noContent().build();
}
```

✅ **Correct** :
```java
@DELETE
@Path("/{id}")
public Response deleteOrder(@PathParam("id") long id) {
    if (!authz.canDelete("maintenance_order", String.valueOf(id))) {
        return Response.status(Response.Status.FORBIDDEN).build();
    }
    service.delete(id);
    return Response.noContent().build();
}
```

### S5 — Pas de stack trace exposée aux clients d'API

Les exceptions techniques ne doivent pas être renvoyées telles quelles à l'appelant HTTP : retourner un code d'erreur générique côté API (`500`, `400`) et logger les détails côté serveur.

### S6 — Validation des entrées utilisateur

Toute valeur reçue d'un client externe (HTTP, file, queue) doit être validée :
- Longueur maximale
- Format attendu (regex, type)
- Plage de valeurs autorisées
- Caractères interdits (selon usage)

## Catégories de findings sécurité

Quand une violation est détectée, la classer parmi :

| Catégorie | Sévérité par défaut |
|---|---|
| Injection SQL | CRITIQUE |
| Secret en clair | CRITIQUE |
| Contrôle d'accès manquant | CRITIQUE |
| Donnée personnelle loggée | HAUTE |
| Stack trace exposée | MOYENNE |
| Validation d'entrée manquante | MOYENNE |

## Que faire en revue de code

Avant chaque commit, l'outil d'IA agentique doit scanner :
- Les fichiers `.properties`, `.yml`, `.yaml` à la recherche de patterns ressemblant à des secrets (`password=`, `token=`, `secret=`, `key=` suivis de chaînes non-vides non-références d'env)
- Les classes `Resource` à la recherche de méthodes publiques sans appel à `authz.can...()`
- Les classes `Dao` à la recherche de SQL construit par concaténation
- Les classes `Service` à la recherche de logs qui passent des identifiants personnels en argument
