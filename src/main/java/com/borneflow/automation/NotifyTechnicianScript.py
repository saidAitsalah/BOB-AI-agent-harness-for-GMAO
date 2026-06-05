# ============================================================================
# Automation Script — NotifyTechnicianScript
# ============================================================================
# Équivalent fonctionnel d'un automation script Maximo en Python (Jython).
#
# Déclencheur (en environnement réel) : à chaque création d'un ordre de
# maintenance en statut SCHEDULED, envoie une notification au technicien
# affecté.
#
# Variables d'entrée disponibles (en environnement Maximo réel) :
#   - mbo           : l'objet MaintenanceOrder qui vient d'être créé
#   - logger        : logger Maximo
#
# Pour le POC BorneFlow, ce fichier sert uniquement de documentation
# de la logique attendue. Il n'est pas exécuté.
# ============================================================================


def notify_technician():
    """Envoie une notification au technicien affecté à l'ordre."""

    status = mbo.getString("STATUS")
    if status != "SCHEDULED":
        return  # pas concerné

    technician_id = mbo.getString("TECHNICIANID")
    if not technician_id:
        logger.warn("Ordre planifie sans technicien affecte")
        return

    scheduled_at = mbo.getDate("SCHEDULEDAT")
    description = mbo.getString("DESCRIPTION")
    station_id = mbo.getString("STATIONID")

    # Construction du message — pas de matricule en clair dans le log applicatif
    subject = "Nouvelle maintenance planifiee"
    body = (
        "Une intervention vous a ete affectee.\n"
        "Borne   : " + station_id + "\n"
        "Date    : " + str(scheduled_at) + "\n"
        "Tache   : " + description + "\n"
    )

    # En environnement Maximo, on utiliserait MXServer pour envoyer la notif
    # send_notification(technician_id, subject, body)

    logger.info("Notification envoyee pour l'ordre " + str(mbo.getString("ORDERID")))


notify_technician()
