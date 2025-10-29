package com.farmmanager.model;

/**
 * Classe Modelo (POJO) que representa um Funcionário.
 */
public class Funcionario {
    private int id;
    private String nome;
    private String cargo;
    private double salario;

    // Construtor para criar um novo funcionário (sem ID ainda)
    public Funcionario(String nome, String cargo, double salario) {
        this.nome = nome;
        this.cargo = cargo;
        this.salario = salario;
    }
    
    // Construtor para ler um funcionário do banco (com ID)
    public Funcionario(int id, String nome, String cargo, double salario) {
        this(nome, cargo, salario);
        this.id = id;
    }

    // Getters e Setters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getCargo() { return cargo; }
    public double getSalario() { return salario; }

    public void setId(int id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public void setSalario(double salario) { this.salario = salario; }
}
