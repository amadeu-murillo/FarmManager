package com.farmmanager.model;

/**
 * DTO (Data Transfer Object) para carregar a lista de safras
 * com o nome do talhão (resultado do JOIN).
 */
public class SafraInfo {
    private final int id;
    private final String cultura;
    private final int anoInicio;
    private final double producaoTotalKg;
    private final String talhaoNome;

    public SafraInfo(int id, String cultura, int ano, String talhaoNome, double producao) {
        this.id = id;
        this.cultura = cultura;
        this.anoInicio = ano;
        this.talhaoNome = talhaoNome;
        this.producaoTotalKg = producao;
    }

    // Getters
    public int getId() { return id; }
    public String getCultura() { return cultura; }
    public int getAnoInicio() { return anoInicio; }
    public double getProducaoTotalKg() { return producaoTotalKg; }
    public String getTalhaoNome() { return talhaoNome; }
}
