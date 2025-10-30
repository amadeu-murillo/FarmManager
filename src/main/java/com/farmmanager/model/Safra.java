package com.farmmanager.model;

/**
 * ATUALIZADO:
 * - anoInicio alterado de int para String (ex: "2025/1").
 * - Adicionado campo 'status'.
 */
public class Safra {
    private int id;
    private String cultura;
    private String anoInicio; // Alterado para String
    private String status; // NOVO
    private int talhaoId;
    private double producaoTotalKg;

    // Construtor atualizado
    public Safra(String cultura, String anoInicio, int talhaoId, String status) {
        this.cultura = cultura;
        this.anoInicio = anoInicio;
        this.talhaoId = talhaoId;
        this.status = status;
        this.producaoTotalKg = 0; // Padrão
    }
    
    // Construtor atualizado
    public Safra(int id, String cultura, String anoInicio, int talhaoId, double producao, String status) {
        this(cultura, anoInicio, talhaoId, status);
        this.id = id;
        this.producaoTotalKg = producao;
    }

    // Getters
    public int getId() { return id; }
    public String getCultura() { return cultura; }
    public String getAnoInicio() { return anoInicio; } // Tipo de retorno alterado
    public String getStatus() { return status; } // NOVO
    public int getTalhaoId() { return talhaoId; }
    public double getProducaoTotalKg() { return producaoTotalKg; }

    // Setters (se necessário)
    public void setStatus(String status) { this.status = status; } // NOVO
}
