package com.borneflow.service;

import com.borneflow.dao.MaintenanceOrderDao;
import com.borneflow.dao.MaintenanceOrderDao.OrderRecord;
import com.borneflow.exception.BorneflowException;
import com.borneflow.mbo.MaintenanceOrderMbo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service d'orchestration métier pour les ordres de maintenance.
 */
public class MaintenanceOrderService {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceOrderService.class);

    private final MaintenanceOrderDao dao;

    public MaintenanceOrderService(MaintenanceOrderDao dao) {
        this.dao = dao;
    }

    public Optional<OrderRecord> getById(long id) throws BorneflowException {
        return dao.findById(id);
    }

    public List<OrderRecord> listScheduledOverdue() throws BorneflowException {
        return dao.findScheduledBefore(Instant.now());
    }

    public List<OrderRecord> listForStation(String stationId, String status) throws BorneflowException {
        if (stationId == null || stationId.isBlank()) {
            throw new BorneflowException(
                "BF_MO_INVALID",
                "L'identifiant de borne est requis");
        }
        return dao.findByStationAndStatus(stationId, status);
    }

    /**
     * Recherche un ordre par son id.
     * Renvoie {@link Optional#empty()} si l'ordre n'existe pas en base.
     * Propage une {@link BorneflowException} en cas d'erreur d'accès aux données.
     */
    public Optional<OrderRecord> findById(long id) throws BorneflowException {
        try {
            return dao.findById(id);
        } catch (BorneflowException e) {
            log.error("Échec findById() pour ordre id={}", id, e);
            throw e;
        }
    }

    /**
     * Charge un ordre par son identifiant.
     *
     * <p>Conforme à BF100-RDG-006 : si aucun ordre ne correspond à l'identifiant
     * fourni, un {@link MaintenanceOrderMbo} à l'état {@code DRAFT} est créé et
     * retourné pour complétion par l'utilisateur. En cas d'erreur technique d'accès
     * aux données, l'exception est journalisée et propagée — elle ne doit jamais
     * être assimilée à une absence de résultat.</p>
     *
     * @param id identifiant de l'ordre recherché
     * @return le MBO existant chargé depuis la base, ou un nouveau brouillon
     * @throws BorneflowException en cas d'erreur d'accès aux données
     */
    public MaintenanceOrderMbo tryFindOrCreate(long id) throws BorneflowException {
        Optional<OrderRecord> existing;
        try {
            existing = dao.findById(id);
        } catch (BorneflowException e) {
            log.error("Échec tryFindOrCreate() pour ordre id={}", id, e);
            throw e;
        }

        if (existing.isPresent()) {
            var record = existing.get();
            var mbo = new MaintenanceOrderMbo();
            mbo.setAttribute("id",           record.id());
            mbo.setAttribute("stationId",    record.stationId());
            mbo.setAttribute("technicianId", record.technicianId());
            mbo.setAttribute("status",       MaintenanceOrderMbo.Status.valueOf(record.status()));
            mbo.setAttribute("priority",     MaintenanceOrderMbo.Priority.valueOf(record.priority()));
            mbo.setAttribute("description",  record.description());
            mbo.setAttribute("scheduledAt",  record.scheduledAt());
            mbo.setAttribute("completedAt",  record.completedAt());
            return mbo;
        }

        // BF100-RDG-006 : aucun ordre trouvé → créer un brouillon proposé à l'utilisateur
        log.info("Ordre id={} introuvable — création d'un brouillon (BF100-RDG-006)", id);
        var draft = new MaintenanceOrderMbo();
        draft.add();
        return draft;
    }
}
