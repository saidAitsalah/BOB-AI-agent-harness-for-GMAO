package com.borneflow.service;
import com.borneflow.dao.MaintenanceOrderDao;
import com.borneflow.mbo.MaintenanceOrderMbo;
public class MaintenanceOrderService {
    private MaintenanceOrderDao dao;
    private MaintenanceOrderMbo mbo;
    public MaintenanceOrder tryFindOrCreate(String orderId) {
        try { return dao.findById(orderId); }
        catch (Exception e) { return null; }
    }
    public void assign(String orderId, String matricule) {
        mbo.reassignTechnician(orderId, matricule);
    }
}
