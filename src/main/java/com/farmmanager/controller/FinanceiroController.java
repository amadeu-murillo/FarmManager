package com.farmmanager.controller;

import com.farmmanager.model.Transacao;
import com.farmmanager.model.FinanceiroDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Controller para o FinanceiroView.fxml.
 * ATUALIZADO: Implementados handlers para adicionar receitas e despesas.
 */
public class FinanceiroController {

    @FXML
    private TableView<Transacao> tabelaFinanceiro;
    @FXML
    private TableColumn<Transacao, Integer> colFinId;
    @FXML
    private TableColumn<Transacao, String> colFinDesc;
    @FXML
    private TableColumn<Transacao, Double> colFinValor;
    @FXML
    private TableColumn<Transacao, String> colFinData;
    @FXML
    private TableColumn<Transacao, String> colFinTipo;

    private final FinanceiroDAO financeiroDAO;
    private final ObservableList<Transacao> dadosTabela;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public FinanceiroController() {
        financeiroDAO = new FinanceiroDAO();
        dadosTabela = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        colFinId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFinDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colFinValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colFinData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colFinTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        
        tabelaFinanceiro.setItems(dadosTabela);
        carregarDadosDaTabela();
    }

    private void carregarDadosDaTabela() {
        try {
            dadosTabela.clear();
            dadosTabela.addAll(financeiroDAO.listTransacoes());
            // TODO: Atualizar o balanço no Dashboard (talvez via um listener ou singleton)
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar as transações.");
        }
    }

    /**
     * NOVO: Manipulador para o botão "+ Nova Receita".
     */
    @FXML
    private void handleNovaReceita() {
        abrirDialogoTransacao("receita");
    }

    /**
     * NOVO: Manipulador para o botão "- Nova Despesa".
     */
    @FXML
    private void handleNovaDespesa() {
        abrirDialogoTransacao("despesa");
    }

    /**
     * NOVO: Método auxiliar para abrir um diálogo de transação (Receita ou Despesa).
     * @param tipo "receita" ou "despesa"
     */
    private void abrirDialogoTransacao(String tipo) {
        boolean isReceita = tipo.equals("receita");
        
        Dialog<Transacao> dialog = new Dialog<>();
        dialog.setTitle(isReceita ? "Adicionar Nova Receita" : "Adicionar Nova Despesa");
        dialog.setHeaderText("Preencha os dados da transação.");

        ButtonType adicionarButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField descField = new TextField();
        descField.setPromptText(isReceita ? "Ex: Venda de Soja" : "Ex: Compra de Diesel");
        TextField valorField = new TextField();
        valorField.setPromptText("Ex: 5000.00");

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String desc = descField.getText();
                    double valor = Double.parseDouble(valorField.getText());
                    String data = LocalDate.now().format(dateFormatter);

                    if (desc.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "A descrição é obrigatória.");
                        return null;
                    }
                    if (valor <= 0) {
                         AlertUtil.showError("Erro de Validação", "O valor deve ser positivo.");
                         return null;
                    }

                    // Se for despesa, inverte o valor para negativo
                    double valorFinal = isReceita ? valor : -valor;
                    
                    return new Transacao(desc, valorFinal, data, tipo);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<Transacao> result = dialog.showAndWait();

        result.ifPresent(transacao -> {
            try {
                financeiroDAO.addTransacao(transacao);
                carregarDadosDaTabela();
                AlertUtil.showInfo("Sucesso", "Transação adicionada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar a transação: " + e.getMessage());
            }
        });
    }
}
