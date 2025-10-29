package com.farmmanager.model;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Classe utilitária para gerenciar a conexão com o banco de dados
 * e a inicialização das tabelas.
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
     * ainda não existirem.
     */
    public static void initDb() {
        // CORREÇÃO: Removido Text Block (aspas triplas) para compatibilidade com Java 11
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
            
        String sqlEstoque = "CREATE TABLE IF NOT EXISTS estoque ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "item_nome TEXT NOT NULL UNIQUE,"
            + "quantidade REAL NOT NULL,"
            + "unidade TEXT NOT NULL"
            + ");";
            
        String sqlFinanceiro = "CREATE TABLE IF NOT EXISTS financeiro ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "descricao TEXT NOT NULL,"
            + "valor REAL NOT NULL,"
            + "data TEXT NOT NULL,"
            + "tipo TEXT NOT NULL"
            + ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            stmt.execute(sqlFuncionarios);
            stmt.execute(sqlTalhoes);
            stmt.execute(sqlSafras);
            stmt.execute(sqlEstoque);
            stmt.execute(sqlFinanceiro);
            System.out.println("Banco de dados inicializado com sucesso.");
        } catch (SQLException e) {
            System.out.println("Erro ao inicializar o banco de dados: " + e.getMessage());
        }
    }
}

