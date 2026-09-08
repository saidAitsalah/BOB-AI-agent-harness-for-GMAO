package com.borneflow.api;

import com.borneflow.dao.ChargingStationDao.StationRecord;
import com.borneflow.exception.BorneflowException;
import com.borneflow.service.ChargingStationService;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Ressource REST exposant les bornes de recharge.
 *
  */
@Path("/api/v1/stations")
@Produces(MediaType.APPLICATION_JSON)
public class ChargingStationResource {

    private static final Logger log = LoggerFactory.getLogger(ChargingStationResource.class);

    private final ChargingStationService service;
    private final AuthorizationContext authz;

    public ChargingStationResource(ChargingStationService service, AuthorizationContext authz) {
        this.service = service;
        this.authz = authz;
    }

    @GET
    @Path("/{externalId}")
    public Response getByExternalId(@PathParam("externalId") String externalId) {
        // Vérification d'accès en lecture
        if (!authz.canRead("station", externalId)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            var result = service.getByExternalId(externalId);
            return result
                .map(s -> Response.ok(s).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
        } catch (BorneflowException e) {
            log.error("Erreur recherche borne", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GET
    @Path("/operational")
    public Response listOperational() {
        if (!authz.canRead("station", null)) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        try {
            List<StationRecord> stations = service.listOperational();
            return Response.ok(stations).build();
        } catch (BorneflowException e) {
            log.error("Erreur liste bornes opérationnelles", e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }
}
