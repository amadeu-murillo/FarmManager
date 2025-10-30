package com.farmmanager.model;

import com.farmmanager.util.DateTimeUtil; // NOVO
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class EstoqueDAO {

    /**
     * Adiciona um item ao estoque.
     * Se o item (pelo nome) já existir, atualiza a quantidade,
     * o valor total e recalcula o valor unitário (custo médio ponderado).
     * Se for um novo item, insere.
     * NOVO: Atualiza data_modificacao ou insere data_criacao/data_modificacao.
     */
    public boolean addEstoque(EstoqueItem item) throws SQLException {
        String sqlSelect = "SELECT id, quantidade, valor_total FROM estoque WHERE item_nome = ?";
        // NOVO: SQLs atualizados
        String sqlUpdate = "UPDATE estoque SET quantidade = ?, valor_total = ?, valor_unitario = ?, data_modificacao = ? WHERE id = ?";
        String sqlInsert = "INSERT INTO estoque (item_nome, quantidade, unidade, valor_unitario, valor_total, data_criacao, data_modificacao) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); // Inicia transação
            
            String now = DateTimeUtil.getCurrentTimestamp(); // NOVO

            try (PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect)) {
                pstmtSelect.setString(1, item.getItemNome());
                
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next()) {
                        // --- ITEM EXISTE (UPDATE) ---
                        int id = rs.getInt("id");
                        double oldQty = rs.getDouble("quantidade");
                        double oldTotalVal = rs.getDouble("valor_total");

                        double newQty = oldQty + item.getQuantidade();
                        double newTotalVal = oldTotalVal + item.getValorTotal();
                        // Custo médio ponderado
                        double newUnitVal = (newQty > 0) ? (newTotalVal / newQty) : 0; 

                        try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {
                            pstmtUpdate.setDouble(1, newQty);
                            pstmtUpdate.setDouble(2, newTotalVal);
                            pstmtUpdate.setDouble(3, newUnitVal);
                            pstmtUpdate.setString(4, now); // NOVO
                            pstmtUpdate.setInt(5, id); // NOVO (índice mudou)
                            pstmtUpdate.executeUpdate();
                        }
                    } else {
                        // --- ITEM NÃO EXISTE (INSERT) ---
                        try (PreparedStatement pstmtInsert = conn.prepareStatement(sqlInsert)) {
                            pstmtInsert.setString(1, item.getItemNome());
                            pstmtInsert.setDouble(2, item.getQuantidade());
                            pstmtInsert.setString(3, item.getUnidade());
                            pstmtInsert.setDouble(4, item.getValorUnitario());
                            pstmtInsert.setDouble(5, item.getValorTotal());
                            pstmtInsert.setString(6, now); // NOVO
                            pstmtInsert.setString(7, now); // NOVO
                            pstmtInsert.executeUpdate();
                        }
                    }
                }
                
                conn.commit(); // Efetiva a transação
                return true;

            } catch (SQLException e) {
                conn.rollback(); // Desfaz em caso de erro
                throw e; // Repassa a exceção
            }
        }
    }

    /**
     * Consome (dá baixa) em uma quantidade específica de um item.
     * Recalcula o valor_total com base no valor_unitário (custo médio).
     * NOVO: Atualiza data_modificacao.
     * @throws IllegalStateException Se não houver estoque suficiente.
     */
    public boolean consumirEstoque(int id, double quantidadeAConsumir) throws SQLException, IllegalStateException {
        String sqlSelect = "SELECT quantidade, valor_unitario FROM estoque WHERE id = ?";
        // NOVO: SQL atualizado
        String sqlUpdate = "UPDATE estoque SET quantidade = ?, valor_total = ?, data_modificacao = ? WHERE id = ?";

        try (Connection conn = Database.getConnection()) {
            conn.setAutoCommit(false); 
            
            String now = DateTimeUtil.getCurrentTimestamp(); // NOVO
            double quantidadeAtual = 0;
            double valorUnitario = 0;

            // 1. Verifica o estoque atual e o valor unitário
            try (PreparedStatement pstmtSelect = conn.prepareStatement(sqlSelect)) {
                pstmtSelect.setInt(1, id);
                try (ResultSet rs = pstmtSelect.executeQuery()) {
                    if (rs.next()) {
                        quantidadeAtual = rs.getDouble("quantidade");
                        valorUnitario = rs.getDouble("valor_unitario");
                    } else {
                        throw new SQLException("Item não encontrado no estoque, ID: " + id);
                    }
                }
            }

            // 2. Valida se há estoque suficiente
            if (quantidadeAtual < quantidadeAConsumir) {
                conn.rollback(); 
                throw new IllegalStateException("Estoque insuficiente. Disponível: " + quantidadeAtual + ", Tentativa de consumo: " + quantidadeAConsumir);
            }

            // 3. Calcula novos valores
            double novaQuantidade = quantidadeAtual - quantidadeAConsumir;
            double novoValorTotal = novaQuantidade * valorUnitario; // Atualiza o valor total baseado no custo médio

            // 4. Atualiza o estoque
            try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdate)) {
                pstmtUpdate.setDouble(1, novaQuantidade);
                pstmtUpdate.setDouble(2, novoValorTotal);
                pstmtUpdate.setString(3, now); // NOVO
                pstmtUpdate.setInt(4, id); // NOVO (índice mudou)
                int rowsAffected = pstmtUpdate.executeUpdate();
                
                conn.commit(); // Efetiva a transação
                return rowsAffected > 0;
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }


    /**
     * Remove um item do estoque pelo ID.
     */
    public boolean removerItemEstoque(int id) throws SQLException {
        String sql = "DELETE FROM estoque WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * NOVO: Busca um item de estoque específico pelo seu ID.
     * ATUALIZADO: Inclui datas.
     */
    public EstoqueItem getItemById(int id) throws SQLException {
        String sql = "SELECT * FROM estoque WHERE id = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new EstoqueItem(
                        rs.getInt("id"),
                        rs.getString("item_nome"),
                        rs.getDouble("quantidade"),
                        rs.getString("unidade"),
                        rs.getDouble("valor_unitario"),
                        rs.getDouble("valor_total"),
                        rs.getString("data_criacao"), // NOVO
                        rs.getString("data_modificacao") // NOVO
                    );
                }
            }
        }
        return null; // Não encontrado
    }

    /**
     * NOVO: Busca um item de estoque específico pelo seu NOME.
     * ATUALIZADO: Inclui datas.
     */
    public EstoqueItem getEstoqueItemPorNome(String nome) throws SQLException {
        String sql = "SELECT * FROM estoque WHERE item_nome = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, nome);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new EstoqueItem(
                        rs.getInt("id"),
                        rs.getString("item_nome"),
                        rs.getDouble("quantidade"),
                        rs.getString("unidade"),
                        rs.getDouble("valor_unitario"),
                        rs.getDouble("valor_total"),
                        rs.getString("data_criacao"), // NOVO
                        rs.getString("data_modificacao") // NOVO
                    );
                }
            }
        }
        return null; // Não encontrado
    }

    /**
     * ATUALIZADO: Seleciona e popula os novos campos de data.
     */
    public List<EstoqueItem> listEstoque() throws SQLException {
        List<EstoqueItem> items = new ArrayList<>();
        // ATUALIZADO: Seleciona novos campos
        String sql = "SELECT id, item_nome, quantidade, unidade, valor_unitario, valor_total, data_criacao, data_modificacao FROM estoque";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                EstoqueItem item = new EstoqueItem(
                    rs.getInt("id"),
                    rs.getString("item_nome"),
                    rs.getDouble("quantidade"),
                    rs.getString("unidade"),
                    rs.getDouble("valor_unitario"), // NOVO
                    rs.getDouble("valor_total"), // NOVO
                    rs.getString("data_criacao"), // NOVO
                    rs.getString("data_modificacao") // NOVO
                );
                items.add(item);
            }
        }
        return items;
    }
    
    /**
     * Retorna a contagem de itens distintos no estoque.
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

    /**
     * Retorna o nome de um item baseado no ID.
     * Usado pelo SafrasController para preencher a tabela de detalhes.
     */
    public String getItemNomeById(int id) throws SQLException {
        String sql = "SELECT item_nome FROM estoque WHERE id = ?";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("item_nome");
                }
            }
        }
        return "ID: " + id + " (Não encontrado)";
    }
}

