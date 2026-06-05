package com.borneflow.service;

import com.borneflow.dao.ChargingStationDao;
import com.borneflow.dao.ChargingStationDao.StationRecord;
import com.borneflow.exception.BorneflowException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class ChargingStationServiceTest {

    private ChargingStationDao dao;
    private ChargingStationService service;

    @BeforeEach
    void setUp() {
        dao = mock(ChargingStationDao.class);
        service = new ChargingStationService(dao);
    }

    @Test
    void shouldRejectLookup_whenExternalIdIsBlank() {
        var ex = assertThrows(BorneflowException.class,
            () -> service.getByExternalId(""));
        assertEquals("BF_STATION_INVALID", ex.getErrorCode());
    }

    @Test
    void shouldReturnEmpty_whenStationNotFound() throws BorneflowException {
        when(dao.findByExternalId(anyString())).thenReturn(Optional.empty());
        var result = service.getByExternalId("BORNE-UNKNOWN");
        assertTrue(result.isEmpty());
    }

    @Test
    void shouldDelegateToDao_whenListingOperational() throws BorneflowException {
        when(dao.findByStatus("OPERATIONAL")).thenReturn(List.of(
            new StationRecord(1L, "BORNE-001", "Lyon-Part-Dieu", 22.0, "OPERATIONAL", "Lyon")
        ));
        var stations = service.listOperational();
        assertEquals(1, stations.size());
        verify(dao).findByStatus("OPERATIONAL");
    }
}
