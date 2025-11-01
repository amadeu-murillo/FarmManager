package com.farmmanager.model;

/**
 * Classe Modelo (POJO) que representa um Funcionário.
 * ATUALIZADO: Adicionado dataInicio, cpf, telefone, endereco.
 */
public class Funcionario {
    private int id;
    private String nome;
    private String cargo;
    private double salario;
    private String dataInicio;
    private String cpf; // NOVO
    private String telefone; // NOVO
    private String endereco; // NOVO

    // Construtor para criar um novo funcionário (sem ID ainda) - Usado pelo Controller
    public Funcionario(String nome, String cargo, double salario, String dataInicio, String cpf, String telefone, String endereco) {
        this.nome = nome;
        this.cargo = cargo;
        this.salario = salario;
        this.dataInicio = dataInicio;
        this.cpf = cpf; // NOVO
        this.telefone = telefone; // NOVO
        this.endereco = endereco; // NOVO
    }
    
    // Construtor para ler um funcionário do banco (com ID) - Usado pelo DAO
    public Funcionario(int id, String nome, String cargo, double salario, String dataInicio, String cpf, String telefone, String endereco) {
        this(nome, cargo, salario, dataInicio, cpf, telefone, endereco); // Chama o construtor acima
        this.id = id;
    }

    // Getters e Setters
    public int getId() { return id; }
    public String getNome() { return nome; }
    public String getCargo() { return cargo; }
    public double getSalario() { return salario; }
    public String getDataInicio() { return dataInicio; }
    public String getCpf() { return cpf; } // NOVO
    public String getTelefone() { return telefone; } // NOVO
    public String getEndereco() { return endereco; } // NOVO

    public void setId(int id) { this.id = id; }
    public void setNome(String nome) { this.nome = nome; }
    public void setCargo(String cargo) { this.cargo = cargo; }
    public void setSalario(double salario) { this.salario = salario; }
    public void setDataInicio(String dataInicio) { this.dataInicio = dataInicio; }
    public void setCpf(String cpf) { this.cpf = cpf; } // NOVO
    public void setTelefone(String telefone) { this.telefone = telefone; } // NOVO
    public void setEndereco(String endereco) { this.endereco = endereco; } // NOVO
}

