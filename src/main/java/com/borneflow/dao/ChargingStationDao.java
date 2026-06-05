package com.borneflow.dao;

import com.borneflow.exception.BorneflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO pour l'entité {@code ChargingStation}.
 *
 * <p>Cette classe sert de référence d'implémentation correcte :
 * toutes les requêtes sont paramétrées, les ressources sont fermées
 * via try-with-resources, et les colonnes sont explicitement énumérées.</p>
 */
public class ChargingStationDao {

    private static final Logger log = LoggerFactory.getLogger(ChargingStationDao.class);

    private final Connection connection;

    public ChargingStationDao(Connection connection) {
        this.connection = connection;
    }

    /** Recherche d'une borne par identifiant externe — requête paramétrée. */
    public Optional<StationRecord> findByExternalId(String externalId) throws BorneflowException {
        var sql = """
            SELECT id, external_id, label, power_kw, status, location
            FROM charging_station
            WHERE external_id = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, externalId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new BorneflowException(
                "BF_DAO_ERROR",
                "Erreur lors de la recherche d'une borne",
                externalId, e);
        }
    }

    /** Liste les bornes d'un statut donné — requête paramétrée. */
    public List<StationRecord> findByStatus(String status) throws BorneflowException {
        var sql = """
            SELECT id, external_id, label, power_kw, status, location
            FROM charging_station
            WHERE status = ?
            ORDER BY external_id
            """;

        var results = new ArrayList<StationRecord>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new BorneflowException(
                "BF_DAO_ERROR",
                "Erreur lors de la recherche par statut", e);
        }
        return results;
    }

    private StationRecord mapRow(ResultSet rs) throws SQLException {
        return new StationRecord(
            rs.getLong("id"),
            rs.getString("external_id"),
            rs.getString("label"),
            rs.getDouble("power_kw"),
            rs.getString("status"),
            rs.getString("location")
        );
    }

    /** Projection en lecture, pas de mutation. */
    public record StationRecord(
        long id,
        String externalId,
        String label,
        double powerKw,
        String status,
        String location
    ) {}
}
