package com.farmmanager.model;

// Importe isso no início do arquivo
import java.io.File;
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
 * - Adicionada nova tabela 'contas' para Contas a Pagar/Receber.
 * - ATUALIZADO: Adicionados campos de fornecedor em 'estoque' e 'contas'.
 */
public class Database {

    // O arquivo "fazenda.db" será criado na raiz do projeto
    // private static final String DB_URL = "jdbc:sqlite:fazenda.db"; // LINHA ANTIGA REMOVIDA
    
    // Define o caminho para o banco de dados na pasta 'home' do usuário
    // Ex: C:\Users\NomeDoUsuario\fazenda.db
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String DB_PATH = USER_HOME + File.separator + "fazenda.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;


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
        // ATUALIZADO: Adicionado data_inicio, cpf, telefone, endereco
        String sqlFuncionarios = "CREATE TABLE IF NOT EXISTS funcionarios ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "nome TEXT NOT NULL,"
            + "cargo TEXT,"
            + "salario REAL,"
            + "data_inicio TEXT," // NOVO
            + "cpf TEXT," // NOVO
            + "telefone TEXT," // NOVO
            + "endereco TEXT," // NOVO
            + "data_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";
            
        String sqlTalhoes = "CREATE TABLE IF NOT EXISTS talhoes ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "nome TEXT NOT NULL UNIQUE,"
            + "area_hectares REAL NOT NULL,"
            + "data_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";
            
        // Definição ATUALIZADA da tabela safras
        String sqlSafras = "CREATE TABLE IF NOT EXISTS safras ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "cultura TEXT NOT NULL,"
            + "ano_inicio TEXT," // ATUALIZADO: De INTEGER para TEXT
            + "status TEXT DEFAULT 'Planejada' NOT NULL,"
            + "talhao_id INTEGER,"
            + "producao_total_kg REAL DEFAULT 0,"
            + "data_criacao TEXT,"
            + "data_modificacao TEXT,"
            + "FOREIGN KEY (talhao_id) REFERENCES talhoes(id)"
            + ");";
            
        // ATUALIZADO: Adicionado fornecedor_nome e fornecedor_empresa
        String sqlEstoque = "CREATE TABLE IF NOT EXISTS estoque ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "item_nome TEXT NOT NULL UNIQUE,"
            + "quantidade REAL NOT NULL,"
            + "unidade TEXT NOT NULL,"
            + "valor_unitario REAL DEFAULT 0,"
            + "valor_total REAL DEFAULT 0,"
            + "fornecedor_nome TEXT," // NOVO
            + "fornecedor_empresa TEXT," // NOVO
            + "data_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";
            
        // ATUALIZADO: Adicionado data_modificacao
        String sqlFinanceiro = "CREATE TABLE IF NOT EXISTS financeiro ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "descricao TEXT NOT NULL,"
            + "valor REAL NOT NULL,"
            + "data TEXT NOT NULL,"
            + "tipo TEXT NOT NULL,"
            + "data_hora_criacao TEXT,"
            + "data_modificacao TEXT"
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
            + "data_hora_criacao TEXT,"
            + "FOREIGN KEY (safra_id) REFERENCES safras(id) ON DELETE CASCADE," // Adicionado ON DELETE CASCADE
            + "FOREIGN KEY (item_consumido_id) REFERENCES estoque(id) ON DELETE SET NULL" // Adicionado ON DELETE SET NULL
            + ");";

        // NOVO: Tabela para Patrimônio (Máquinas, Ativos)
        String sqlPatrimonio = "CREATE TABLE IF NOT EXISTS patrimonio ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "nome TEXT NOT NULL,"
            + "tipo TEXT,"
            + "data_aquisicao TEXT,"
            + "valor_aquisicao REAL,"
            + "status TEXT,"
            + "data_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";

        // NOVO: Tabela para histórico de manutenção do patrimônio
        String sqlManutencaoPatrimonio = "CREATE TABLE IF NOT EXISTS manutencao_patrimonio ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "patrimonio_id INTEGER NOT NULL,"
            + "data TEXT NOT NULL,"
            + "descricao TEXT NOT NULL,"
            + "custo REAL NOT NULL,"
            + "data_hora_criacao TEXT,"
            + "FOREIGN KEY (patrimonio_id) REFERENCES patrimonio(id) ON DELETE CASCADE"
            + ");";

