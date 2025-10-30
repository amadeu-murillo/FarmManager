package com.farmmanager.model;

public class EstoqueItem {
    private int id;
    private String itemNome;
    private double quantidade;
    private String unidade;
    private double valorUnitario; // NOVO
    private double valorTotal; // NOVO

    // Construtor para Adicionar (sem ID)
    public EstoqueItem(String itemNome, double quantidade, String unidade, double valorUnitario, double valorTotal) {
        this.itemNome = itemNome;
        this.quantidade = quantidade;
        this.unidade = unidade;
        this.valorUnitario = valorUnitario;
        this.valorTotal = valorTotal;
    }
    
    // Construtor para Ler do Banco (com ID)
    public EstoqueItem(int id, String itemNome, double quantidade, String unidade, double valorUnitario, double valorTotal) {
        this(itemNome, quantidade, unidade, valorUnitario, valorTotal);
        this.id = id;
    }

    // Getters
    public int getId() { return id; }
    public String getItemNome() { return itemNome; }
    public double getQuantidade() { return quantidade; }
    public String getUnidade() { return unidade; }
    public double getValorUnitario() { return valorUnitario; }
    public double getValorTotal() { return valorTotal; }
}
