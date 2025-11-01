package com.farmmanager.model;

public class Transacao {
    private int id;
    private String descricao;
    private double valor; // Positivo para receita, negativo para despesa
    private String data; // YYYY-MM-DD
    private String tipo; // "receita" ou "despesa"
    private String dataHoraCriacao; // NOVO CAMPO
    // private String dataModificacao; // Adicionado implicitamente pela DAO

    public Transacao(String descricao, double valor, String data, String tipo) {
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.tipo = tipo;
    }
    
    // Construtor antigo (mantido para compatibilidade)
    public Transacao(int id, String descricao, double valor, String data, String tipo) {
        this(descricao, valor, data, tipo);
        this.id = id;
        this.dataHoraCriacao = null; // Define como nulo se não for fornecido
    }

    // NOVO CONSTRUTOR para ler do banco (com dataHoraCriacao)
    public Transacao(int id, String descricao, double valor, String data, String tipo, String dataHoraCriacao) {
        this(descricao, valor, data, tipo);
        this.id = id;
        this.dataHoraCriacao = dataHoraCriacao;
    }

    // Getters
    public int getId() { return id; }
    public String getDescricao() { return descricao; }
    public double getValor() { return valor; }
    public String getData() { return data; }
    public String getTipo() { return tipo; }
    public String getDataHoraCriacao() { return dataHoraCriacao; } // NOVO GETTER
}
