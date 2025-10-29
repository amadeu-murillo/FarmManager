package com.farmmanager.model;

public class Safra {
    private int id;
    private String cultura;
    private int anoInicio;
    private int talhaoId;
    private double producaoTotalKg;

    public Safra(String cultura, int anoInicio, int talhaoId) {
        this.cultura = cultura;
        this.anoInicio = anoInicio;
        this.talhaoId = talhaoId;
        this.producaoTotalKg = 0; // Padrão
    }
    
    public Safra(int id, String cultura, int anoInicio, int talhaoId, double producao) {
        this(cultura, anoInicio, talhaoId);
        this.id = id;
        this.producaoTotalKg = producao;
    }

    // Getters
    public int getId() { return id; }
    public String getCultura() { return cultura; }
    public int getAnoInicio() { return anoInicio; }
    public int getTalhaoId() { return talhaoId; }
    public double getProducaoTotalKg() { return producaoTotalKg; }
}
