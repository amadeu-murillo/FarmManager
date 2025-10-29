package com.farmmanager.model;

public class EstoqueItem {
    private int id;
    private String itemNome;
    private double quantidade;
    private String unidade;

    public EstoqueItem(String itemNome, double quantidade, String unidade) {
        this.itemNome = itemNome;
        this.quantidade = quantidade;
        this.unidade = unidade;
    }
    
    public EstoqueItem(int id, String itemNome, double quantidade, String unidade) {
        this(itemNome, quantidade, unidade);
        this.id = id;
    }

    // Getters
    public int getId() { return id; }
    public String getItemNome() { return itemNome; }
    public double getQuantidade() { return quantidade; }
    public String getUnidade() { return unidade; }
}
