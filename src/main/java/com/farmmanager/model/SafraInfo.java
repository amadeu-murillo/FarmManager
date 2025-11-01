package com.farmmanager.model;

/**
 * DTO (Data Transfer Object) para carregar a lista de safras
 * com o nome do talhão (resultado do JOIN).
 * * ATUALIZAÇÃO:
 * - anoInicio alterado de int para String.
 * - Adicionado campo 'status'.
 * - Adicionado campo 'dataModificacao'.
 */
public class SafraInfo {
    // Constante para cálculo de produtividade
    private static final double KG_POR_SACO = 60.0;

    private final int id;
    private final String cultura;
    private final String anoInicio; // Alterado para String
    private final double producaoTotalKg;
    private final String talhaoNome;
    private final double areaHectares;
    private final String status; // NOVO
    private final String dataModificacao; // NOVO

    public SafraInfo(int id, String cultura, String ano, String talhaoNome, double producao, double areaHectares, String status, String dataModificacao) {
        this.id = id;
        this.cultura = cultura;
        this.anoInicio = ano; // Alterado
        this.talhaoNome = talhaoNome;
        this.producaoTotalKg = producao;
        this.areaHectares = areaHectares;
        this.status = status; // NOVO
        this.dataModificacao = dataModificacao; // NOVO
    }

    // Getters
    public int getId() { return id; }
    public String getCultura() { return cultura; }
    public String getAnoInicio() { return anoInicio; } // Tipo de retorno alterado
    public double getProducaoTotalKg() { return producaoTotalKg; }
    public String getTalhaoNome() { return talhaoNome; }
    public double getAreaHectares() { return areaHectares; }
    public String getStatus() { return status; } // NOVO
    
    /**
     * NOVO: Retorna a data da última modificação.
     * Para safras colhidas, esta será a data da colheita.
     */
    public String getDataModificacao() { return dataModificacao; }

    /**
     * Calcula a produção em sacos por hectare.
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

    /**
     * Calcula a produção total em sacos.
     * Usado pela nova coluna da tabela.
     * @return Produção total em sacos.
     */
    public double getProducaoTotalSacos() {
        if (producaoTotalKg == 0) {
            return 0.0;
        }
        return producaoTotalKg / KG_POR_SACO;
    }
}

