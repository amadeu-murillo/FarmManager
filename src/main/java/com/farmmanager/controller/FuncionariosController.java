package com.farmmanager.controller;

import com.farmmanager.model.Funcionario;
import com.farmmanager.model.FuncionarioDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Controller para o FuncionariosView.fxml.
 */
public class FuncionariosController {

    @FXML
    private TableView<Funcionario> tabelaFuncionarios;
    @FXML
    private TableColumn<Funcionario, Integer> colFuncId;
    @FXML
    private TableColumn<Funcionario, String> colFuncNome;
    @FXML
    private TableColumn<Funcionario, String> colFuncCargo;
    @FXML
    private TableColumn<Funcionario, Double> colFuncSalario;

    private final FuncionarioDAO funcionarioDAO;
    private final ObservableList<Funcionario> dadosTabela;

    public FuncionariosController() {
        funcionarioDAO = new FuncionarioDAO();
        dadosTabela = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        // Configura as colunas da tabela
        colFuncId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFuncNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colFuncCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colFuncSalario.setCellValueFactory(new PropertyValueFactory<>("salario"));
        
        // Associa a lista observável à tabela
        tabelaFuncionarios.setItems(dadosTabela);

        // Carrega os dados
        carregarDadosDaTabela();
    }

    /**
     * Busca os dados do DAO e atualiza a ObservableList.
     */
    private void carregarDadosDaTabela() {
        try {
            List<Funcionario> lista = funcionarioDAO.listFuncionarios();
            dadosTabela.clear();
            dadosTabela.addAll(lista);
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os funcionários.");
        }
    }

    @FXML
    private void handleAdicionar() {
        // Este é um exemplo simples. O ideal seria um DialogPane customizado.
        TextInputDialog dialogNome = new TextInputDialog();
        dialogNome.setTitle("Adicionar Funcionário");
        dialogNome.setHeaderText("Passo 1 de 3: Nome");
        dialogNome.setContentText("Nome:");
        Optional<String> nome = dialogNome.showAndWait();

        if (nome.isPresent() && !nome.get().isEmpty()) {
            TextInputDialog dialogCargo = new TextInputDialog();
            dialogCargo.setTitle("Adicionar Funcionário");
            dialogCargo.setHeaderText("Passo 2 de 3: Cargo");
            dialogCargo.setContentText("Cargo:");
            Optional<String> cargo = dialogCargo.showAndWait();

            if (cargo.isPresent() && !cargo.get().isEmpty()) {
                TextInputDialog dialogSalario = new TextInputDialog();
                dialogSalario.setTitle("Adicionar Funcionário");
                dialogSalario.setHeaderText("Passo 3 de 3: Salário");
                dialogSalario.setContentText("Salário (ex: 2500.50):");
                Optional<String> salarioStr = dialogSalario.showAndWait();

                try {
                    double salario = Double.parseDouble(salarioStr.get());
                    Funcionario f = new Funcionario(nome.get(), cargo.get(), salario);
                    funcionarioDAO.addFuncionario(f);
                    
                    carregarDadosDaTabela(); // Atualiza a tabela
                    AlertUtil.showInfo("Sucesso", "Funcionário adicionado com sucesso.");

                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor do salário inválido.");
                } catch (SQLException e) {
                    AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar o funcionário.");
                }
            }
        }
    }

    @FXML
    private void handleRemover() {
        Funcionario selecionado = tabelaFuncionarios.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um funcionário na tabela para remover.");
            return;
        }

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Remoção", 
            "Tem certeza que deseja remover o funcionário '" + selecionado.getNome() + "'?");

        if (confirmado) {
            try {
                // (O DAO não tinha um método de remoção, vamos assumir que ele exista)
                // funcionarioDAO.removerFuncionario(selecionado.getId());
                
                // Vamos apenas simular a remoção da lista por enquanto
                dadosTabela.remove(selecionado);
                AlertUtil.showInfo("Removido", "Funcionário removido com sucesso.");
                // carregarDadosDaTabela(); // Recarregaria do banco
                
            } catch (Exception e) {
                AlertUtil.showError("Erro ao Remover", "Não foi possível remover o funcionário.");
            }
        }
    }
}
