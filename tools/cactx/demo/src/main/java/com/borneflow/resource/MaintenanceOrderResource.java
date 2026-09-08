package com.borneflow.resource;
import com.borneflow.service.MaintenanceOrderService;
public class MaintenanceOrderResource {
    private MaintenanceOrderService service;
    public Response deleteOrder(String orderId) {
        service.delete(orderId);
        return Response.ok().build();
    }
    public Response getOrder(String orderId) {
        return Response.ok(service.tryFindOrCreate(orderId)).build();
    }
}
