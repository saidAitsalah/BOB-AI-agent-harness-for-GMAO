package com.borneflow.mbo;

import com.borneflow.exception.BorneflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

/**
 * MBO représentant une borne de recharge électrique (équivalent {@code Asset} en Maximo).
 *
 * <p>Cycle de vie d'une borne :</p>
 * <pre>
 *   COMMISSIONED  →  OPERATIONAL  ⇄  MAINTENANCE
 *                       ↓
 *                   DECOMMISSIONED
 * </pre>
 *
 * <p>Conforme aux conventions BorneFlow :</p>
 * <ul>
 *   <li>Préfixe {@code Bf} <em>absent volontairement</em> pour ce MBO — cf. README</li>
 *   <li>Extends {@link BfMbo}</li>
 *   <li>Override de {@link #add()}, {@link #save()}, {@link #validate()}</li>
 * </ul>
 */
public class ChargingStationMbo extends BfMbo {

    private static final Logger log = LoggerFactory.getLogger(ChargingStationMbo.class);

    /** Statuts possibles d'une borne. */
    public enum Status {
        COMMISSIONED,    // installée mais pas encore mise en service
        OPERATIONAL,     // en service
        MAINTENANCE,     // hors service pour maintenance
        DECOMMISSIONED   // retirée du parc
    }

    @Override
    public void add() throws BorneflowException {
        super.add();
        // Valeurs par défaut à la création
        setAttribute("status", Status.COMMISSIONED);
        setAttribute("totalSessions", 0L);
        setAttribute("totalEnergyKwh", 0.0);
        log.debug("Nouvelle borne créée avec statut COMMISSIONED");
    }

    @Override
    public void validate() throws BorneflowException {
        // Règle métier : l'identifiant externe est obligatoire
        var externalId = (String) getAttribute("externalId");
        if (externalId == null || externalId.isBlank()) {
            throw new BorneflowException(
                "BF_STATION_INVALID",
                "L'identifiant externe de la borne est obligatoire");
        }

        // Règle métier : la puissance doit être strictement positive
        var powerKw = (Double) getAttribute("powerKw");
        if (powerKw == null || powerKw <= 0) {
            throw new BorneflowException(
                "BF_STATION_INVALID",
                "La puissance doit être strictement positive",
                externalId);
        }
    }

    @Override
    public void save() throws BorneflowException {
        super.save();
        // Hook spécifique : si la borne passe en MAINTENANCE, on note la date
        if (getAttribute("status") == Status.MAINTENANCE) {
            setAttribute("lastMaintenanceStartedAt", Instant.now());
        }
    }

    /** Demande un changement de statut avec validation des transitions autorisées. */
    public void changeStatus(Status newStatus) throws BorneflowException {
        var current = (Status) getAttribute("status");
        if (!isTransitionAllowed(current, newStatus)) {
            throw new BorneflowException(
                "BF_STATION_INVALID_TRANSITION",
                "Transition de statut interdite : " + current + " → " + newStatus,
                (String) getAttribute("externalId"));
        }
        setAttribute("status", newStatus);
        log.info("Statut borne changé : {} → {}", current, newStatus);
    }

    private boolean isTransitionAllowed(Status from, Status to) {
        return switch (from) {
            case COMMISSIONED   -> to == Status.OPERATIONAL || to == Status.DECOMMISSIONED;
            case OPERATIONAL    -> to == Status.MAINTENANCE || to == Status.DECOMMISSIONED;
            case MAINTENANCE    -> to == Status.OPERATIONAL || to == Status.DECOMMISSIONED;
            case DECOMMISSIONED -> false;
        };
    }
}
