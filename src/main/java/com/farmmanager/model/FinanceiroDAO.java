package com.farmmanager.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap; // NOVO
import java.util.List;
import java.util.Map; // NOVO

public class FinanceiroDAO {

    public boolean addTransacao(Transacao transacao) throws SQLException {
        String sql = "INSERT INTO financeiro(descricao, valor, data, tipo) VALUES(?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, transacao.getDescricao());
            pstmt.setDouble(2, transacao.getValor());
            pstmt.setString(3, transacao.getData());
            pstmt.setString(4, transacao.getTipo());
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Transacao> listTransacoes() throws SQLException {
        List<Transacao> transacoes = new ArrayList<>();
        String sql = "SELECT id, data, descricao, tipo, valor FROM financeiro ORDER BY data DESC";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Transacao t = new Transacao(
                    rs.getInt("id"),
                    rs.getString("descricao"),
                    rs.getDouble("valor"),
                    rs.getString("data"),
                    rs.getString("tipo")
                );
                transacoes.add(t);
            }
        }
        return transacoes;
    }

    public double getBalançoFinanceiro() throws SQLException {
        String sql = "SELECT SUM(valor) AS balanco FROM financeiro";
        try (Connection conn = Database.getConnection();
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
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sqlReceita);
             ResultSet rs = pstmt.executeQuery()) {
            if (rs.next()) {
                totais.put("receita", rs.getDouble("total"));
            }
        }
        
        // Query para somar todas as despesas (valores negativos) e pegar o absoluto
        String sqlDespesa = "SELECT ABS(SUM(valor)) AS total FROM financeiro WHERE tipo = 'despesa'";
        try (Connection conn = Database.getConnection();
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

        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {

            while (rs.next()) {
                balancoMensal.put(rs.getString("mes"), rs.getDouble("balanco_mes"));
            }
        }
        return balancoMensal;
    }
}

