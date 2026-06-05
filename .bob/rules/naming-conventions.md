# Règle BorneFlow — Conventions de nommage

## Préfixe métier obligatoire

Toutes les classes Java custom de ce projet doivent être préfixées par `Bf` (pour BorneFlow), afin de les distinguer du code standard ou des classes tierces.

### Exemples corrects
- `BfChargingStationMbo`
- `BfMaintenanceOrderService`
- `BfTechnicianDao`

### Exemples incorrects
- `ChargingStationMbo` (préfixe manquant)
- `MaintenanceService` (préfixe et suffixe manquants)

## Suffixes par couche

| Couche | Suffixe | Exemple |
|---|---|---|
| MBO | `Mbo` | `BfChargingStationMbo` |
| Collection MBO | `MboSet` | `BfChargingStationMboSet` |
| Service | `Service` | `BfMaintenanceOrderService` |
| DAO | `Dao` | `BfTechnicianDao` |
| API REST | `Resource` | `BfChargingStationResource` |
| App Bean | `AppBean` | `BfMaintenanceOrderAppBean` |
| Exception | `Exception` | `BfStationOfflineException` |

## Que faire en cas de violation

Quand une classe existante viole cette convention :
1. **Ne pas renommer en silence** : la signaler dans les findings
2. Proposer un renommage groupé (refactoring) car cela impacte tous les imports
3. Indiquer si le renommage est compatible avec les conventions du module concerné

## Variables et méthodes

- Variables : `lowerCamelCase`
- Méthodes : `lowerCamelCase`, verbe d'action (`save`, `findByExternalId`)
- Constantes : `UPPER_SNAKE_CASE`
- Tests : nommage `should<Comportement>_when<Condition>`, ex: `shouldRejectOrder_whenStationIsOffline`
