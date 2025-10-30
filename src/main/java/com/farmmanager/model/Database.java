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
 * ATUALIZADO:
 * - Adicionada lógica de migração para
 * 1. Adicionar colunas de valor ao estoque.
 * 2. Adicionar coluna 'status' à tabela 'safras'.
 * 3. Alterar tipo da coluna 'ano_inicio' da tabela 'safras' para TEXT.
 * - Adicionada nova tabela 'atividades_safra' para rastreamento de custos.
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
            
        // Definição ATUALIZADA da tabela safras
        String sqlSafras = "CREATE TABLE IF NOT EXISTS safras ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "cultura TEXT NOT NULL,"
            + "ano_inicio TEXT," // ATUALIZADO: De INTEGER para TEXT
            + "status TEXT DEFAULT 'Planejada' NOT NULL," // NOVO
            + "talhao_id INTEGER,"
            + "producao_total_kg REAL DEFAULT 0,"
            + "FOREIGN KEY (talhao_id) REFERENCES talhoes(id)"
            + ");";
            
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

        // NOVO: Definição da tabela de atividades da safra
        String sqlAtividadesSafra = "CREATE TABLE IF NOT EXISTS atividades_safra ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "safra_id INTEGER NOT NULL,"
            + "descricao TEXT NOT NULL,"
            + "data TEXT NOT NULL,"
            + "item_consumido_id INTEGER,"
            + "quantidade_consumida REAL,"
            + "custo_total_atividade REAL NOT NULL,"
            + "FOREIGN KEY (safra_id) REFERENCES safras(id) ON DELETE CASCADE," // Adicionado ON DELETE CASCADE
            + "FOREIGN KEY (item_consumido_id) REFERENCES estoque(id) ON DELETE SET NULL" // Adicionado ON DELETE SET NULL
            + ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Cria tabelas se não existirem (para instalação inicial)
            stmt.execute(sqlFuncionarios);
            stmt.execute(sqlTalhoes);
            stmt.execute(sqlSafras); // Cria a tabela com as colunas novas
            stmt.execute(sqlEstoque);
            stmt.execute(sqlFinanceiro);
            stmt.execute(sqlAtividadesSafra); // NOVO: Executa a criação da nova tabela

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

        // Migração 2: Adicionar coluna status em safras
        if (!columnExists(conn, "safras", "status")) {
            System.out.println("Executando migração: Adicionando 'status' à tabela 'safras'...");
            try (Statement stmt = conn.createStatement()) {
                // Adiciona com default 'Planejada' e NOT NULL
                stmt.execute("ALTER TABLE safras ADD COLUMN status TEXT DEFAULT 'Planejada' NOT NULL");
            }
            System.out.println("Migração concluída.");
        }

        // Migração 3: Alterar tipo da coluna ano_inicio de INTEGER para TEXT
        if (columnTypeIs(conn, "safras", "ano_inicio", "INTEGER")) {
            System.out.println("Executando migração: Alterando tipo da coluna 'ano_inicio' para TEXT...");
            try (Statement stmt = conn.createStatement()) {
                // 1. Desabilitar chaves estrangeiras (importante para SQLite)
                stmt.execute("PRAGMA foreign_keys=OFF;");
                
                // 2. Iniciar transação
                conn.setAutoCommit(false);
                
                // 3. Renomear tabela antiga
                stmt.execute("ALTER TABLE safras RENAME TO safras_old;");
                
                // 4. Criar tabela nova com o schema correto (copiado de initDb)
                stmt.execute("CREATE TABLE safras ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "cultura TEXT NOT NULL,"
                    + "ano_inicio TEXT," // O TIPO CORRETO
                    + "status TEXT DEFAULT 'Planejada' NOT NULL," // A COLUNA NOVA
                    + "talhao_id INTEGER,"
                    + "producao_total_kg REAL DEFAULT 0,"
                    + "FOREIGN KEY (talhao_id) REFERENCES talhoes(id)"
                    + ");");

                // 5. Copiar dados, convertendo a coluna
                // Atualiza o status de safras antigas (com produção) para 'Colhida'
                stmt.execute("INSERT INTO safras (id, cultura, ano_inicio, status, talhao_id, producao_total_kg) "
                    + "SELECT id, cultura, CAST(ano_inicio AS TEXT), "
                    + "CASE WHEN producao_total_kg > 0 THEN 'Colhida' ELSE 'Planejada' END, "
                    + "talhao_id, producao_total_kg "
                    + "FROM safras_old;");
                    
                // 6. Remover tabela antiga
                stmt.execute("DROP TABLE safras_old;");
                
                // 7. Commit
                conn.commit();
                
                // 8. Reabilitar chaves estrangeiras
                stmt.execute("PRAGMA foreign_keys=ON;");
            } catch (SQLException e) {
                conn.rollback(); // Desfaz em caso de erro na migração
                System.out.println("Erro na migração de 'safras': " + e.getMessage());
                throw e;
            } finally {
                conn.setAutoCommit(true); // Restaura auto-commit
            }
            System.out.println("Migração 'ano_inicio' concluída.");
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

    /**
     * NOVO: Verifica o tipo de uma coluna específica.
     */
    private static boolean columnTypeIs(Connection conn, String tableName, String columnName, String expectedType) throws SQLException {
        String sql = "PRAGMA table_info(" + tableName + ")";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                if (rs.getString("name").equalsIgnoreCase(columnName)) {
                    return rs.getString("type").equalsIgnoreCase(expectedType);
                }
            }
        }
        return false;
    }
}

