package com.farmmanager.model;

/**
 * NOVO: Classe Modelo (POJO) que representa um lançamento futuro
 * (Conta a Pagar ou Conta a Receber).
 * ATUALIZADO: Adicionados setters para permitir a edição.
 */
public class Conta {
    private int id;
    private String descricao;
    private double valor; // Sempre positivo. O 'tipo' define se é despesa ou receita.
    private String dataVencimento; // YYYY-MM-DD
    private String tipo; // "pagar" ou "receber"
    private String status; // "pendente" ou "pago"
    private String dataCriacao;
    
    // Construtor para criar (sem ID)
    public Conta(String descricao, double valor, String dataVencimento, String tipo, String status) {
        this.descricao = descricao;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.tipo = tipo;
        this.status = status;
    }

    // Construtor para ler (com ID)
    public Conta(int id, String descricao, double valor, String dataVencimento, String tipo, String status, String dataCriacao) {
        this(descricao, valor, dataVencimento, tipo, status);
        this.id = id;
        this.dataCriacao = dataCriacao;
    }

    // Getters
    public int getId() { return id; }
    public String getDescricao() { return descricao; }
    public double getValor() { return valor; }
    public String getDataVencimento() { return dataVencimento; }
    public String getTipo() { return tipo; }
    public String getStatus() { return status; }
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
}

