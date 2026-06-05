package com.borneflow.mbo;

import com.borneflow.exception.BorneflowException;
import com.borneflow.mbo.ChargingStationMbo.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChargingStationMboTest {

    private ChargingStationMbo station;

    @BeforeEach
    void setUp() throws BorneflowException {
        station = new ChargingStationMbo();
        station.add();
        station.setAttribute("externalId", "BORNE-TEST-0001");
        station.setAttribute("powerKw", 22.0);
    }

    @Test
    void shouldInitializeWithCommissionedStatus_whenAdded() {
        assertEquals(Status.COMMISSIONED, station.getAttribute("status"));
        assertEquals(0L, station.getAttribute("totalSessions"));
    }

    @Test
    void shouldAllowTransitionCommissionedToOperational() throws BorneflowException {
        station.changeStatus(Status.OPERATIONAL);
        assertEquals(Status.OPERATIONAL, station.getAttribute("status"));
    }

    @Test
    void shouldRejectTransitionDecommissionedToOperational() throws BorneflowException {
        station.changeStatus(Status.DECOMMISSIONED);
        var ex = assertThrows(BorneflowException.class,
            () -> station.changeStatus(Status.OPERATIONAL));
        assertEquals("BF_STATION_INVALID_TRANSITION", ex.getErrorCode());
    }

    @Test
    void shouldRejectSave_whenExternalIdMissing() {
        var s = new ChargingStationMbo();
        try {
            s.add();
            s.setAttribute("powerKw", 22.0);
        } catch (BorneflowException e) {
            fail("setup should not throw");
        }
        var ex = assertThrows(BorneflowException.class, s::save);
        assertEquals("BF_STATION_INVALID", ex.getErrorCode());
    }

    @Test
    void shouldRejectSave_whenPowerKwIsZeroOrNegative() {
        station.setAttribute("powerKw", 0.0);
        var ex = assertThrows(BorneflowException.class, () -> station.save());
        assertEquals("BF_STATION_INVALID", ex.getErrorCode());
    }
}
