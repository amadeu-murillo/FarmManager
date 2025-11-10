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
 * NOVO: DAO para gerenciar a tabela 'contas' (Contas a Pagar/Receber).
 * ATUALIZADO: Adicionado updateConta.
 * ATUALIZADO: Adicionados métodos para alertas de dashboard (Vencidas, A Vencer).
 * ATUALIZADO: Adicionados campos de fornecedor.
 * ATUALIZADO: Adicionado listContasPorDescricaoLike para o balanço de safras.
 * ATUALIZADO: Revertido SQL de data para sintaxe do SQLite.
 */
public class ContaDAO {

    /**
     * Adiciona uma nova conta (pagar/receber).
     */
    public boolean addConta(Conta conta) throws SQLException {
        String sql = "INSERT INTO contas(descricao, valor, data_vencimento, tipo, status, fornecedor_nome, fornecedor_empresa, data_criacao) VALUES(?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, conta.getDescricao());
            pstmt.setDouble(2, conta.getValor()); // Valor é sempre positivo
            pstmt.setString(3, conta.getDataVencimento());
            pstmt.setString(4, conta.getTipo());
            pstmt.setString(5, conta.getStatus());
            pstmt.setString(6, conta.getFornecedorNome()); // NOVO
            pstmt.setString(7, conta.getFornecedorEmpresa()); // NOVO
            pstmt.setString(8, DateTimeUtil.getCurrentTimestamp());
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * Remove uma conta.
     */
    public boolean removerConta(int id) throws SQLException {
        String sql = "DELETE FROM contas WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * NOVO: Atualiza os dados de uma conta pendente.
     */
    public boolean updateConta(Conta conta) throws SQLException {
        String sql = "UPDATE contas SET descricao = ?, valor = ?, data_vencimento = ?, tipo = ?, "
                   + "fornecedor_nome = ?, fornecedor_empresa = ? "
                   + "WHERE id = ? AND status = 'pendente'";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, conta.getDescricao());
            pstmt.setDouble(2, conta.getValor());
            pstmt.setString(3, conta.getDataVencimento());
            pstmt.setString(4, conta.getTipo());
            pstmt.setString(5, conta.getFornecedorNome()); // NOVO
            pstmt.setString(6, conta.getFornecedorEmpresa()); // NOVO
            pstmt.setInt(7, conta.getId());
            
            return pstmt.executeUpdate() > 0;
        }
    }

    /**
     * "Liquida" uma conta:
     * 1. Atualiza o status da conta para "pago".
     * 2. Cria uma transação correspondente no 'financeiro'.
     */
    public boolean liquidarConta(int id, String dataPagamento) throws SQLException {
        // 1. Buscar a conta
        Conta conta = getContaById(id);
        if (conta == null || conta.getStatus().equals("pago")) {
            throw new SQLException("Conta não encontrada ou já foi paga.");
        }

        // 2. Definir a transação
        double valorTransacao = conta.getTipo().equals("pagar") ? -conta.getValor() : conta.getValor();
        String tipoTransacao = conta.getTipo().equals("pagar") ? "despesa" : "receita";
        
        // Adiciona info do fornecedor na descrição do financeiro
        String descTransacao = "Liquidação: " + conta.getDescricao();
        if (conta.getFornecedorNome() != null && !conta.getFornecedorNome().isEmpty()) {
            descTransacao += " (Fornec: " + conta.getFornecedorNome() + ")";
        }

        Transacao transacao = new Transacao(descTransacao, valorTransacao, dataPagamento, tipoTransacao);
        
        String sqlUpdateConta = "UPDATE contas SET status = 'pago' WHERE id = ?";
        
        Connection conn = null;
        try {
            conn = Database.getConnection();
            conn.setAutoCommit(false); // Inicia transação

            // 3. Adiciona no financeiro
            FinanceiroDAO financeiroDAO = new FinanceiroDAO();
            // Como o FinanceiroDAO não está preparado para isso, vamos chamá-lo fora da transação
            // (idealmente, o addTransacao receberia a conexão)
            
            // ATUALIZAÇÃO: Para simplificar, vamos fazer as duas operações separadas
            // Se o primeiro falhar, o segundo não executa.
            
            // 1. Adiciona no financeiro
            financeiroDAO.addTransacao(transacao);
            
            // 2. Atualiza o status da conta
            try (PreparedStatement pstmtUpdate = conn.prepareStatement(sqlUpdateConta)) {
                pstmtUpdate.setInt(1, id);
                pstmtUpdate.executeUpdate();
            }

            conn.commit(); // Efetiva (apenas o update, neste caso)
            return true;
            
        } catch (SQLException e) {
            if (conn != null) {
                conn.rollback();
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Busca uma conta pelo ID.
     */
    public Conta getContaById(int id) throws SQLException {
        String sql = "SELECT * FROM contas WHERE id = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, id);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToConta(rs);
                }
            }
        }
        return null;
    }

