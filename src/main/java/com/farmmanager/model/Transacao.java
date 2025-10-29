package com.farmmanager.model;

public class Transacao {
    private int id;
    private String descricao;
    private double valor; // Positivo para receita, negativo para despesa
    private String data; // YYYY-MM-DD
    private String tipo; // "receita" ou "despesa"

    public Transacao(String descricao, double valor, String data, String tipo) {
        this.descricao = descricao;
        this.valor = valor;
        this.data = data;
        this.tipo = tipo;
    }
    
    public Transacao(int id, String descricao, double valor, String data, String tipo) {
        this(descricao, valor, data, tipo);
        this.id = id;
    }

    // Getters
    public int getId() { return id; }
    public String getDescricao() { return descricao; }
    public double getValor() { return valor; }
    public String getData() { return data; }
    public String getTipo() { return tipo; }
}
