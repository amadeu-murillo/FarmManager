package com.farmmanager.model;

/**
 * ATUALIZADO: Adicionado dataCriacao e dataModificacao.
 */
public class EstoqueItem {
    private int id;
    private String itemNome;
    private double quantidade;
    private String unidade;
    private double valorUnitario; // NOVO
    private double valorTotal; // NOVO
    private String dataCriacao; // NOVO
    private String dataModificacao; // NOVO

    // Construtor para Adicionar (sem ID) - usado principalmente pelo Controller
    public EstoqueItem(String itemNome, double quantidade, String unidade, double valorUnitario, double valorTotal) {
        this.itemNome = itemNome;
        this.quantidade = quantidade;
        this.unidade = unidade;
        this.valorUnitario = valorUnitario;
        this.valorTotal = valorTotal;
    }
    
    // Construtor para Ler do Banco (com ID e datas)
    public EstoqueItem(int id, String itemNome, double quantidade, String unidade, double valorUnitario, double valorTotal, String dataCriacao, String dataModificacao) {
        this(itemNome, quantidade, unidade, valorUnitario, valorTotal);
        this.id = id;
        this.dataCriacao = dataCriacao;
        this.dataModificacao = dataModificacao;
    }

    // Construtor antigo (mantido para compatibilidade onde as datas não são lidas)
    public EstoqueItem(int id, String itemNome, double quantidade, String unidade, double valorUnitario, double valorTotal) {
        this(id, itemNome, quantidade, unidade, valorUnitario, valorTotal, null, null);
    }


    // Getters
    public int getId() { return id; }
    public String getItemNome() { return itemNome; }
    public double getQuantidade() { return quantidade; }
    public String getUnidade() { return unidade; }
    public double getValorUnitario() { return valorUnitario; }
    public double getValorTotal() { return valorTotal; }
    public String getDataCriacao() { return dataCriacao; } // NOVO
    public String getDataModificacao() { return dataModificacao; } // NOVO
}
