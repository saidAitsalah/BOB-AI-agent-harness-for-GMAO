package com.borneflow.bean;

import com.borneflow.exception.BorneflowException;
import com.borneflow.mbo.MaintenanceOrderMbo;
import com.borneflow.mbo.MaintenanceOrderMbo.Priority;
import com.borneflow.mbo.MaintenanceOrderMbo.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * App Bean pour l'écran de gestion des ordres de maintenance.
 *
 * <p>Équivalent du pattern App Bean de Maximo : ces composants coordonnent
 * l'affichage et les actions de l'écran utilisateur, en encapsulant les
 * appels aux MBO et services.</p>
 *
 * <p>Méthodes typiques :</p>
 * <ul>
 *   <li>Actions des boutons de l'écran</li>
 *   <li>Préparation des listes affichées</li>
 *   <li>Validation côté présentation</li>
 * </ul>
 */
public class MaintenanceOrderAppBean {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceOrderAppBean.class);

    /** Action déclenchée par le bouton "Planifier" de l'écran. */
    public void onScheduleButtonClick(MaintenanceOrderMbo order, java.time.Instant when)
            throws BorneflowException {
        order.schedule(when);
        order.save();
        log.info("Ordre planifié via l'écran utilisateur");
    }

    /** Action déclenchée par le bouton "Démarrer". */
    public void onStartButtonClick(MaintenanceOrderMbo order) throws BorneflowException {
        order.start();
        order.save();
    }

    /** Action déclenchée par le bouton "Compléter". */
    public void onCompleteButtonClick(MaintenanceOrderMbo order, String notes)
            throws BorneflowException {
        if (notes == null || notes.isBlank()) {
            throw new BorneflowException(
                "BF_UI_VALIDATION",
                "Des notes de résolution sont obligatoires pour clôturer");
        }
        order.complete(notes);
        order.save();
    }

    /** Filtre les ordres affichables à un utilisateur selon leur priorité. */
    public List<MaintenanceOrderMbo> filterHighPriority(List<MaintenanceOrderMbo> orders) {
        var filtered = new ArrayList<MaintenanceOrderMbo>();
        for (var o : orders) {
            var prio = (Priority) o.getAttribute("priority");
            if (prio == Priority.HIGH || prio == Priority.CRITICAL) {
                filtered.add(o);
            }
        }
        return filtered;
    }

    /** Compte les ordres affichés par statut, pour les compteurs en haut d'écran. */
    public int countByStatus(List<MaintenanceOrderMbo> orders, Status status) {
        int count = 0;
        for (var o : orders) {
            if (o.getAttribute("status") == status) {
                count++;
            }
        }
        return count;
    }
}
