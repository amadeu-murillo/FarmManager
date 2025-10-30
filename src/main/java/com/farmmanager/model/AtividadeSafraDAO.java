package com.farmmanager.model;

import com.farmmanager.util.DateTimeUtil; // NOVO
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;

/**
 * NOVO: DAO para gerenciar a tabela 'atividades_safra'.
 * NOVO: Adicionado data_hora_criacao.
 */
public class AtividadeSafraDAO {

    /**
     * Adiciona uma nova atividade/custo ao banco de dados.
     */
    public boolean addAtividade(AtividadeSafra atividade) throws SQLException {
        // NOVO: SQL atualizado
        String sql = "INSERT INTO atividades_safra (safra_id, descricao, data, item_consumido_id, quantidade_consumida, custo_total_atividade, data_hora_criacao) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, atividade.getSafraId());
            pstmt.setString(2, atividade.getDescricao());
            pstmt.setString(3, atividade.getData());
            
            if (atividade.getItemConsumidoId() != null) {
                pstmt.setInt(4, atividade.getItemConsumidoId());
                pstmt.setDouble(5, atividade.getQuantidadeConsumida());
            } else {
                pstmt.setNull(4, Types.INTEGER);
                pstmt.setDouble(5, 0.0);
            }
            
            pstmt.setDouble(6, atividade.getCustoTotalAtividade());
            pstmt.setString(7, DateTimeUtil.getCurrentTimestamp()); // NOVO
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Lista todas as atividades de uma safra específica.
     * (Será usado na Etapa 3)
     */
    public List<AtividadeSafra> listAtividadesPorSafra(int safraId) throws SQLException {
        List<AtividadeSafra> atividades = new ArrayList<>();
        String sql = "SELECT * FROM atividades_safra WHERE safra_id = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, safraId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Trata o item_consumido_id nulo
                    Integer itemConsumidoId = (Integer) rs.getObject("item_consumido_id");

                    AtividadeSafra atv = new AtividadeSafra(
                        rs.getInt("id"),
                        rs.getInt("safra_id"),
                        rs.getString("descricao"),
                        rs.getString("data"),
                        itemConsumidoId,
                        rs.getDouble("quantidade_consumida"),
                        rs.getDouble("custo_total_atividade")
                    );
                    atividades.add(atv);
                }
            }
        }
        return atividades;
    }

    /**
     * Calcula o custo total (soma de todas as atividades) de uma safra.
     * (Será usado na Etapa 3)
     */
    public double getCustoTotalPorSafra(int safraId) throws SQLException {
        String sql = "SELECT SUM(custo_total_atividade) AS custo_total FROM atividades_safra WHERE safra_id = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, safraId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("custo_total");
                }
            }
        }
        return 0.0;
    }
}
