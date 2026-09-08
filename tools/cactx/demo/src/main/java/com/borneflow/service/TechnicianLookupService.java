package com.borneflow.service;
import com.borneflow.dao.TechnicianDao;
import org.slf4j.Logger;
public class TechnicianLookupService {
    private TechnicianDao technicianDao;
    private Logger log;
    public Technician lookup(String matricule) {
        log.info("Lookup for matricule=" + matricule);
        return technicianDao.findByMatricule(matricule);
    }
}
