package com.farmmanager.model;

// Removido: import java.io.File;
import com.farmmanager.util.AlertUtil; // Certifique-se que AlertUtil está acessível
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
 * - Migrado de SQLite (ficheiro local) para PostgreSQL (servidor).
 * - Sintaxe de AUTOINCREMENT alterada para SERIAL PRIMARY KEY.
 * - Adicionada lógica de conexão com Host, Porta, Utilizador e Senha.
 */
public class Database {

    // --- CONFIGURAÇÃO ANTIGA (REMOVIDA) ---
    // private static final String USER_HOME = System.getProperty("user.home");
    // private static final String DB_PATH = USER_HOME + File.separator + "fazenda.db";
    // private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;


    // --- NOVA CONFIGURAÇÃO CLIENTE-SERVIDOR (PostgreSQL) ---
    
    // !!! IMPORTANTE !!!
    // IP do servidor fornecido pelo utilizador.
    private static final String DB_HOST_IP = "192.168.56.1"; // <-- ATUALIZADO
    
    private static final String DB_PORT = "5432"; // Porta padrão do PostgreSQL
    private static final String DB_NAME = "farmmanager_db"; // O banco que você criou
    private static final String DB_USER = "postgres"; // O superutilizador padrão
    private static final String DB_PASS = "root"; // A senha que você definiu

    // Formato da URL de conexão do PostgreSQL
    private static final String DB_URL = "jdbc:postgresql://" + DB_HOST_IP + ":" + DB_PORT + "/" + DB_NAME;


    /**
     * Retorna uma nova conexão com o banco de dados PostgreSQL.
     * ATUALIZADO: Agora usa utilizador e senha.
     */
    public static Connection getConnection() throws SQLException {
        // Carrega o driver do PostgreSQL (boa prática)
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver PostgreSQL não encontrado. Verifique se o pom.xml está correto.");
            throw new SQLException("Driver PostgreSQL não encontrado.", e);
        }
        // Tenta conectar usando a URL, utilizador e senha
        return DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
    }

    /**
     * Cria todas as tabelas necessárias no banco de dados se elas
     * ainda não existirem.
     * ATUALIZADO: Sintaxe do SQLite 'INTEGER PRIMARY KEY AUTOINCREMENT'
     * foi alterada para a sintaxe do PostgreSQL 'SERIAL PRIMARY KEY'.
     */
    public static void initDb() {
        
        // Sintaxe de 'AUTOINCREMENT' do SQLite foi mudada para 'SERIAL' do PostgreSQL
        String sqlFuncionarios = "CREATE TABLE IF NOT EXISTS funcionarios ("
            + "id SERIAL PRIMARY KEY," // MUDANÇA AQUI
            + "nome TEXT NOT NULL,"
            + "cargo TEXT,"
            + "salario REAL,"
            + "data_inicio TEXT,"
            + "cpf TEXT,"
            + "telefone TEXT,"
            + "endereco TEXT,"
            + "data_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";
            
        String sqlTalhoes = "CREATE TABLE IF NOT EXISTS talhoes ("
            + "id SERIAL PRIMARY KEY," // MUDANÇA AQUI
            + "nome TEXT NOT NULL UNIQUE,"
            + "area_hectares REAL NOT NULL,"
            + "data_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";
            
        // Definição ATUALIZADA da tabela safras
        String sqlSafras = "CREATE TABLE IF NOT EXISTS safras ("
            + "id SERIAL PRIMARY KEY," // MUDANÇA AQUI
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
            + "id SERIAL PRIMARY KEY," // MUDANÇA AQUI
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
            + "id SERIAL PRIMARY KEY," // MUDANÇA AQUI
            + "descricao TEXT NOT NULL,"
            + "valor REAL NOT NULL,"
            + "data TEXT NOT NULL,"
            + "tipo TEXT NOT NULL,"
            + "data_hora_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";

        // NOVO: Definição da tabela de atividades da safra
        String sqlAtividadesSafra = "CREATE TABLE IF NOT EXISTS atividades_safra ("
            + "id SERIAL PRIMARY KEY," // MUDANÇA AQUI
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
            + "id SERIAL PRIMARY KEY," // MUDANÇA AQUI
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
            + "id SERIAL PRIMARY KEY," // MUDANÇA AQUI
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
            + "id SERIAL PRIMARY KEY," // MUDANÇA AQUI
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
            // 1. Cria tabelas se não existirem
            stmt.execute(sqlFuncionarios);
            stmt.execute(sqlTalhoes);
            stmt.execute(sqlSafras);
            stmt.execute(sqlEstoque);
            stmt.execute(sqlFinanceiro);
            stmt.execute(sqlAtividadesSafra);
            stmt.execute(sqlPatrimonio);
            stmt.execute(sqlManutencaoPatrimonio);
            stmt.execute(sqlContas);

            // A lógica de migração (runMigrations) não é mais necessária 
            // para um banco de dados novo e limpo.
            // runMigrations(conn); 

            System.out.println("Banco de dados PostgreSQL inicializado com sucesso.");
        } catch (SQLException e) {
            System.out.println("Erro ao inicializar o banco de dados: " + e.getMessage());
            // Lança um erro mais descritivo
            AlertUtil.showError("Erro de Conexão", 
                "Não foi possível conectar ao banco de dados PostgreSQL em: " + DB_URL + "\n\n" +
                "Verifique:\n" +
                "1. O IP do servidor (" + DB_HOST_IP + ") está correto?\n" +
                "2. O PostgreSQL está rodando no servidor?\n" +
                "3. O Firewall está liberando a porta 5432?\n" +
                "4. O utilizador (" + DB_USER + ") e a senha estão corretos?\n" +
                "5. Os arquivos 'postgresql.conf' e 'pg_hba.conf' estão configurados para acesso de rede?\n\n" +
                "Erro original: " + e.getMessage()
            );
        }
    }

    // Os métodos de migração (runMigrations, columnExists, etc.) podem ser removidos
    // pois eles eram específicos para atualizar um banco SQLite existente.
}