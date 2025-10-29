package com.farmmanager.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class SafraDAO {

    public boolean addSafra(Safra safra) throws SQLException {
        String sql = "INSERT INTO safras(cultura, ano_inicio, talhao_id) VALUES(?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, safra.getCultura());
            pstmt.setInt(2, safra.getAnoInicio());
            pstmt.setInt(3, safra.getTalhaoId());
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Lista safras com informações do talhão (JOIN).
     * Retorna um DTO (SafraInfo) ao invés da entidade Safra pura.
     */
    public List<SafraInfo> listSafrasComInfo() throws SQLException {
        List<SafraInfo> safras = new ArrayList<>();
        // CORREÇÃO: Removido Text Block (aspas triplas) para compatibilidade com Java 11
        String sql = "SELECT s.id, s.cultura, s.ano_inicio, s.producao_total_kg, t.nome as talhao_nome "
            + "FROM safras s "
            + "JOIN talhoes t ON s.talhao_id = t.id";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SafraInfo si = new SafraInfo(
                    rs.getInt("id"),
                    rs.getString("cultura"),
                    rs.getInt("ano_inicio"),
                    rs.getString("talhao_nome"),
                    rs.getDouble("producao_total_kg")
                );
                safras.add(si);
            }
        }
        return safras;
    }
}

