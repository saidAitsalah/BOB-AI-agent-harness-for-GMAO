# Règle BorneFlow — Gestion des erreurs

## Principe général

Toute erreur métier du projet doit être représentée par une `BorneflowException` (ou un de ses descendants), avec un code d'erreur fonctionnel identifiable.

## Règles opposables

### R1 — Jamais de catch vide ou silencieux

❌ **Interdit** :
```java
try {
    someService.doWork();
} catch (Exception e) {
    // rien ici
}
```

❌ **Interdit** (variante) :
```java
try {
    someService.doWork();
} catch (Exception e) {
    return Optional.empty(); // masque la vraie raison
}
```

✅ **Correct** :
```java
try {
    someService.doWork();
} catch (BorneflowException e) {
    log.error("Échec opération doWork sur entité={}", entityId, e);
    throw e; // ou wrap dans une exception métier de plus haut niveau
}
```

### R2 — Toujours typer les exceptions métier

Toute exception métier doit étendre `BorneflowException`. Les `RuntimeException` brutes (`IllegalStateException`, `IllegalArgumentException`) sont autorisées uniquement pour les violations de contrat de programmation, pas pour les règles métier.

### R3 — Toujours logger avec contexte

Le log d'une exception doit inclure :
- Le verbe / l'opération en échec
- L'identifiant de l'entité concernée (si non sensible)
- L'exception elle-même (passée comme dernier argument SLF4J, pas concaténée)

✅ **Correct** :
```java
log.error("Échec save() de la borne externalId={}", externalId, e);
```

❌ **Incorrect** :
```java
log.error("Échec : " + e.getMessage()); // perd la stack trace
```

### R4 — Documenter les rattrapages volontaires

Si une exception est rattrapée et volontairement non propagée, il faut un commentaire qui explique pourquoi.

## Codes d'erreur

Les codes d'erreur sont préfixés `BF_` et utilisent la nomenclature `BF_<DOMAINE>_<MOTIF>`.

Exemples :
- `BF_STATION_INVALID` — données de borne invalides
- `BF_STATION_INVALID_TRANSITION` — transition de statut interdite
- `BF_MO_INVALID` — ordre de maintenance invalide
- `BF_TECH_NOT_FOUND` — technicien introuvable
- `BF_DAO_ERROR` — erreur d'accès aux données

## Que faire en revue de code

- Détecter tout `catch (Exception e)` sans logging et sans rethrow
- Détecter tout `e.printStackTrace()`
- Détecter toute exception brute (non `BorneflowException`) jetée pour une règle métier
- Détecter les logs sans contexte (juste `e.getMessage()`)
