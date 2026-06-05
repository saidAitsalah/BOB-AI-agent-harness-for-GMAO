package com.borneflow.api;

import com.borneflow.exception.BorneflowException;
import com.borneflow.service.MaintenanceOrderService;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Ressource REST exposant les ordres de maintenance.
 */
@Path("/api/v1/maintenance-orders")
@Produces(MediaType.APPLICATION_JSON)
public class MaintenanceOrderResource {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceOrderResource.class);

    private final MaintenanceOrderService service;
    private final AuthorizationContext authz;

    public MaintenanceOrderResource(MaintenanceOrderService service, AuthorizationContext authz) {
        this.service = service;
        this.authz = authz;
    }

    @GET
    @Path("/{id}")
    public Response getOrder(@PathParam("id") long id) {
        // Contrôle d'accès en lecture — implémentation correcte
        if (!authz.canRead("maintenance_order", String.valueOf(id))) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            var result = service.getById(id);
            return result
                .map(o -> Response.ok(o).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
        } catch (BorneflowException e) {
            log.error("Erreur recherche ordre", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @POST
    public Response createOrder(@QueryParam("stationId") String stationId,
                                @QueryParam("description") String description) {
        // Contrôle d'accès en création — implémentation correcte
        if (!authz.canCreate("maintenance_order")) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        // (simplification : pas d'implémentation complète pour le POC)
        return Response.status(Response.Status.CREATED).build();
    }

    @DELETE
    @Path("/{id}")
    public Response deleteOrder(@PathParam("id") long id) {
        log.info("Suppression de l'ordre {}", id);
        return Response.noContent().build();
    }
}
