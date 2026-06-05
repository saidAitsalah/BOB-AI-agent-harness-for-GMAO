package com.borneflow.dao;

import com.borneflow.exception.BorneflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pour l'entité {@code MaintenanceOrder}.
 *
 * <p>Toutes les requêtes utilisent {@link PreparedStatement}.</p>
 */
public class MaintenanceOrderDao {

    private static final Logger log = LoggerFactory.getLogger(MaintenanceOrderDao.class);

    private final Connection connection;

    public MaintenanceOrderDao(Connection connection) {
        this.connection = connection;
    }

    public Optional<OrderRecord> findById(long id) throws BorneflowException {
        var sql = """
            SELECT id, station_id, technician_id, status, priority,
                   description, scheduled_at, completed_at
            FROM maintenance_order
            WHERE id = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new BorneflowException("BF_DAO_ERROR",
                "Erreur lors de la recherche d'une maintenance", e);
        }
    }

    public List<OrderRecord> findByStationAndStatus(String stationId, String status) throws BorneflowException {
        var sql = """
            SELECT id, station_id, technician_id, status, priority,
                   description, scheduled_at, completed_at
            FROM maintenance_order
            WHERE station_id = ? AND status = ?
            ORDER BY scheduled_at DESC
            """;
        var results = new ArrayList<OrderRecord>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, stationId);
            ps.setString(2, status);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new BorneflowException("BF_DAO_ERROR",
                "Erreur lors de la recherche par borne+statut", e);
        }
        return results;
    }

    public List<OrderRecord> findScheduledBefore(Instant deadline) throws BorneflowException {
        var sql = """
            SELECT id, station_id, technician_id, status, priority,
                   description, scheduled_at, completed_at
            FROM maintenance_order
            WHERE status = 'SCHEDULED' AND scheduled_at <= ?
            """;
        var results = new ArrayList<OrderRecord>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(deadline));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new BorneflowException("BF_DAO_ERROR",
                "Erreur lors de la recherche par échéance", e);
        }
        return results;
    }

    private OrderRecord mapRow(ResultSet rs) throws SQLException {
        var scheduledTs = rs.getTimestamp("scheduled_at");
        var completedTs = rs.getTimestamp("completed_at");
        return new OrderRecord(
            rs.getLong("id"),
            rs.getString("station_id"),
            rs.getString("technician_id"),
            rs.getString("status"),
            rs.getString("priority"),
            rs.getString("description"),
            scheduledTs != null ? scheduledTs.toInstant() : null,
            completedTs != null ? completedTs.toInstant() : null
        );
    }

    public record OrderRecord(
        long id,
        String stationId,
        String technicianId,
        String status,
        String priority,
        String description,
        Instant scheduledAt,
        Instant completedAt
    ) {}
}
