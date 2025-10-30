package com.farmmanager.model;

/**
 * NOVO: Classe Modelo (POJO) que representa um registro de manutenção
 * para um item do patrimônio.
 */
public class Manutencao {
    
    private int id;
    private int patrimonioId;
    private String data; // YYYY-MM-DD
    private String descricao;
    private double custo;

    // Construtor para criar (sem ID)
    public Manutencao(int patrimonioId, String data, String descricao, double custo) {
        this.patrimonioId = patrimonioId;
        this.data = data;
        this.descricao = descricao;
        this.custo = custo;
    }

    // Construtor para ler (com ID)
    public Manutencao(int id, int patrimonioId, String data, String descricao, double custo) {
        this(patrimonioId, data, descricao, custo);
        this.id = id;
    }

    // Getters
    public int getId() { return id; }
    public int getPatrimonioId() { return patrimonioId; }
    public String getData() { return data; }
    public String getDescricao() { return descricao; }
    public double getCusto() { return custo; }
}
