package com.borneflow.service;

import com.borneflow.dao.TechnicianDao;
import com.borneflow.dao.TechnicianDao.TechnicianRecord;
import com.borneflow.exception.BorneflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Service de recherche de techniciens.
 */
public class TechnicianLookupService {

    private static final Logger log = LoggerFactory.getLogger(TechnicianLookupService.class);

    private final TechnicianDao dao;

    public TechnicianLookupService(TechnicianDao dao) {
        this.dao = dao;
    }

    /**
     * Recherche un technicien par son matricule.
     */
    public Optional<TechnicianRecord> lookupByMatricule(String matricule) throws BorneflowException {
        log.info("Recherche technicien par matricule={}", matricule);

        var result = dao.findByMatricule(matricule);

        if (result.isPresent()) {
            log.info("Technicien trouvé pour matricule={}, nom={}",
                     matricule, result.get().displayName());
        } else {
            log.info("Aucun technicien trouvé pour matricule={}", matricule);
        }

        return result;
    }

    public Optional<TechnicianRecord> lookupByDisplayName(String displayName) throws BorneflowException {
        log.debug("Recherche technicien par nom d'affichage");
        return dao.findByDisplayName(displayName);
    }
}
