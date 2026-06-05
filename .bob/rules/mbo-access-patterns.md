# Règle BorneFlow — Accès aux MBO et aux données

## Principe général

L'accès aux données est strictement séparé par couches : seuls les DAO accèdent à la base. Les services et MBO ne contiennent **jamais** de SQL.

## Règles opposables

### R1 — Pas de SQL hors de la couche DAO

❌ **Interdit** : SQL direct dans un service ou un MBO.

✅ **Correct** : appel d'une méthode DAO qui renvoie un record / un MBO.

### R2 — Requêtes paramétrées obligatoires

Toute requête SQL doit utiliser `PreparedStatement` avec paramètres liés, **jamais** de concaténation de chaîne avec une valeur d'entrée utilisateur.

❌ **Interdit** :
```java
String sql = "SELECT * FROM x WHERE id = '" + userInput + "'";
Statement st = connection.createStatement();
ResultSet rs = st.executeQuery(sql);
```

✅ **Correct** :
```java
String sql = "SELECT id, name FROM x WHERE id = ?";
try (PreparedStatement ps = connection.prepareStatement(sql)) {
    ps.setString(1, userInput);
    try (ResultSet rs = ps.executeQuery()) { ... }
}
```

### R3 — Sélectionner explicitement les colonnes

`SELECT *` est interdit. Toujours énumérer les colonnes attendues. Cela évite les régressions silencieuses si le schéma évolue.

### R4 — Fermer les ressources via try-with-resources

Toute ressource JDBC (`Connection`, `Statement`, `PreparedStatement`, `ResultSet`) doit être ouverte dans un `try-with-resources`. Pas de `finally` manuel pour la fermeture.

### R5 — Le DAO renvoie des records, pas des MBO

Les DAO renvoient des `record` Java (Projection de lecture). La transformation `record → MBO` se fait dans la couche service, pas dans le DAO. Cela maintient les MBO indépendants du schéma de base.

## Patterns d'accès aux MBO

### Hooks à respecter

Tout MBO doit appeler la chaîne `add() → validate() → save()` dans cet ordre. Ne jamais appeler `save()` sans avoir appelé `validate()` au préalable (la classe de base le fait automatiquement, mais si une sous-classe override `save()`, elle doit `super.save()` en premier).

### Transactions

- Les MBO ne gèrent pas les transactions eux-mêmes : elles sont déclenchées par le service ou par le contexte d'invocation.
- En cas d'erreur lors d'un `save()` d'un MBO d'un set, le rollback de toute la transaction est attendu.

## Que faire en revue de code

- Détecter toute concaténation `"... '" + ... + "'"` dans une requête SQL : potentielle injection
- Détecter tout `Statement` (non `PreparedStatement`) utilisé avec une valeur d'entrée
- Détecter tout `SELECT *`
- Détecter toute connexion ouverte hors `try-with-resources`
- Détecter tout SQL dans une classe non-DAO
