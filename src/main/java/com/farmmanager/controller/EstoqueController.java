package com.farmmanager.controller;

import com.farmmanager.model.EstoqueItem;
import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.util.Optional;
import java.util.Locale; 

/**
 * Controller para o EstoqueView.fxml.
 * ATUALIZADO: Implementado cálculo automático de valor unitário/total.
 */
public class EstoqueController {

    @FXML
    private TableView<EstoqueItem> tabelaEstoque;
    @FXML
    private TableColumn<EstoqueItem, Integer> colItemId;
    @FXML
    private TableColumn<EstoqueItem, String> colItemNome;
    @FXML
    private TableColumn<EstoqueItem, Double> colItemQtd;
    @FXML
    private TableColumn<EstoqueItem, String> colItemUnidade;
    @FXML
    private TableColumn<EstoqueItem, Double> colItemValorUnit; // NOVO
    @FXML
    private TableColumn<EstoqueItem, Double> colItemValorTotal; // NOVO

    private final EstoqueDAO estoqueDAO;
    private final ObservableList<EstoqueItem> dadosTabela;

    // Flag para evitar loops nos listeners
    private boolean isUpdating = false;

    public EstoqueController() {
        estoqueDAO = new EstoqueDAO();
        dadosTabela = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        colItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemNome.setCellValueFactory(new PropertyValueFactory<>("itemNome"));
        colItemQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colItemUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));
        colItemValorUnit.setCellValueFactory(new PropertyValueFactory<>("valorUnitario")); // NOVO
        colItemValorTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal")); // NOVO

        tabelaEstoque.setItems(dadosTabela);
        carregarDadosDaTabela();
    }

    private void carregarDadosDaTabela() {
        try {
            dadosTabela.clear();
            dadosTabela.addAll(estoqueDAO.listEstoque());
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar o estoque.");
        }
    }

    /**
     * Manipulador para o botão "+ Adicionar Item".
     * ATUALIZADO: Com cálculo automático de valor.
     */
    @FXML
    private void handleAdicionarItem() {
        Dialog<EstoqueItem> dialog = new Dialog<>();
        dialog.setTitle("Adicionar Item ao Estoque");
        dialog.setHeaderText("Preencha os dados do item.\nSe o item já existir, os valores serão somados (custo médio).");

        ButtonType adicionarButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomeField = new TextField();
        nomeField.setPromptText("Ex: Semente de Milho");
        TextField qtdField = new TextField();
        qtdField.setPromptText("Ex: 50.0");
        TextField unidadeField = new TextField();
        unidadeField.setPromptText("Ex: Sacos ou Kg");
        
        TextField valorUnitarioField = new TextField();
        valorUnitarioField.setPromptText("Ex: 150.00");
        TextField valorTotalField = new TextField();
        valorTotalField.setPromptText("Ex: 7500.00");


        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Quantidade:"), 0, 1);
        grid.add(qtdField, 1, 1);
        grid.add(new Label("Unidade:"), 0, 2);
        grid.add(unidadeField, 1, 2);
        grid.add(new Label("Valor Unitário (R$):"), 0, 3);
        grid.add(valorUnitarioField, 1, 3);
        grid.add(new Label("Valor Total (R$):"), 0, 4);
        grid.add(valorTotalField, 1, 4);

        // --- Lógica de Cálculo Automático ---

        // Se Qtd ou Vlr. Unitário mudam, calcula o Vlr. Total
        qtdField.textProperty().addListener((obs, oldV, newV) -> calcularTotal(qtdField, valorUnitarioField, valorTotalField));
        valorUnitarioField.textProperty().addListener((obs, oldV, newV) -> calcularTotal(qtdField, valorUnitarioField, valorTotalField));

        // Se Vlr. Total muda, calcula o Vlr. Unitário
        valorTotalField.textProperty().addListener((obs, oldV, newV) -> calcularUnitario(qtdField, valorUnitarioField, valorTotalField));
        
        // --- Fim da Lógica ---

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    String unidade = unidadeField.getText();
                    double qtd = parseDouble(qtdField.getText());
                    double valorUnitario = parseDouble(valorUnitarioField.getText());
                    double valorTotal = parseDouble(valorTotalField.getText());

                    if (nome.isEmpty() || unidade.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "Nome e Unidade são obrigatórios.");
                        return null;
                    }
                    if (qtd <= 0 || valorUnitario <= 0 || valorTotal <= 0) {
                         AlertUtil.showError("Erro de Validação", "Quantidade e valores devem ser positivos.");
                         return null;
                    }
                    
                    return new EstoqueItem(nome, qtd, unidade, valorUnitario, valorTotal);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valores de quantidade ou R$ inválidos.");
                    return null;
                }
            }
            return null;
        });

        Optional<EstoqueItem> result = dialog.showAndWait();

        result.ifPresent(item -> {
            try {
                // O DAO agora espera o objeto Item
                estoqueDAO.addEstoque(item);
                carregarDadosDaTabela();
                AlertUtil.showInfo("Sucesso", "Estoque atualizado com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar o estoque: " + e.getMessage());
            }
        });
    }

    // Helper para converter texto em double (aceita , e .)
    private double parseDouble(String text) throws NumberFormatException {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(text.replace(",", "."));
    }

    // Helper para formatar double para o campo de texto
    private String formatDouble(double value) {
        // Usa Locale.US para garantir o ponto como separador decimal
        return String.format(Locale.US, "%.2f", value);
    }

    // Calcula Vlr. Total
    private void calcularTotal(TextField qtdField, TextField valorUnitarioField, TextField valorTotalField) {
        if (isUpdating) return; // Evita loop
        isUpdating = true;
        try {
            double qtd = parseDouble(qtdField.getText());
            double valorUnitario = parseDouble(valorUnitarioField.getText());
            if (qtd > 0 && valorUnitario > 0) {
                valorTotalField.setText(formatDouble(qtd * valorUnitario));
            }
        } catch (NumberFormatException e) {
            // ignora
        }
        isUpdating = false;
    }

    // Calcula Vlr. Unitário
    private void calcularUnitario(TextField qtdField, TextField valorUnitarioField, TextField valorTotalField) {
        if (isUpdating) return; // Evita loop
        isUpdating = true;
        try {
            double qtd = parseDouble(qtdField.getText());
            double valorTotal = parseDouble(valorTotalField.getText());
            if (qtd > 0 && valorTotal > 0) {
                valorUnitarioField.setText(formatDouble(valorTotal / qtd));
            }
        } catch (NumberFormatException e) {
            // ignora
        }
        isUpdating = false;
    }


    /**
     * Manipulador para o botão "Consumir Item".
     */
    @FXML
    private void handleConsumirItem() {
        EstoqueItem selecionado = tabelaEstoque.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um item na tabela para consumir.");
            return;
        }

        // Formata a quantidade atual para exibir no diálogo
        String qtdAtualStr = String.format(Locale.US, "%.2f", selecionado.getQuantidade());

        TextInputDialog dialog = new TextInputDialog("1.0");
        dialog.setTitle("Consumir Item do Estoque");
        dialog.setHeaderText("Item: " + selecionado.getItemNome() + " (Disponível: " + qtdAtualStr + " " + selecionado.getUnidade() + ")");
        dialog.setContentText("Digite a quantidade a consumir:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(qtdConsumirStr -> {
            try {
                // Aceita vírgula ou ponto
                double qtdAConsumir = Double.parseDouble(qtdConsumirStr.replace(",", "."));

                if (qtdAConsumir <= 0) {
                    AlertUtil.showError("Valor Inválido", "A quantidade a consumir deve ser positiva.");
                    return;
                }

                // Chama o novo método do DAO
                estoqueDAO.consumirEstoque(selecionado.getId(), qtdAConsumir);
                
                AlertUtil.showInfo("Sucesso", "Estoque atualizado com sucesso.");
                carregarDadosDaTabela(); // Recarrega a tabela

            } catch (NumberFormatException e) {
                AlertUtil.showError("Erro de Formato", "Valor de quantidade inválido. Use apenas números (ex: 10.5).");
            } catch (IllegalStateException e) {
                // Captura a exceção de estoque insuficiente lançada pelo DAO
                AlertUtil.showError("Erro de Estoque", e.getMessage());
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível consumir o item: " + e.getMessage());
            }
        });
    }


    /**
     * Manipulador para o botão "- Remover Item".
     */
    @FXML
    private void handleRemoverItem() {
        EstoqueItem selecionado = tabelaEstoque.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um item na tabela para remover.");
            return;
        }

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Remoção", 
            "Tem certeza que deseja remover o item '" + selecionado.getItemNome() + "' do estoque?\nEsta ação é permanente.");

        if (confirmado) {
            try {
                if (estoqueDAO.removerItemEstoque(selecionado.getId())) {
                    AlertUtil.showInfo("Removido", "Item removido do estoque com sucesso.");
                    carregarDadosDaTabela();
                } else {
                    AlertUtil.showError("Erro ao Remover", "O item não pôde ser removido.");
                }
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover o item: " + e.getMessage());
            }
        }
    }
}

