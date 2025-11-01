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
 * ATUALIZADO:
 * - addFuncionario e listFuncionarios agora incluem 'data_inicio', 'cpf', 'telefone', 'endereco'.
 */
public class FuncionarioDAO {

    public boolean addFuncionario(Funcionario funcionario) throws SQLException {
        // NOVO: SQL atualizado com data_inicio e novos campos
        String sql = "INSERT INTO funcionarios(nome, cargo, salario, data_inicio, cpf, telefone, endereco, data_criacao, data_modificacao) VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            String now = DateTimeUtil.getCurrentTimestamp();

            pstmt.setString(1, funcionario.getNome());
            pstmt.setString(2, funcionario.getCargo());
            pstmt.setDouble(3, funcionario.getSalario());
            pstmt.setString(4, funcionario.getDataInicio());
            pstmt.setString(5, funcionario.getCpf()); // NOVO
            pstmt.setString(6, funcionario.getTelefone()); // NOVO
            pstmt.setString(7, funcionario.getEndereco()); // NOVO
            pstmt.setString(8, now); // Índice atualizado
            pstmt.setString(9, now); // Índice atualizado
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
        // ATUALIZADO: Seleciona novos campos
        String sql = "SELECT id, nome, cargo, salario, data_inicio, cpf, telefone, endereco FROM funcionarios";
        
        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // ATUALIZADO: Usa o novo construtor
                Funcionario f = new Funcionario(
                    rs.getInt("id"),
                    rs.getString("nome"),
                    rs.getString("cargo"),
                    rs.getDouble("salario"),
                    rs.getString("data_inicio"),
                    rs.getString("cpf"), // NOVO
                    rs.getString("telefone"), // NOVO
                    rs.getString("endereco") // NOVO
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

