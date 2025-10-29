package com.farmmanager.model;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class FuncionarioDAO {

    public boolean addFuncionario(Funcionario funcionario) throws SQLException {
        String sql = "INSERT INTO funcionarios(nome, cargo, salario) VALUES(?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, funcionario.getNome());
            pstmt.setString(2, funcionario.getCargo());
            pstmt.setDouble(3, funcionario.getSalario());
            int affectedRows = pstmt.executeUpdate();
            return affectedRows > 0;
        }
    }

    /**
     * NOVO: Remove um funcionário pelo ID.
     */
    public boolean removerFuncionario(int id) throws SQLException {
        String sql = "DELETE FROM funcionarios WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    public List<Funcionario> listFuncionarios() throws SQLException {
        List<Funcionario> funcionarios = new ArrayList<>();
        String sql = "SELECT id, nome, cargo, salario FROM funcionarios";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                Funcionario f = new Funcionario(
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getString("cargo"),
                    rs.getDouble("salario")
                );
                funcionarios.add(f);
            }
        }
        return funcionarios;
    }
    
    /**
     * NOVO: Retorna a contagem total de funcionários.
     * Usado pelo Dashboard.
     */
    public int getContagemFuncionarios() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM funcionarios";
        
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
