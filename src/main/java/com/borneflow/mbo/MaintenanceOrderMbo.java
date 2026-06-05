package com.borneflow.mbo;

import com.borneflow.exception.BorneflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * MBO représentant une opération de maintenance (équivalent {@code Work Order} en Maximo).
 *
 * <p>Une MaintenanceOrder est rattachée à une borne ({@link ChargingStationMbo}) et
 * affectée à un technicien ({@link TechnicianMbo}).</p>
 *
 * <p>Cycle de vie :</p>
 * <pre>
 *   DRAFT → SCHEDULED → IN_PROGRESS → COMPLETED
 *                  ↘                    ↗
 *                    CANCELLED ←────────
 * </pre>
 */
public class MaintenanceOrderMbo extends BfMbo {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceOrderMbo.class);

    public enum Status {
        DRAFT, SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    }

    public enum Priority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    @Override
    public void add() throws BorneflowException {
        super.add();
        setAttribute("status", Status.DRAFT);
        setAttribute("priority", Priority.NORMAL);
    }

    @Override
    public void validate() throws BorneflowException {
        var stationId = (String) getAttribute("stationId");
        if (stationId == null || stationId.isBlank()) {
            throw new BorneflowException(
                "BF_MO_INVALID",
                "Une maintenance doit être rattachée à une borne (stationId)");
        }

        var description = (String) getAttribute("description");
        if (description == null || description.isBlank()) {
            throw new BorneflowException(
                "BF_MO_INVALID",
                "La description de la maintenance est obligatoire",
                stationId);
        }
    }

    public void schedule(Instant when) throws BorneflowException {
        if (getAttribute("status") != Status.DRAFT) {
            throw new BorneflowException(
                "BF_MO_INVALID_TRANSITION",
                "Seul un ordre en DRAFT peut être planifié");
        }
        setAttribute("scheduledAt", when);
        setAttribute("status", Status.SCHEDULED);
        log.info("Maintenance planifiée pour {}", when);
    }

    public void start() throws BorneflowException {
        if (getAttribute("status") != Status.SCHEDULED) {
            throw new BorneflowException(
                "BF_MO_INVALID_TRANSITION",
                "Seul un ordre SCHEDULED peut démarrer");
        }
        setAttribute("startedAt", Instant.now());
        setAttribute("status", Status.IN_PROGRESS);
    }

    public void complete(String resolutionNotes) throws BorneflowException {
        if (getAttribute("status") != Status.IN_PROGRESS) {
            throw new BorneflowException(
                "BF_MO_INVALID_TRANSITION",
                "Seul un ordre IN_PROGRESS peut être complété");
        }
        setAttribute("completedAt", Instant.now());
        setAttribute("resolutionNotes", resolutionNotes);
        setAttribute("status", Status.COMPLETED);
    }

    public void reassignTechnician(String newTechnicianId) {
        var oldTechId = (String) getAttribute("technicianId");
        setAttribute("technicianId", newTechnicianId);

        if (getAttribute("priority") == Priority.CRITICAL) {
            setAttribute("priority", Priority.HIGH);
        }

        log.info("Technicien réaffecté ({}→{})", oldTechId, newTechnicianId);
    }
}
