package com.farmmanager.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class TalhaoDAO {

    public boolean addTalhao(Talhao talhao) throws SQLException {
        String sql = "INSERT INTO talhoes(nome, area_hectares) VALUES(?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, talhao.getNome());
            pstmt.setDouble(2, talhao.getAreaHectares());
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Talhao> listTalhoes() throws SQLException {
        List<Talhao> talhoes = new ArrayList<>();
        String sql = "SELECT id, nome, area_hectares FROM talhoes";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Talhao t = new Talhao(
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getDouble("area_hectares")
                );
                talhoes.add(t);
            }
        }
        return talhoes;
    }

    /**
     * NOVO: Retorna a contagem total de talhões.
     * Usado pelo Dashboard.
     */
    public int getContagemTalhoes() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM talhoes";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        return 0;
    }

    /**
     * NOVO: Retorna a soma da área de todos os talhões.
     * Usado pelo Dashboard.
     */
    public double getTotalAreaHectares() throws SQLException {
        String sql = "SELECT SUM(area_hectares) AS total_area FROM talhoes";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getDouble("total_area");
            }
        }
        return 0.0;
    }
}
