package com.borneflow.service;

import com.borneflow.dao.ChargingStationDao;
import com.borneflow.dao.ChargingStationDao.StationRecord;
import com.borneflow.exception.BorneflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

/**
 * Service d'orchestration métier pour les bornes de recharge.
 *
 * <p>Encapsule les règles métier de haut niveau et délègue la persistance
 * au {@link ChargingStationDao}.</p>
 */
public class ChargingStationService {

    private static final Logger log = LoggerFactory.getLogger(ChargingStationService.class);

    private final ChargingStationDao dao;

    public ChargingStationService(ChargingStationDao dao) {
        this.dao = dao;
    }

    public Optional<StationRecord> getByExternalId(String externalId) throws BorneflowException {
        if (externalId == null || externalId.isBlank()) {
            throw new BorneflowException(
                "BF_STATION_INVALID",
                "L'identifiant externe est requis");
        }
        return dao.findByExternalId(externalId);
    }

    public List<StationRecord> listOperational() throws BorneflowException {
        log.debug("Recherche des bornes opérationnelles");
        return dao.findByStatus("OPERATIONAL");
    }

    public List<StationRecord> listInMaintenance() throws BorneflowException {
        return dao.findByStatus("MAINTENANCE");
    }
}
