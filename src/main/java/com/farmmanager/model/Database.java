package com.farmmanager.model;

import java.io.File; // RE-ADICIONADO
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
 * - Revertido de PostgreSQL (servidor) para SQLite (ficheiro local).
 * - Sintaxe de 'SERIAL PRIMARY KEY' alterada de volta para 'INTEGER PRIMARY KEY AUTOINCREMENT'.
 */
public class Database {

    // --- CONFIGURAÇÃO ANTIGA (RE-ATIVADA) ---
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String DB_PATH = USER_HOME + File.separator + "fazenda.db";
    private static final String DB_URL = "jdbc:sqlite:" + DB_PATH;


    // --- NOVA CONFIGURAÇÃO CLIENTE-SERVIDOR (PostgreSQL) - DESATIVADA ---
    
    // private static final String DB_HOST_IP = "192.168.56.1"; 
    // private static final String DB_PORT = "5432"; 
    // private static final String DB_NAME = "farmmanager_db"; 
    // private static final String DB_USER = "postgres"; 
    // private static final String DB_PASS = "root"; 

    // private static final String DB_URL = "jdbc:postgresql://" + DB_HOST_IP + ":" + DB_PORT + "/" + DB_NAME;


    /**
     * Retorna uma nova conexão com o banco de dados SQLite.
     * ATUALIZADO: Agora usa a URL do SQLite sem utilizador/senha.
     */
    public static Connection getConnection() throws SQLException {
        // Carrega o driver do SQLite (boa prática)
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite não encontrado. Verifique se o pom.xml está correto.");
            throw new SQLException("Driver SQLite não encontrado.", e);
        }
        // Tenta conectar usando a URL do SQLite
        return DriverManager.getConnection(DB_URL);
    }

    /**
     * Cria todas as tabelas necessárias no banco de dados se elas
     * ainda não existirem.
     * ATUALIZADO: Sintaxe do PostgreSQL 'SERIAL PRIMARY KEY'
     * foi alterada de volta para a sintaxe do SQLite 'INTEGER PRIMARY KEY AUTOINCREMENT'.
     */
    public static void initDb() {
        
        // Sintaxe de 'SERIAL' do PostgreSQL foi mudada de volta para 'INTEGER PRIMARY KEY AUTOINCREMENT'
        String sqlFuncionarios = "CREATE TABLE IF NOT EXISTS funcionarios ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT," // MUDANÇA AQUI
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
            + "id INTEGER PRIMARY KEY AUTOINCREMENT," // MUDANÇA AQUI
            + "nome TEXT NOT NULL UNIQUE,"
            + "area_hectares REAL NOT NULL,"
            + "data_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";
            
        // Definição ATUALIZADA da tabela safras
        String sqlSafras = "CREATE TABLE IF NOT EXISTS safras ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT," // MUDANÇA AQUI
            + "cultura TEXT NOT NULL,"
            + "ano_inicio TEXT," 
            + "status TEXT DEFAULT 'Planejada' NOT NULL,"
            + "talhao_id INTEGER,"
            + "producao_total_kg REAL DEFAULT 0,"
            + "data_criacao TEXT,"
            + "data_modificacao TEXT,"
            + "FOREIGN KEY (talhao_id) REFERENCES talhoes(id)"
            + ");";
            
        // ATUALIZADO: Adicionado fornecedor_nome e fornecedor_empresa
        String sqlEstoque = "CREATE TABLE IF NOT EXISTS estoque ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT," // MUDANÇA AQUI
            + "item_nome TEXT NOT NULL UNIQUE,"
            + "quantidade REAL NOT NULL,"
            + "unidade TEXT NOT NULL,"
            + "valor_unitario REAL DEFAULT 0,"
            + "valor_total REAL DEFAULT 0,"
            + "fornecedor_nome TEXT," 
            + "fornecedor_empresa TEXT," 
            + "data_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";
            
        // ATUALIZADO: Adicionado data_modificacao
        String sqlFinanceiro = "CREATE TABLE IF NOT EXISTS financeiro ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT," // MUDANÇA AQUI
            + "descricao TEXT NOT NULL,"
            + "valor REAL NOT NULL,"
            + "data TEXT NOT NULL,"
            + "tipo TEXT NOT NULL,"
            + "data_hora_criacao TEXT,"
            + "data_modificacao TEXT"
            + ");";

        // NOVO: Definição da tabela de atividades da safra
        String sqlAtividadesSafra = "CREATE TABLE IF NOT EXISTS atividades_safra ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT," // MUDANÇA AQUI
            + "safra_id INTEGER NOT NULL,"
            + "descricao TEXT NOT NULL,"
            + "data TEXT NOT NULL,"
            + "item_consumido_id INTEGER,"
            + "quantidade_consumida REAL,"
            + "custo_total_atividade REAL NOT NULL,"
            + "data_hora_criacao TEXT,"
            + "FOREIGN KEY (safra_id) REFERENCES safras(id) ON DELETE CASCADE," 
            + "FOREIGN KEY (item_consumido_id) REFERENCES estoque(id) ON DELETE SET NULL"
            + ");";

        // NOVO: Tabela para Patrimônio (Máquinas, Ativos)
        String sqlPatrimonio = "CREATE TABLE IF NOT EXISTS patrimonio ("
            + "id INTEGER PRIMARY KEY AUTOINCREMENT," // MUDANÇA AQUI
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
            + "id INTEGER PRIMARY KEY AUTOINCREMENT," // MUDANÇA AQUI
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
            + "id INTEGER PRIMARY KEY AUTOINCREMENT," // MUDANÇA AQUI
            + "descricao TEXT NOT NULL,"
            + "valor REAL NOT NULL,"
            + "data_vencimento TEXT NOT NULL,"
            + "tipo TEXT NOT NULL," 
            + "status TEXT NOT NULL," 
            + "fornecedor_nome TEXT," 
            + "fornecedor_empresa TEXT," 
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

            System.out.println("Banco de dados SQLite inicializado com sucesso em: " + DB_PATH);
        } catch (SQLException e) {
            System.out.println("Erro ao inicializar o banco de dados: " + e.getMessage());
            // Lança um erro mais descritivo
            AlertUtil.showError("Erro de Conexão", 
                "Não foi possível criar ou conectar ao banco de dados SQLite em: " + DB_PATH + "\n\n" +
                "Verifique as permissões de escrita na pasta do usuário.\n\n" +
                "Erro original: " + e.getMessage()
            );
        }
    }
}