package com.farmmanager.model;

/**
 * NOVO: Classe Modelo (POJO) que representa uma atividade ou custo
 * lançado para uma safra específica.
 */
public class AtividadeSafra {
    
    private int id;
    private int safraId;
    private String descricao;
    private String data; // YYYY-MM-DD
    private Integer itemConsumidoId; // Pode ser nulo se for um custo manual (ex: "Mão de obra")
    private double quantidadeConsumida;
    private double custoTotalAtividade;

    // Construtor para criar (sem ID)
    public AtividadeSafra(int safraId, String descricao, String data, Integer itemConsumidoId, double quantidadeConsumida, double custoTotalAtividade) {
        this.safraId = safraId;
        this.descricao = descricao;
        this.data = data;
        this.itemConsumidoId = itemConsumidoId;
        this.quantidadeConsumida = quantidadeConsumida;
        this.custoTotalAtividade = custoTotalAtividade;
    }

    // Construtor para ler (com ID)
    public AtividadeSafra(int id, int safraId, String descricao, String data, Integer itemConsumidoId, double quantidadeConsumida, double custoTotalAtividade) {
        this(safraId, descricao, data, itemConsumidoId, quantidadeConsumida, custoTotalAtividade);
        this.id = id;
    }

    // Getters
    public int getId() { return id; }
    public int getSafraId() { return safraId; }
    public String getDescricao() { return descricao; }
    public String getData() { return data; }
    public Integer getItemConsumidoId() { return itemConsumidoId; }
    public double getQuantidadeConsumida() { return quantidadeConsumida; }
    public double getCustoTotalAtividade() { return custoTotalAtividade; }
}

