package com.farmmanager.model;

import com.farmmanager.util.DateTimeUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * NOVO: DAO para gerenciar a tabela 'manutencao_patrimonio'.
 */
public class ManutencaoDAO {

    /**
     * Adiciona um novo registro de manutenção.
     */
    public boolean addManutencao(Manutencao manutencao) throws SQLException {
        String sql = "INSERT INTO manutencao_patrimonio (patrimonio_id, data, descricao, custo, data_hora_criacao) "
                   + "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, manutencao.getPatrimonioId());
            pstmt.setString(2, manutencao.getData());
            pstmt.setString(3, manutencao.getDescricao());
            pstmt.setDouble(4, manutencao.getCusto());
            pstmt.setString(5, DateTimeUtil.getCurrentTimestamp());
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Lista todos os registros de manutenção para um ativo específico.
     */
    public List<Manutencao> listManutencaoPorPatrimonio(int patrimonioId) throws SQLException {
        List<Manutencao> lista = new ArrayList<>();
        String sql = "SELECT * FROM manutencao_patrimonio WHERE patrimonio_id = ? ORDER BY data DESC";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, patrimonioId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Manutencao m = new Manutencao(
                        rs.getInt("id"),
                        rs.getInt("patrimonio_id"),
                        rs.getString("data"),
                        rs.getString("descricao"),
                        rs.getDouble("custo")
                    );
                    lista.add(m);
                }
            }
        }
        return lista;
    }

    /**
     * Calcula o custo total (soma de todas as manutenções) de um ativo.
     */
    public double getCustoTotalManutencao(int patrimonioId) throws SQLException {
        String sql = "SELECT SUM(custo) AS custo_total FROM manutencao_patrimonio WHERE patrimonio_id = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, patrimonioId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("custo_total");
                }
            }
        }
        return 0.0;
    }
}
