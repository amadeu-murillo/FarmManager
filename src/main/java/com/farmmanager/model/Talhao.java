package com.farmmanager.model;

public class Talhao {
    private int id;
    private String nome;
    private double areaHectares;

    public Talhao(String nome, double areaHectares) {
        this.nome = nome;
        this.areaHectares = areaHectares;
    }

    public Talhao(int id, String nome, double areaHectares) {
        this(nome, areaHectares);
        this.id = id;
    }

    // Getters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public double getAreaHectares() { return areaHectares; }
}
