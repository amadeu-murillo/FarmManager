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

/**
 * Controller para o EstoqueView.fxml.
 * ATUALIZADO: Implementados handlers para adicionar e remover itens.
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

    private final EstoqueDAO estoqueDAO;
    private final ObservableList<EstoqueItem> dadosTabela;

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
     * NOVO: Manipulador para o botão "+ Adicionar Item".
     * Usa a lógica UPSERT do EstoqueDAO.
     */
    @FXML
    private void handleAdicionarItem() {
        Dialog<EstoqueItem> dialog = new Dialog<>();
        dialog.setTitle("Adicionar Item ao Estoque");
        dialog.setHeaderText("Preencha os dados do item.\nSe o item já existir, a quantidade será somada.");

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

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Quantidade:"), 0, 1);
        grid.add(qtdField, 1, 1);
        grid.add(new Label("Unidade:"), 0, 2);
        grid.add(unidadeField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    double qtd = Double.parseDouble(qtdField.getText());
                    String unidade = unidadeField.getText();

                    if (nome.isEmpty() || unidade.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "Todos os campos são obrigatórios.");
                        return null;
                    }
                    if (qtd <= 0) {
                         AlertUtil.showError("Erro de Validação", "A quantidade deve ser positiva.");
                         return null;
                    }
                    // Usamos o construtor sem ID, pois o DAO cuida disso
                    return new EstoqueItem(nome, qtd, unidade);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor de quantidade inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<EstoqueItem> result = dialog.showAndWait();

        result.ifPresent(item -> {
            try {
                estoqueDAO.addEstoque(item.getItemNome(), item.getQuantidade(), item.getUnidade());
                carregarDadosDaTabela();
                AlertUtil.showInfo("Sucesso", "Estoque atualizado com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar o estoque: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Manipulador para o botão "- Remover Item".
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
