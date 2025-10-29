package com.farmmanager.controller;

import com.farmmanager.model.Funcionario;
import com.farmmanager.model.FuncionarioDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Pair;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Controller para o FuncionariosView.fxml.
 * ATUALIZADO: Substituído o fluxo de múltiplos TextInputDialogs por um único Dialog customizado
 * e implementada a remoção real.
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
        // 1. Criar o diálogo customizado
        Dialog<Funcionario> dialog = new Dialog<>();
        dialog.setTitle("Adicionar Novo Funcionário");
        dialog.setHeaderText("Preencha os dados do novo funcionário.");

        // 2. Definir os botões
        ButtonType adicionarButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        // 3. Criar o layout (GridPane)
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomeField = new TextField();
        nomeField.setPromptText("Nome");
        TextField cargoField = new TextField();
        cargoField.setPromptText("Cargo");
        TextField salarioField = new TextField();
        salarioField.setPromptText("Salário (ex: 2500.50)");

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Cargo:"), 0, 1);
        grid.add(cargoField, 1, 1);
        grid.add(new Label("Salário (R$):"), 0, 2);
        grid.add(salarioField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        // 4. Converter o resultado para um objeto Funcionario
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    String cargo = cargoField.getText();
                    double salario = Double.parseDouble(salarioField.getText());
                    
                    if (nome.isEmpty() || cargo.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "Nome e Cargo são obrigatórios.");
                        return null; // Retorna nulo para não fechar o diálogo
                    }
                    
                    return new Funcionario(nome, cargo, salario);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor do salário inválido.");
                    return null; // Retorna nulo para não fechar o diálogo
                }
            }
            return null;
        });

        // 5. Exibir o diálogo e processar o resultado
        Optional<Funcionario> result = dialog.showAndWait();

        result.ifPresent(funcionario -> {
            try {
                funcionarioDAO.addFuncionario(funcionario);
                carregarDadosDaTabela(); // Atualiza a tabela
                AlertUtil.showInfo("Sucesso", "Funcionário adicionado com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar o funcionário: " + e.getMessage());
            }
        });
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
                // ATUALIZADO: Chama o DAO real para remover
                if (funcionarioDAO.removerFuncionario(selecionado.getId())) {
                    AlertUtil.showInfo("Removido", "Funcionário removido com sucesso.");
                    carregarDadosDaTabela(); // Recarrega os dados do banco
                } else {
                    AlertUtil.showError("Erro ao Remover", "O funcionário não pôde ser removido.");
                }
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover o funcionário: " + e.getMessage());
            } catch (Exception e) {
                AlertUtil.showError("Erro ao Remover", "Não foi possível remover o funcionário.");
            }
        }
    }
}
