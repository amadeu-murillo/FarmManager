package com.farmmanager.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EstoqueDAO {

    /**
     * Adiciona ou atualiza um item no estoque (lógica de UPSERT).
     * Se o item já existir (pelo nome), soma a quantidade.
     * Se for um novo item, insere.
     */
    public boolean addEstoque(String itemNome, double quantidade, String unidade) throws SQLException {
        // Tenta atualizar primeiro
        String sqlUpdate = "UPDATE estoque SET quantidade = quantidade + ? WHERE item_nome = ?";
        // Se não atualizar (0 linhas afetadas), insere
        String sqlInsert = "INSERT INTO estoque (item_nome, quantidade, unidade) VALUES (?, ?, ?)";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); // Inicia transação

            try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {
                pstmtUpdate.setDouble(1, quantidade);
                pstmtUpdate.setString(2, itemNome);
                int rowsAffected = pstmtUpdate.executeUpdate();

                if (rowsAffected == 0) {
                    // Item não existe, vamos inserir
                    try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
                        pstmtInsert.setString(1, itemNome);
                        pstmtInsert.setDouble(2, quantidade);
                        pstmtInsert.setString(3, unidade);
                        rowsAffected = pstmtInsert.executeUpdate();
                    }
                }
                
                conn.commit(); // Efetiva a transação
                return rowsAffected > 0;

            } catch (SQLException e) {
                conn.rollback(); // Desfaz em caso de erro
                throw e; // Repassa a exceção
            }
        }
    }

    /**
     * NOVO: Remove um item do estoque pelo ID.
     */
    public boolean removerItemEstoque(int id) throws SQLException {
        String sql = "DELETE FROM estoque WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<EstoqueItem> listEstoque() throws SQLException {
        List<EstoqueItem> items = new ArrayList<>();
        String sql = "SELECT id, item_nome, quantidade, unidade FROM estoque";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                EstoqueItem item = new EstoqueItem(
                    rs.getInt("id"),
                    rs.getString("item_nome"),
                    rs.getDouble("quantidade"),
                    rs.getString("unidade")
                );
                items.add(item);
            }
        }
        return items;
    }
    
    /**
     * NOVO: Retorna a contagem de itens distintos no estoque.
     * Usado pelo Dashboard.
     */
    public int getContagemItensDistintos() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM estoque";
        
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
