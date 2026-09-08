package com.borneflow.mbo;

import com.borneflow.exception.BorneflowException;
import com.borneflow.mbo.MaintenanceOrderMbo.Priority;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests couvrant les trois effets indissociables de BF100-RDG-032
 * lors d'une réaffectation de technicien :
 *
 *   Effet 1 — mise à jour de l'affectation (technicianId)
 *   Effet 2 — recalcul de la date d'intervention (scheduledAt)
 *   Effet 3 — inscription à l'historique de l'ordre (assignmentHistory)
 *
 * Un quatrième cas documente l'écart non spécifié :
 * le downgrade CRITICAL→HIGH présent dans le code n'a aucune base dans la spec.
 */
class MaintenanceOrderMboRdg032Test {

    private MaintenanceOrderMbo order;

    @BeforeEach
    void setUp() throws BorneflowException {
        order = new MaintenanceOrderMbo();
        order.add();
        order.setAttribute("stationId",    "BORNE-001");
        order.setAttribute("description",  "Inspection annuelle");
        order.setAttribute("technicianId", "TECH00001");
        order.setAttribute("scheduledAt",  Instant.parse("2025-06-10T08:00:00Z"));
    }

    // ── Effet 1 : mise à jour de l'affectation ────────────────────────────────

    @Test
    void shouldUpdateTechnicianId_whenReassigned() {
        // BF100-RDG-032, effet 1 : la mise à jour du technicianId est obligatoire.
        order.reassignTechnician("TECH00002");

        assertEquals("TECH00002", order.getAttribute("technicianId"),
                "BF100-RDG-032 effet 1 : technicianId doit pointer vers le nouveau technicien");
    }

    // ── Effet 2 : recalcul de la date d'intervention ─────────────────────────

    @Test
    void shouldRecalculateScheduledAt_whenReassigned() {
        // BF100-RDG-032, effet 2 : la date d'intervention est recalculée selon
        // la disponibilité du nouveau technicien — elle ne doit donc plus être
        // identique à la date initiale.
        // ÉTAT ACTUEL : non implémenté → ce test ÉCHOUE intentionnellement.
        Instant initialDate = Instant.parse("2025-06-10T08:00:00Z");

        order.reassignTechnician("TECH00002");

        Instant recalculated = (Instant) order.getAttribute("scheduledAt");
        assertNotNull(recalculated,
                "BF100-RDG-032 effet 2 : scheduledAt ne doit pas être null après réaffectation");
        assertNotEquals(initialDate, recalculated,
                "BF100-RDG-032 effet 2 : scheduledAt doit être recalculé selon la disponibilité du nouveau technicien");
    }

    // ── Effet 3 : inscription à l'historique ─────────────────────────────────

    @Test
    @SuppressWarnings("unchecked")
    void shouldAppendEntryToAssignmentHistory_whenReassigned() {
        // BF100-RDG-032, effet 3 : l'opération de réaffectation doit être inscrite
        // dans l'historique de l'ordre.
        // ÉTAT ACTUEL : non implémenté → ce test ÉCHOUE intentionnellement.
        order.reassignTechnician("TECH00002");

        List<?> history = (List<?>) order.getAttribute("assignmentHistory");
        assertNotNull(history,
                "BF100-RDG-032 effet 3 : l'attribut assignmentHistory doit exister après réaffectation");
        assertFalse(history.isEmpty(),
                "BF100-RDG-032 effet 3 : l'historique doit contenir au moins une entrée");
    }

    // ── Écart non spécifié : downgrade CRITICAL→HIGH ─────────────────────────

    @Test
    void shouldNotDowngradePriority_whenReassigned_specGap() {
        // BF100-RDG-032 ne mentionne aucun changement de priorité lors d'une réaffectation.
        // Le code actuel abaisse CRITICAL→HIGH, ce qui est hors spec.
        // ÉTAT ACTUEL : ce test ÉCHOUE intentionnellement pour documenter l'écart.
        order.setAttribute("priority", Priority.CRITICAL);

        order.reassignTechnician("TECH00002");

        assertEquals(Priority.CRITICAL, order.getAttribute("priority"),
                "BF100-RDG-032 : aucun downgrade de priorité n'est prévu par la spec lors d'une réaffectation");
    }
}
