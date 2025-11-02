package com.farmmanager.model;

/**
 * NOVO: Classe Modelo (POJO) que representa um lançamento futuro
 * (Conta a Pagar ou Conta a Receber).
 * ATUALIZADO: Adicionados setters para permitir a edição.
 * ATUALIZADO: Adicionado fornecedorNome e fornecedorEmpresa.
 */
public class Conta {
    private int id;
    private String descricao;
    private double valor; // Sempre positivo. O 'tipo' define se é despesa ou receita.
    private String dataVencimento; // YYYY-MM-DD
    private String tipo; // "pagar" ou "receber"
    private String status; // "pendente" ou "pago"
    private String fornecedorNome; // NOVO
    private String fornecedorEmpresa; // NOVO
    private String dataCriacao;
    
    // Construtor para criar (sem ID)
    public Conta(String descricao, double valor, String dataVencimento, String tipo, String status, String fornecedorNome, String fornecedorEmpresa) {
        this.descricao = descricao;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.tipo = tipo;
        this.status = status;
        this.fornecedorNome = fornecedorNome; // NOVO
        this.fornecedorEmpresa = fornecedorEmpresa; // NOVO
    }

    // Construtor para ler (com ID)
    public Conta(int id, String descricao, double valor, String dataVencimento, String tipo, String status, String fornecedorNome, String fornecedorEmpresa, String dataCriacao) {
        this(descricao, valor, dataVencimento, tipo, status, fornecedorNome, fornecedorEmpresa);
        this.id = id;
        this.dataCriacao = dataCriacao;
    }
    
    // Construtor antigo (sem fornecedor) - mantido para compatibilidade
    public Conta(int id, String descricao, double valor, String dataVencimento, String tipo, String status, String dataCriacao) {
        this(id, descricao, valor, dataVencimento, tipo, status, null, null, dataCriacao);
    }
    
    // Construtor antigo (sem fornecedor e sem ID) - mantido para compatibilidade
     public Conta(String descricao, double valor, String dataVencimento, String tipo, String status) {
         this(descricao, valor, dataVencimento, tipo, status, null, null);
     }


    // Getters
    public int getId() { return id; }
    public String getDescricao() { return descricao; }
    public double getValor() { return valor; }
    public String getDataVencimento() { return dataVencimento; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
    public String getFornecedorNome() { return fornecedorNome; } // NOVO
    public String getFornecedorEmpresa() { return fornecedorEmpresa; } // NOVO
    public String getDataCriacao() { return dataCriacao; }

    // NOVO: Setters para edição
    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }
    public void setValor(double valor) {
        this.valor = valor;
    }
    public void setDataVencimento(String dataVencimento) {
        this.dataVencimento = dataVencimento;
    }
    public void setTipo(String tipo) {
        this.tipo = tipo;
    }
    public void setFornecedorNome(String fornecedorNome) { // NOVO
        this.fornecedorNome = fornecedorNome;
    }
    public void setFornecedorEmpresa(String fornecedorEmpresa) { // NOVO
        this.fornecedorEmpresa = fornecedorEmpresa;
    }
}
