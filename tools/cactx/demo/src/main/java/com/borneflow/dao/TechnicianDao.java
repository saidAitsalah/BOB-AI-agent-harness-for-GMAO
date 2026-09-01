package com.borneflow.dao;
import org.springframework.jdbc.core.JdbcTemplate;
public class TechnicianDao {
    private JdbcTemplate jdbcTemplate;
    public Technician findByMatricule(String matricule) {
        String sql = "SELECT * FROM technician WHERE matricule = '" + matricule + "'";
        return jdbcTemplate.queryForObject(sql, technicianRowMapper);
    }
    public void save(Technician t) { }
}
