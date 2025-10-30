package com.farmmanager.model;

/**
 * NOVO: Classe Modelo (POJO) que representa um Ativo Fixo (Máquina, Trator, etc.)
 */
public class Patrimonio {
    
    private int id;
    private String nome;
    private String tipo;
    private String dataAquisicao; // YYYY-MM-DD
    private double valorAquisicao;
    private String status; // Ex: "Operacional", "Em Manutenção"
    private String dataCriacao;
    private String dataModificacao;

    // Construtor para criar (sem ID)
    public Patrimonio(String nome, String tipo, String dataAquisicao, double valorAquisicao, String status) {
        this.nome = nome;
        this.tipo = tipo;
        this.dataAquisicao = dataAquisicao;
        this.valorAquisicao = valorAquisicao;
        this.status = status;
    }

    // Construtor para ler (com ID)
    public Patrimonio(int id, String nome, String tipo, String dataAquisicao, double valorAquisicao, String status, String dataCriacao, String dataModificacao) {
        this(nome, tipo, dataAquisicao, valorAquisicao, status);
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataModificacao = dataModificacao;
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getTipo() { return tipo; }
    public String getDataAquisicao() { return dataAquisicao; }
    public double getValorAquisicao() { return valorAquisicao; }
    public String getStatus() { return status; }
    public String getDataCriacao() { return dataCriacao; }
    public String getDataModificacao() { return dataModificacao; }
}
