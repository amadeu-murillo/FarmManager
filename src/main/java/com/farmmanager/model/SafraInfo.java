package com.farmmanager.model;

/**
 * DTO (Data Transfer Object) para carregar a lista de safras
 * com o nome do talhão (resultado do JOIN).
 */
public class SafraInfo {
    // Constante para cálculo de produtividade
    private static final double KG_POR_SACO = 60.0;

    private final int id;
    private final String cultura;
    private final int anoInicio;
    private final double producaoTotalKg;
    private final String talhaoNome;
    private final double areaHectares; // NOVO: Campo para armazenar a área

    public SafraInfo(int id, String cultura, int ano, String talhaoNome, double producao, double areaHectares) {
        this.id = id;
        this.cultura = cultura;
        this.anoInicio = ano;
        this.talhaoNome = talhaoNome;
        this.producaoTotalKg = producao;
        this.areaHectares = areaHectares; // NOVO
    }

    // Getters
    public int getId() { return id; }
    public String getCultura() { return cultura; }
    public int getAnoInicio() { return anoInicio; }
    public double getProducaoTotalKg() { return producaoTotalKg; }
    public String getTalhaoNome() { return talhaoNome; }
    public double getAreaHectares() { return areaHectares; } // NOVO

    /**
     * NOVO: Calcula a produção em sacos por hectare.
     * Este método será usado pela TableView.
     * @return Produção em sc/ha.
     */
    public double getProducaoSacosPorHectare() {
        if (areaHectares == 0 || producaoTotalKg == 0) {
            return 0.0;
        }
        double totalSacos = producaoTotalKg / KG_POR_SACO;
        return totalSacos / areaHectares;
    }
}
