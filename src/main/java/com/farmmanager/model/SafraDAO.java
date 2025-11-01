package com.farmmanager.model;

import com.farmmanager.util.DateTimeUtil; // NOVO
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.format.DateTimeFormatter; 
import java.util.ArrayList;
import java.util.LinkedHashMap; // NOVO
import java.util.List;
import java.util.Map; // NOVO

/**
 * ATUALIZADO:
 * - addSafra: Salva ano_inicio como TEXT e salva o novo campo 'status'.
 * - updateProducaoSafra: Define status como 'Colhida' ao registrar produção.
 * - listSafrasComInfo: Carrega o 'status' e o 'ano_inicio' (TEXT).
 * - getContagemSafrasAtivas: Conta safras onde status != 'Colhida'.
 * - Adicionado updateStatusSafra.
 * - NOVO: Adicionado data_criacao e data_modificacao.
 */
public class SafraDAO {

    // NOVO: Exposto para ser usado por outros controllers
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public DateTimeFormatter getDateFormatter() {
        return dateFormatter;
    }

    public boolean addSafra(Safra safra) throws SQLException {
        // SQL atualizado com 'status' e 4 parâmetros
        // NOVO: Adicionado data_criacao e data_modificacao
        String sql = "INSERT INTO safras(cultura, ano_inicio, talhao_id, status, data_criacao, data_modificacao) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String now = DateTimeUtil.getCurrentTimestamp(); // NOVO
            
            pstmt.setString(1, safra.getCultura());
            pstmt.setString(2, safra.getAnoInicio()); // Alterado para setString
            pstmt.setInt(3, safra.getTalhaoId());
            pstmt.setString(4, safra.getStatus()); // NOVO
            pstmt.setString(5, now); // NOVO
            pstmt.setString(6, now); // NOVO
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Atualiza a produção total de uma safra existente e define o status para 'Colhida'.
     * NOVO: Atualiza data_modificacao.
     */
    public boolean updateProducaoSafra(int safraId, double producaoKg) throws SQLException {
        // SQL atualizado para incluir a mudança de status e data_modificacao
        String sql = "UPDATE safras SET producao_total_kg = ?, status = 'Colhida', data_modificacao = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setDouble(1, producaoKg);
            pstmt.setString(2, DateTimeUtil.getCurrentTimestamp()); // NOVO
            pstmt.setInt(3, safraId); // NOVO (índice mudou)
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * NOVO: Atualiza apenas o status de uma safra.
     * NOVO: Atualiza data_modificacao.
     */
    public boolean updateStatusSafra(int safraId, String novoStatus) throws SQLException {
        String sql = "UPDATE safras SET status = ?, data_modificacao = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, novoStatus);
            pstmt.setString(2, DateTimeUtil.getCurrentTimestamp()); // NOVO
            pstmt.setInt(3, safraId); // NOVO (índice mudou)
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
        // ATUALIZAÇÃO 2: Adicionado s.data_modificacao (será a data da colheita para safras colhidas)
        String sql = "SELECT s.id, s.cultura, s.ano_inicio, s.producao_total_kg, t.nome as talhao_nome, t.area_hectares, s.status, s.data_modificacao "
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
                    rs.getString("status"), // NOVO
                    rs.getString("data_modificacao") // NOVO
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

    /**
     * NOVO: Retorna um mapa das culturas ativas e suas respectivas contagens.
     * Usado pelo Gráfico de Pizza de Culturas no Dashboard.
     */
    public Map<String, Integer> getContagemCulturasAtivas() throws SQLException {
        Map<String, Integer> contagemCulturas = new LinkedHashMap<>();
        String sql = "SELECT cultura, COUNT(*) AS total FROM safras WHERE status != 'Colhida' GROUP BY cultura ORDER BY total DESC";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            while (rs.next()) {
                contagemCulturas.put(rs.getString("cultura"), rs.getInt("total"));
            }
        }
        return contagemCulturas;
    }
}

