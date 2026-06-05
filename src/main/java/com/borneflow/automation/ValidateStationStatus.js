// ============================================================================
// Automation Script — ValidateStationStatus
// ============================================================================
// Équivalent fonctionnel d'un automation script Maximo en JavaScript.
//
// Déclencheur (en environnement réel) : avant chaque save() d'un objet
// ChargingStation, valide la cohérence des transitions de statut.
//
// Variables d'entrée disponibles (en environnement Maximo réel) :
//   - mbo                  : l'objet ChargingStation en cours de sauvegarde
//   - oldStatus            : valeur précédente du statut
//   - newStatus            : valeur cible du statut
//   - logger               : logger Maximo
//
// Pour le POC BorneFlow, ce fichier sert uniquement de documentation
// de la logique attendue. Il n'est pas exécuté.
// ============================================================================

(function validateStationStatus() {
    var ALLOWED_TRANSITIONS = {
        "COMMISSIONED":   ["OPERATIONAL", "DECOMMISSIONED"],
        "OPERATIONAL":    ["MAINTENANCE",  "DECOMMISSIONED"],
        "MAINTENANCE":    ["OPERATIONAL",  "DECOMMISSIONED"],
        "DECOMMISSIONED": []
    };

    if (oldStatus === newStatus) {
        return; // pas de changement, rien à valider
    }

    var allowed = ALLOWED_TRANSITIONS[oldStatus] || [];
    if (allowed.indexOf(newStatus) === -1) {
        throw new Error(
            "BF_STATION_INVALID_TRANSITION: transition " +
            oldStatus + " -> " + newStatus + " interdite"
        );
    }

    // Effet de bord : si on passe en MAINTENANCE, on note la date
    if (newStatus === "MAINTENANCE") {
        mbo.setValue("LASTMAINTENANCESTARTEDAT", new Date());
    }

    logger.info("Transition de statut validée : " + oldStatus + " -> " + newStatus);
})();
