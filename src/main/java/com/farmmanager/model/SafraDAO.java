package com.farmmanager.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter; // NOVO
import java.util.ArrayList;
import java.util.List;

/**
 * ATUALIZADO:
 * - addSafra: Salva ano_inicio como TEXT e salva o novo campo 'status'.
 * - updateProducaoSafra: Define status como 'Colhida' ao registrar produção.
 * - listSafrasComInfo: Carrega o 'status' e o 'ano_inicio' (TEXT).
 * - getContagemSafrasAtivas: Conta safras onde status != 'Colhida'.
 * - Adicionado updateStatusSafra.
 */
public class SafraDAO {

    // NOVO: Exposto para ser usado por outros controllers
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }

    public boolean addSafra(Safra safra) throws SQLException {
        // SQL atualizado com 'status' e 4 parâmetros
        String sql = "INSERT INTO safras(cultura, ano_inicio, talhao_id, status) VALUES(?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, safra.getCultura());
            pstmt.setString(2, safra.getAnoInicio()); // Alterado para setString
            pstmt.setInt(3, safra.getTalhaoId());
            pstmt.setString(4, safra.getStatus()); // NOVO
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Atualiza a produção total de uma safra existente e define o status para 'Colhida'.
     */
    public boolean updateProducaoSafra(int safraId, double producaoKg) throws SQLException {
        // SQL atualizado para incluir a mudança de status
        String sql = "UPDATE safras SET producao_total_kg = ?, status = 'Colhida' WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, producaoKg);
            pstmt.setInt(2, safraId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * NOVO: Atualiza apenas o status de uma safra.
     */
    public boolean updateStatusSafra(int safraId, String novoStatus) throws SQLException {
        String sql = "UPDATE safras SET status = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, novoStatus);
            pstmt.setInt(2, safraId);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Remove uma safra pelo ID.
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
        // ATUALIZAÇÃO: Adicionado s.status e t.area_hectares à consulta
        String sql = "SELECT s.id, s.cultura, s.ano_inicio, s.producao_total_kg, t.nome as talhao_nome, t.area_hectares, s.status "
            + "FROM safras s "
            + "JOIN talhoes t ON s.talhao_id = t.id";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                SafraInfo si = new SafraInfo(
                    rs.getInt("id"),
                    rs.getString("cultura"),
                    rs.getString("ano_inicio"), // Alterado para getString
                    rs.getString("talhao_nome"),
                    rs.getDouble("producao_total_kg"),
                    rs.getDouble("area_hectares"),
                    rs.getString("status") // NOVO
                );
                safras.add(si);
            }
        }
        return safras;
    }

    /**
     * Retorna a contagem de safras "ativas" (status diferente de 'Colhida').
     * Usado pelo Dashboard.
     */
    public int getContagemSafrasAtivas() throws SQLException {
        // Query atualizada para usar o status
        String sql = "SELECT COUNT(*) AS total FROM safras WHERE status != 'Colhida'";
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

