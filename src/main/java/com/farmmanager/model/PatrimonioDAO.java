package com.farmmanager.model;

import com.farmmanager.util.DateTimeUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * NOVO: DAO para gerenciar a tabela 'patrimonio'.
 * ATUALIZADO: Adicionado updateStatus.
 */
public class PatrimonioDAO {

    /**
     * Adiciona um novo ativo (máquina) ao banco de dados.
     */
    public boolean addPatrimonio(Patrimonio patrimonio) throws SQLException {
        String sql = "INSERT INTO patrimonio(nome, tipo, data_aquisicao, valor_aquisicao, status, data_criacao, data_modificacao) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String now = DateTimeUtil.getCurrentTimestamp();
            
            pstmt.setString(1, patrimonio.getNome());
            pstmt.setString(2, patrimonio.getTipo());
            pstmt.setString(3, patrimonio.getDataAquisicao());
            pstmt.setDouble(4, patrimonio.getValorAquisicao());
            pstmt.setString(5, patrimonio.getStatus());
            pstmt.setString(6, now);
            pstmt.setString(7, now);
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * NOVO: Atualiza apenas o status de um ativo.
     */
    public boolean updateStatus(int id, String novoStatus) throws SQLException {
        String sql = "UPDATE patrimonio SET status = ?, data_modificacao = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, novoStatus);
            pstmt.setString(2, DateTimeUtil.getCurrentTimestamp());
            pstmt.setInt(3, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Remove um ativo do banco de dados.
     */
    public boolean removerPatrimonio(int id) throws SQLException {
        String sql = "DELETE FROM patrimonio WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Lista todos os ativos (máquinas) do banco de dados.
     */
    public List<Patrimonio> listPatrimonio() throws SQLException {
        List<Patrimonio> lista = new ArrayList<>();
        String sql = "SELECT * FROM patrimonio ORDER BY nome ASC";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Patrimonio p = new Patrimonio(
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getString("tipo"),
                    rs.getString("data_aquisicao"),
                    rs.getDouble("valor_aquisicao"),
                    rs.getString("status"),
                    rs.getString("data_criacao"),
                    rs.getString("data_modificacao")
                );
                lista.add(p);
            }
        }
        return lista;
    }
    
    // TODO: Adicionar métodos para updateStatus, registrarManutencao, etc.
}