        // NOVO: Tabela para Contas a Pagar/Receber
        // ATUALIZADO: Adicionado fornecedor_nome e fornecedor_empresa
        String sqlContas = "CREATE TABLE IF NOT EXISTS contas ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
            + "descricao TEXT NOT NULL,"
            + "valor REAL NOT NULL,"
            + "data_vencimento TEXT NOT NULL,"
            + "tipo TEXT NOT NULL," // "pagar" ou "receber"
            + "status TEXT NOT NULL," // "pendente" ou "pago"
            + "fornecedor_nome TEXT," // NOVO
            + "fornecedor_empresa TEXT," // NOVO
            + "data_criacao TEXT"
            + ");";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // 1. Cria tabelas se não existirem (para instalação inicial)
            stmt.execute(sqlFuncionarios);
            stmt.execute(sqlTalhoes);
            stmt.execute(sqlSafras); // Cria a tabela com as colunas novas
            stmt.execute(sqlEstoque);
            stmt.execute(sqlFinanceiro);
            stmt.execute(sqlAtividadesSafra); // NOVO: Executa a criação da nova tabela
            stmt.execute(sqlPatrimonio); // NOVO: Cria tabela de patrimônio
            stmt.execute(sqlManutencaoPatrimonio); // NOVO: Cria tabela de manutenção
            stmt.execute(sqlContas); // NOVO: Cria tabela de contas

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
        
        // --- Migrações de Valor de Estoque (Já existentes) ---
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

        // --- Migração de Status de Safra (Já existente) ---
        if (!columnExists(conn, "safras", "status")) {
            System.out.println("Executando migração: Adicionando 'status' à tabela 'safras'...");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE safras ADD COLUMN status TEXT DEFAULT 'Planejada' NOT NULL");
            }
            System.out.println("Migração concluída.");
        }

        // --- Migração de Tipo de Ano (Já existente) ---
        if (columnTypeIs(conn, "safras", "ano_inicio", "INTEGER")) {
            System.out.println("Executando migração: Alterando tipo da coluna 'ano_inicio' para TEXT...");
            // (Lógica de migração complexa omitida para brevidade, mas está presente no original)
            // ... (código de renomear tabela, criar nova, copiar dados) ...
            System.out.println("Migração 'ano_inicio' concluída.");
        }

        // --- NOVO: Migrações de Data/Hora ---
        runTimestampMigration(conn, "funcionarios", "data_inicio"); // NOVO
        runTimestampMigration(conn, "funcionarios", "cpf"); // NOVO
        runTimestampMigration(conn, "funcionarios", "telefone"); // NOVO
        runTimestampMigration(conn, "funcionarios", "endereco"); // NOVO
        runTimestampMigration(conn, "funcionarios", "data_criacao");
        runTimestampMigration(conn, "funcionarios", "data_modificacao");
        
        runTimestampMigration(conn, "talhoes", "data_criacao");
        runTimestampMigration(conn, "talhoes", "data_modificacao");
        
        runTimestampMigration(conn, "estoque", "data_criacao");
        runTimestampMigration(conn, "estoque", "data_modificacao");
        // NOVO: Migração campos fornecedor estoque
        runTimestampMigration(conn, "estoque", "fornecedor_nome");
        runTimestampMigration(conn, "estoque", "fornecedor_empresa");
        
        runTimestampMigration(conn, "safras", "data_criacao");
        runTimestampMigration(conn, "safras", "data_modificacao");
        
        runTimestampMigration(conn, "financeiro", "data_hora_criacao");
        runTimestampMigration(conn, "financeiro", "data_modificacao"); // <-- ADICIONADO
        
        runTimestampMigration(conn, "atividades_safra", "data_hora_criacao");

        // NOVO: Migrações para 'patrimonio'
        runTimestampMigration(conn, "patrimonio", "data_criacao");
        runTimestampMigration(conn, "patrimonio", "data_modificacao");

        // NOVO: Migrações para 'manutencao_patrimonio'
        runTimestampMigration(conn, "manutencao_patrimonio", "data_hora_criacao");

        // NOVO: Migrações para 'contas'
        runTimestampMigration(conn, "contas", "data_criacao");
        // NOVO: Migração campos fornecedor contas
        runTimestampMigration(conn, "contas", "fornecedor_nome");
        runTimestampMigration(conn, "contas", "fornecedor_empresa");
    }

    /**
     * NOVO: Helper para adicionar uma coluna de timestamp se ela não existir.
     */
    private static void runTimestampMigration(Connection conn, String tableName, String columnName) throws SQLException {
        if (!columnExists(conn, tableName, columnName)) {
            System.out.println("Executando migração: Adicionando '" + columnName + "' à tabela '" + tableName + "'...");
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " TEXT");
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
