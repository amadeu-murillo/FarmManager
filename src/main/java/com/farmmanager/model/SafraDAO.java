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
     * NOVO: Atualiza a produção total de uma safra existente.
     * Usado para registrar a colheita.
     */
    public boolean updateProducaoSafra(int safraId, double producaoKg) throws SQLException {
        String sql = "UPDATE safras SET producao_total_kg = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, producaoKg);
            pstmt.setInt(2, safraId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * NOVO: Remove uma safra pelo ID.
     */
    public boolean removerSafra(int safraId) throws SQLException {
        String sql = "DELETE FROM safras WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, safraId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Lista safras com informações do talhão (JOIN).
     * Retorna um DTO (SafraInfo) ao invés da entidade Safra pura.
     */
    public List<SafraInfo> listSafrasComInfo() throws SQLException {
        List<SafraInfo> safras = new ArrayList<>();
        // ATUALIZAÇÃO: Adicionado t.area_hectares à consulta
        String sql = "SELECT s.id, s.cultura, s.ano_inicio, s.producao_total_kg, t.nome as talhao_nome, t.area_hectares "
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
                    rs.getDouble("producao_total_kg"),
                    rs.getDouble("area_hectares") // ATUALIZAÇÃO: Passando a área para o construtor
                );
                safras.add(si);
            }
        }
        return safras;
    }

    /**
     * NOVO: Retorna a contagem de safras "ativas" (sem produção registrada).
     * Usado pelo Dashboard.
     */
    public int getContagemSafrasAtivas() throws SQLException {
        // Considera safras "ativas" as que ainda não tiveram produção registrada (produção = 0)
        String sql = "SELECT COUNT(*) AS total FROM safras WHERE producao_total_kg = 0 OR producao_total_kg IS NULL";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("total");
            }
        }
        return 0;
    }
}

