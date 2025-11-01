package com.farmmanager.model;

import com.farmmanager.util.DateTimeUtil; 
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap; 
import java.util.List;
import java.util.Map; 

public class FinanceiroDAO {

    public boolean addTransacao(Transacao transacao) throws SQLException {
        // NOVO: SQL atualizado com data_hora_criacao
        String sql = "INSERT INTO financeiro(descricao, valor, data, tipo, data_hora_criacao, data_modificacao) VALUES(?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection(); // CORRIGIDO
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String now = DateTimeUtil.getCurrentTimestamp(); // NOVO
            pstmt.setString(1, transacao.getDescricao());
            pstmt.setDouble(2, transacao.getValor());
            pstmt.setString(3, transacao.getData());
            pstmt.setString(4, transacao.getTipo());
            pstmt.setString(5, now); // NOVO: data_hora_criacao
            pstmt.setString(6, now); // NOVO: data_modificacao
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * NOVO: Atualiza uma transação existente no banco de dados.
     */
    public boolean updateTransacao(Transacao transacao) throws SQLException {
        String sql = "UPDATE financeiro SET descricao = ?, valor = ?, data = ?, tipo = ?, data_modificacao = ? WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, transacao.getDescricao());
            pstmt.setDouble(2, transacao.getValor());
            pstmt.setString(3, transacao.getData());
            pstmt.setString(4, transacao.getTipo());
            pstmt.setString(5, DateTimeUtil.getCurrentTimestamp()); // Atualiza o timestamp
            pstmt.setInt(6, transacao.getId()); // Cláusula WHERE
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * NOVO: Remove uma transação do banco de dados pelo ID.
     */
    public boolean removerTransacao(int id) throws SQLException {
        String sql = "DELETE FROM financeiro WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Transacao> listTransacoes() throws SQLException {
        List<Transacao> transacoes = new ArrayList<>();
        // ATUALIZADO: Seleciona data_hora_criacao e ordena por ela (DESC - mais recente primeiro)
        String sql = "SELECT id, data, descricao, tipo, valor, data_hora_criacao FROM financeiro ORDER BY data_hora_criacao DESC";
        
        try (Connection conn = Database.getConnection(); // CORRIGIDO
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // ATUALIZADO: Usa o novo construtor para incluir data_hora_criacao
                Transacao t = new Transacao(
                    rs.getInt("id"),
                    rs.getString("descricao"),
                    rs.getDouble("valor"),
                    rs.getString("data"),
                    rs.getString("tipo"),
                    rs.getString("data_hora_criacao") // NOVO
                );
                transacoes.add(t);
            }
        }
        return transacoes;
    }

    /**
     * NOVO: Retorna uma transação específica pela descrição.
     * Usado pelo SafrasController para encontrar a data da transação da colheita.
     */
    public Transacao getTransacaoPorDescricao(String descricao) throws SQLException {
        String sql = "SELECT * FROM financeiro WHERE descricao = ?";
        
        try (Connection conn = Database.getConnection(); // CORRIGIDO
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, descricao);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new Transacao(
                        rs.getInt("id"),
                        rs.getString("descricao"),
                        rs.getDouble("valor"),
                        rs.getString("data"),
                        rs.getString("tipo"),
                        rs.getString("data_hora_criacao") // NOVO
                    );
                }
            }
        }
        return null; // Não encontrado
    }

    /**
     * NOVO: Retorna uma lista de transações onde a descrição começa com o texto fornecido.
     * Usado pelo SafrasController para encontrar todas as vendas de uma colheita.
     */
    public List<Transacao> listTransacoesPorDescricaoLike(String partialDesc) throws SQLException {
        List<Transacao> transacoes = new ArrayList<>();
        String sql = "SELECT * FROM financeiro WHERE descricao LIKE ?";
        
        try (Connection conn = Database.getConnection(); // CORRIGIDO
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            // O '%' é o curinga do SQL para "qualquer coisa"
            pstmt.setString(1, partialDesc + "%"); 
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    transacoes.add(new Transacao(
                        rs.getInt("id"),
                        rs.getString("descricao"),
                        rs.getDouble("valor"),
                        rs.getString("data"),
                        rs.getString("tipo"),
                        rs.getString("data_hora_criacao") // NOVO
                    ));
                }
            }
        }
        return transacoes;
    }


    public double getBalançoFinanceiro() throws SQLException {
        String sql = "SELECT SUM(valor) AS balanco FROM financeiro";
        try (Connection conn = Database.getConnection(); // CORRIGIDO
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble("balanco");
            }
        }
        return 0.0;
    }

    /**
     * NOVO: Retorna os totais de Receita e Despesa.
     * Usado pelo Gráfico de Pizza.
     */
    public Map<String, Double> getTotaisReceitaDespesa() throws SQLException {
        // Usa LinkedHashMap para garantir a ordem
        Map<String, Double> totais = new LinkedHashMap<>();
        
        // Query para somar todas as receitas (valores positivos)
        String sqlReceita = "SELECT SUM(valor) AS total FROM financeiro WHERE tipo = 'receita'";
        try (Connection conn = Database.getConnection(); // CORRIGIDO
             PreparedStatement pstmt = conn.prepareStatement(sqlReceita);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                totais.put("receita", rs.getDouble("total"));
            }
        }
        
        // Query para somar todas as despesas (valores negativos) e pegar o absoluto
        String sqlDespesa = "SELECT ABS(SUM(valor)) AS total FROM financeiro WHERE tipo = 'despesa'";
        try (Connection conn = Database.getConnection(); // CORRIGIDO
             PreparedStatement pstmt = conn.prepareStatement(sqlDespesa);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                totais.put("despesa", rs.getDouble("total"));
            }
        }
        return totais;
    }

    /**
     * NOVO: Retorna um histórico do balanço (soma de transações) agrupado por mês.
     * Usa strftime (função do SQLite) para agrupar por Ano-Mês.
     * Usado pelo Gráfico de Linha.
     */
    public Map<String, Double> getBalancoPorMes() throws SQLException {
        Map<String, Double> balancoMensal = new LinkedHashMap<>(); // LinkedHashMap para manter a ordem
        
        String sql = "SELECT strftime('%Y-%m', data) as mes, SUM(valor) as balanco_mes " +
                     "FROM financeiro " +
                     "GROUP BY mes " +
                     "ORDER BY mes ASC";

        try (Connection conn = Database.getConnection(); // CORRIGIDO
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                balancoMensal.put(rs.getString("mes"), rs.getDouble("balanco_mes"));
            }
        }
        return balancoMensal;
    }
}
