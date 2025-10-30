package com.farmmanager.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe utilitária para gerenciar a conexão com o banco de dados
 * e a inicialização das tabelas.
 *
 * ATUALIZADO: Adicionada lógica de migração para atualizar tabelas existentes.
 */
public class Database {

    // O arquivo "fazenda.db" será criado na raiz do projeto
    private static final String DB_URL = "jdbc:sqlite:fazenda.db";

    /**
     * Retorna uma nova conexão com o banco de dados SQLite.
     */
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Cria todas as tabelas necessárias no banco de dados se elas
     * ainda não existirem, e executa migrações em bancos existentes.
     */
    public static void initDb() {
        String sqlFuncionarios = "CREATE TABLE IF NOT EXISTS funcionarios ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "nome TEXT NOT NULL,"
            + "cargo TEXT,"
            + "salario REAL"
            + ");";
            
        String sqlTalhoes = "CREATE TABLE IF NOT EXISTS talhoes ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "nome TEXT NOT NULL UNIQUE,"
            + "area_hectares REAL NOT NULL"
            + ");";
            
        String sqlSafras = "CREATE TABLE IF NOT EXISTS safras ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "cultura TEXT NOT NULL,"
            + "ano_inicio INTEGER,"
            + "talhao_id INTEGER,"
            + "producao_total_kg REAL DEFAULT 0,"
            + "FOREIGN KEY (talhao_id) REFERENCES talhoes(id)"
            + ");";
            
        // Esta é a definição ATUALIZADA (para novas instalações)
        String sqlEstoque = "CREATE TABLE IF NOT EXISTS estoque ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "item_nome TEXT NOT NULL UNIQUE,"
            + "quantidade REAL NOT NULL,"
            + "unidade TEXT NOT NULL,"
            + "valor_unitario REAL DEFAULT 0,"
            + "valor_total REAL DEFAULT 0"
            + ");";
            
        String sqlFinanceiro = "CREATE TABLE IF NOT EXISTS financeiro ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "descricao TEXT NOT NULL,"
            + "valor REAL NOT NULL,"
            + "data TEXT NOT NULL,"
            + "tipo TEXT NOT NULL"
            + ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Cria tabelas se não existirem (para instalação inicial)
            stmt.execute(sqlFuncionarios);
            stmt.execute(sqlTalhoes);
            stmt.execute(sqlSafras);
            stmt.execute(sqlEstoque); // Cria a tabela com as colunas novas
            stmt.execute(sqlFinanceiro);

            // 2. NOVO: Executa migrações para bancos de dados antigos
            runMigrations(conn);

            System.out.println("Banco de dados inicializado com sucesso.");
        } catch (SQLException e) {
            System.out.println("Erro ao inicializar o banco de dados: " + e.getMessage());
        }
    }

    /**
     * NOVO: Executa migrações no esquema do banco de dados.
     * Isso garante que usuários com versões antigas do banco (fazenda.db)
     * recebam as novas colunas sem perder dados.
     */
    private static void runMigrations(Connection conn) throws SQLException {
        // Migração 1: Adicionar colunas de valor ao estoque
        if (!columnExists(conn, "estoque", "valor_unitario")) {
            System.out.println("Executando migração: Adicionando 'valor_unitario' à tabela 'estoque'...");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE estoque ADD COLUMN valor_unitario REAL DEFAULT 0");
            }
            System.out.println("Migração concluída.");
        }

        if (!columnExists(conn, "estoque", "valor_total")) {
            System.out.println("Executando migração: Adicionando 'valor_total' à tabela 'estoque'...");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE estoque ADD COLUMN valor_total REAL DEFAULT 0");
            }
            System.out.println("Migração concluída.");
        }
    }

    /**
     * NOVO: Verifica se uma coluna específica existe em uma tabela usando PRAGMA table_info (SQLite).
     */
    private static boolean columnExists(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                // A coluna "name" no resultado do PRAGMA contém o nome da coluna
                if (rs.getString("name").equalsIgnoreCase(columnName)) {
                    return true;
                }
            }
        }
        return false;
    }
}

