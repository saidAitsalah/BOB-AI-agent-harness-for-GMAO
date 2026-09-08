package com.borneflow.service;

import com.borneflow.dao.MaintenanceOrderDao;
import com.borneflow.dao.MaintenanceOrderDao.OrderRecord;
import com.borneflow.exception.BorneflowException;
import com.borneflow.mbo.MaintenanceOrderMbo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MaintenanceOrderServiceTest {

    private MaintenanceOrderDao dao;
    private MaintenanceOrderService service;

    @BeforeEach
    void setUp() {
        dao = mock(MaintenanceOrderDao.class);
        service = new MaintenanceOrderService(dao);
    }

    // ── findById ──────────────────────────────────────────────────────────────

    @Test
    void shouldReturnOrder_whenFoundById() throws BorneflowException {
        var record = new OrderRecord(42L, "BORNE-001", "TECH00001",
                "SCHEDULED", "HIGH", "Inspection annuelle", null, null);
        when(dao.findById(42L)).thenReturn(Optional.of(record));

        var result = service.findById(42L);

        assertTrue(result.isPresent());
        assertEquals(42L, result.get().id());
        verify(dao).findById(42L);
    }

    @Test
    void shouldReturnEmpty_whenOrderNotFound() throws BorneflowException {
        when(dao.findById(99L)).thenReturn(Optional.empty());

        var result = service.findById(99L);

        assertTrue(result.isEmpty());
    }

    @Test
    void shouldPropagateException_whenDaoFails() throws BorneflowException {
        var cause = new BorneflowException("BF_DAO_ERROR", "Erreur base de données");
        when(dao.findById(1L)).thenThrow(cause);

        var thrown = assertThrows(BorneflowException.class, () -> service.findById(1L));

        assertEquals("BF_DAO_ERROR", thrown.getErrorCode());
    }

    // ── tryFindOrCreate (BF100-RDG-006) ───────────────────────────────────────

    @Test
    void shouldReturnLoadedMbo_whenOrderExists() throws BorneflowException {
        var record = new OrderRecord(10L, "BORNE-002", "TECH00042",
                "IN_PROGRESS", "NORMAL", "Remplacement fusible", null, null);
        when(dao.findById(10L)).thenReturn(Optional.of(record));

        var mbo = service.tryFindOrCreate(10L);

        assertEquals(10L,           mbo.getAttribute("id"));
        assertEquals("BORNE-002",   mbo.getAttribute("stationId"));
        assertEquals(MaintenanceOrderMbo.Status.IN_PROGRESS, mbo.getAttribute("status"));
        verify(dao).findById(10L);
    }

    @Test
    void shouldReturnDraftMbo_whenOrderNotFound() throws BorneflowException {
        // BF100-RDG-006 : ordre absent → brouillon proposé à l'utilisateur
        when(dao.findById(999L)).thenReturn(Optional.empty());

        var mbo = service.tryFindOrCreate(999L);

        assertEquals(MaintenanceOrderMbo.Status.DRAFT, mbo.getAttribute("status"));
        assertTrue(mbo.isNew());
    }

    @Test
    void shouldPropagateException_whenDaoFailsOnTryFindOrCreate() throws BorneflowException {
        // BF100-RDG-006 : erreur technique ≠ absence de résultat
        var cause = new BorneflowException("BF_DAO_ERROR", "Connexion perdue");
        when(dao.findById(5L)).thenThrow(cause);

        var thrown = assertThrows(BorneflowException.class, () -> service.tryFindOrCreate(5L));

        assertEquals("BF_DAO_ERROR", thrown.getErrorCode());
    }

    // ── listForStation ─────────────────────────────────────────────────────────

    @Test
    void shouldRejectListForStation_whenStationIdIsBlank() {
        var ex = assertThrows(BorneflowException.class,
                () -> service.listForStation("", null));
        assertEquals("BF_MO_INVALID", ex.getErrorCode());
    }

    @Test
    void shouldRejectListForStation_whenStationIdIsNull() {
        var ex = assertThrows(BorneflowException.class,
                () -> service.listForStation(null, "SCHEDULED"));
        assertEquals("BF_MO_INVALID", ex.getErrorCode());
    }
}