    /**
     * Lista as contas com base no status.
     * @param statusFiltro "pendente", "pago", ou "todos"
     */
    public List<Conta> listContas(String statusFiltro) throws SQLException {
        List<Conta> contas = new ArrayList<>();
        String sql;
        
        switch (statusFiltro.toLowerCase()) {
            case "pendente":
                sql = "SELECT * FROM contas WHERE status = 'pendente' ORDER BY data_vencimento ASC";
                break;
            case "pago":
                sql = "SELECT * FROM contas WHERE status = 'pago' ORDER BY data_vencimento DESC";
                break;
            default: // "todos"
                sql = "SELECT * FROM contas ORDER BY data_vencimento ASC";
                break;
        }

        try (Connection conn = Database.getConnection();
             Statement stmt = conn.createStatement(); 
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                contas.add(mapRowToConta(rs));
            }
        }
        return contas;
    }

    /**
     * NOVO: Retorna uma lista de contas (pagar/receber) onde a descrição começa com o texto.
     * Usado pelo SafrasController para encontrar vendas a prazo.
     */
    public List<Conta> listContasPorDescricaoLike(String partialDesc) throws SQLException {
        List<Conta> contas = new ArrayList<>();
        // Busca por contas que COMECEM com o prefixo
        String sql = "SELECT * FROM contas WHERE descricao LIKE ? ORDER BY data_vencimento ASC";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, partialDesc + "%"); 
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    contas.add(mapRowToConta(rs)); // Reusa o helper existente
                }
            }
        }
        return contas;
    }

    /**
     * Calcula o total pendente (a pagar ou a receber).
     */
    public double getTotalPendente(String tipo) throws SQLException {
        String sql = "SELECT SUM(valor) AS total FROM contas WHERE status = 'pendente' AND tipo = ?";
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, tipo); // "pagar" ou "receber"
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getDouble("total");
                }
            }
        }
        return 0.0;
    }

    /**
     * NOVO: Retorna a contagem de contas pendentes VENCIDAS.
     * @return A contagem de contas.
     * @throws SQLException
     */
    public int getContagemContasVencidas() throws SQLException {
        // SQL do SQLite para comparar datas. date('now') é hoje.
        String sql = "SELECT COUNT(*) AS total FROM contas WHERE status = 'pendente' " +
                     "AND data_vencimento < date('now')"; // REVERTIDO PARA SQLITE
        
        // NOVO: SQL alterado para PostgreSQL (AGORA COMENTADO)
        // String sql = "SELECT COUNT(*) AS total FROM contas WHERE status = 'pendente' " +
        //              "AND data_vencimento::date < CURRENT_DATE";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }


    /**
     * NOVO: Retorna a contagem de contas pendentes a vencer em um determinado
     * número de dias.
     * @param dias O número de dias a partir de hoje (ex: 7 para a próxima semana).
     * @return A contagem de contas.
     * @throws SQLException
     */
    public int getContagemContasAVencer(int dias) throws SQLException {
        // SQL do SQLite para comparar datas. date('now') é hoje.
        String sql = "SELECT COUNT(*) AS total FROM contas WHERE status = 'pendente' " +
                     "AND data_vencimento >= date('now') " +
                     "AND data_vencimento <= date('now', '+' || ? || ' days')"; // REVERTIDO PARA SQLITE
        
        // NOVO: SQL alterado para PostgreSQL (AGORA COMENTADO)
        // String sql = "SELECT COUNT(*) AS total FROM contas WHERE status = 'pendente' " +
        //              "AND data_vencimento::date >= CURRENT_DATE " +
        //              "AND data_vencimento::date <= (CURRENT_DATE + ? * INTERVAL '1 day')";
        
        try (Connection conn = Database.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, String.valueOf(dias)); // REVERTIDO PARA SQLITE
            // pstmt.setInt(1, dias); // NOVO: PostgreSQL espera um INT para o '?' no INTERVAL
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        }
        return 0;
    }

    // Helper para mapear o ResultSet
    private Conta mapRowToConta(ResultSet rs) throws SQLException {
        return new Conta(
            rs.getInt("id"),
            rs.getString("descricao"),
            rs.getDouble("valor"),
            rs.getString("data_vencimento"),
            rs.getString("tipo"),
            rs.getString("status"),
            rs.getString("fornecedor_nome"), // NOVO
            rs.getString("fornecedor_empresa"), // NOVO
            rs.getString("data_criacao")
        );
    }
}