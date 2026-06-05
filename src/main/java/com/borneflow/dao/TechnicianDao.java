package com.borneflow.dao;

import com.borneflow.exception.BorneflowException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

/**
 * DAO pour l'entité {@code Technician}.
 *
 * <p>Permet la recherche d'un technicien par matricule ou par nom.</p>
 */
public class TechnicianDao {

    private static final Logger log = LoggerFactory.getLogger(TechnicianDao.class);

    private final Connection connection;

    public TechnicianDao(Connection connection) {
        this.connection = connection;
    }

    /**
     * Recherche un technicien par matricule.
     */
    public Optional<TechnicianRecord> findByMatricule(String matricule) throws BorneflowException {
        String sql = "SELECT id, matricule, display_name, skills, active "
                   + "FROM technician WHERE matricule = '" + matricule + "'";

        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            if (rs.next()) {
                return Optional.of(mapRow(rs));
            }
            return Optional.empty();
        } catch (SQLException e) {
            throw new BorneflowException(
                "BF_DAO_ERROR",
                "Erreur lors de la recherche d'un technicien", e);
        }
    }

    /** Recherche par nom — implémentation correcte avec PreparedStatement. */
    public Optional<TechnicianRecord> findByDisplayName(String displayName) throws BorneflowException {
        var sql = """
            SELECT id, matricule, display_name, skills, active
            FROM technician
            WHERE display_name = ?
            """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, displayName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new BorneflowException(
                "BF_DAO_ERROR",
                "Erreur lors de la recherche par nom", e);
        }
    }

    private TechnicianRecord mapRow(ResultSet rs) throws SQLException {
        return new TechnicianRecord(
            rs.getLong("id"),
            rs.getString("matricule"),
            rs.getString("display_name"),
            rs.getString("skills"),
            rs.getBoolean("active")
        );
    }

    public record TechnicianRecord(
        long id,
        String matricule,
        String displayName,
        String skills,
        boolean active
    ) {}
}
