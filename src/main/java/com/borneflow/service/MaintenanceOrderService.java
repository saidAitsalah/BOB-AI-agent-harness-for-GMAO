package com.borneflow.service;

import com.borneflow.dao.MaintenanceOrderDao;
import com.borneflow.dao.MaintenanceOrderDao.OrderRecord;
import com.borneflow.exception.BorneflowException;
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
     * Tente de retrouver un ordre par son id, et renvoie {@link Optional#empty()}
     * en cas d'absence comme en cas d'erreur.
     */
    public Optional<OrderRecord> tryFindOrCreate(long id) {
        try {
            return dao.findById(id);
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
