package com.farmmanager.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

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
}
