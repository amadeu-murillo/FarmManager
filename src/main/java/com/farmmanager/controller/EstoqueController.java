package com.farmmanager.controller;

import com.farmmanager.model.EstoqueItem;
import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.model.FinanceiroDAO; // NOVO
import com.farmmanager.model.Transacao; // NOVO
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.util.Pair; // NOVO

import java.sql.SQLException;
import java.time.LocalDate; // NOVO
import java.time.format.DateTimeFormatter; // NOVO
import java.util.Optional;
import java.util.Locale; 

/**
 * Controller para o EstoqueView.fxml.
 * ATUALIZADO:
 * - 'handleAdicionarItem' renomeado para 'handleComprarItem' e agora lança despesa.
 * - 'handleConsumirItem' agora lança uma despesa baseada no custo médio.
 * - NOVO: 'handleVenderItem' para remover estoque e lançar receita.
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
    private final FinanceiroDAO financeiroDAO; // NOVO
    private final ObservableList<EstoqueItem> dadosTabela;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // NOVO

    // Flag para evitar loops nos listeners
    private boolean isUpdating = false;

    public EstoqueController() {
        estoqueDAO = new EstoqueDAO();
        financeiroDAO = new FinanceiroDAO(); // NOVO
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
     * Manipulador para o botão "+ Comprar Item" (antigo Adicionar Item).
     * ATUALIZADO: Agora também lança uma despesa no financeiro.
     */
    @FXML
    private void handleComprarItem() {
        Dialog<EstoqueItem> dialog = new Dialog<>();
        dialog.setTitle("Comprar Item para Estoque");
        dialog.setHeaderText("Preencha os dados da compra.\nSe o item já existir, os valores serão somados (custo médio).");

        ButtonType adicionarButtonType = new ButtonType("Comprar", ButtonBar.ButtonData.OK_DONE);
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
                // 1. Adiciona ao estoque
                estoqueDAO.addEstoque(item);
                
                // 2. NOVO: Lança a despesa no financeiro
                String data = LocalDate.now().format(dateFormatter);
                String desc = "Compra de " + item.getItemNome();
                double valor = -item.getValorTotal(); // Despesa é negativa
                Transacao transacao = new Transacao(desc, valor, data, "despesa");
                financeiroDAO.addTransacao(transacao);

                carregarDadosDaTabela();
                AlertUtil.showInfo("Sucesso", "Item comprado com sucesso e despesa registrada.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível registrar a compra: " + e.getMessage());
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
     * NOVO: Manipulador para o botão "Vender Item".
     * Lança uma receita no financeiro e dá baixa no estoque.
     */
    @FXML
    private void handleVenderItem() {
        EstoqueItem selecionado = tabelaEstoque.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um item na tabela para vender.");
            return;
        }

        Dialog<Pair<Double, Double>> dialog = new Dialog<>();
        dialog.setTitle("Vender Item do Estoque");
        dialog.setHeaderText("Item: " + selecionado.getItemNome() + " (Disponível: " + selecionado.getQuantidade() + " " + selecionado.getUnidade() + ")");

        ButtonType venderButtonType = new ButtonType("Vender", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(venderButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField qtdField = new TextField("1.0");
        // Sugere o preço de venda baseado no custo unitário (valor de estoque)
        TextField precoVendaField = new TextField(String.format(Locale.US, "%.2f", selecionado.getValorUnitario()));
        Label valorTotalVendaLabel = new Label("Total da Venda: R$ 0,00");

        grid.add(new Label("Quantidade a Vender:"), 0, 0);
        grid.add(qtdField, 1, 0);
        grid.add(new Label("Preço de Venda (unitário):"), 0, 1);
        grid.add(precoVendaField, 1, 1);
        grid.add(valorTotalVendaLabel, 1, 2);

        // Listener para atualizar o total da venda
        ChangeListener<String> listener = (obs, oldV, newV) -> {
            try {
                double qtd = parseDouble(qtdField.getText());
                double preco = parseDouble(precoVendaField.getText());
                valorTotalVendaLabel.setText(String.format(Locale.US, "Total da Venda: R$ %.2f", qtd * preco));
            } catch (NumberFormatException e) {
                valorTotalVendaLabel.setText("Total da Venda: R$ ---");
            }
        };

        qtdField.textProperty().addListener(listener);
        precoVendaField.textProperty().addListener(listener);
        listener.changed(null, null, null); // Calcula o valor inicial

        dialog.getDialogPane().setContent(grid);

        // Converte o resultado para um Par (Qtd, PrecoUnit)
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == venderButtonType) {
                try {
                    double qtd = parseDouble(qtdField.getText());
                    double preco = parseDouble(precoVendaField.getText());

                    if (qtd <= 0 || preco < 0) {
                        AlertUtil.showError("Valor Inválido", "Quantidade deve ser positiva e preço não pode ser negativo.");
                        return null;
                    }
                    if (qtd > selecionado.getQuantidade()) {
                         AlertUtil.showError("Estoque Insuficiente", "Disponível: " + selecionado.getQuantidade() + ". Solicitado: " + qtd);
                         return null;
                    }
                    return new Pair<>(qtd, preco);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valores de quantidade ou R$ inválidos.");
                    return null;
                }
            }
            return null;
        });

        Optional<Pair<Double, Double>> result = dialog.showAndWait();

        result.ifPresent(par -> {
            double qtdAVender = par.getKey();
            double precoVendaUnitario = par.getValue();
            
            try {
                // 1. Dá baixa no estoque
                estoqueDAO.consumirEstoque(selecionado.getId(), qtdAVender);

                // 2. Lança a receita
                double valorReceita = qtdAVender * precoVendaUnitario;
                String data = LocalDate.now().format(dateFormatter);
                String desc = "Venda de " + selecionado.getItemNome();
                Transacao transacao = new Transacao(desc, valorReceita, data, "receita"); // Receita é positiva
                financeiroDAO.addTransacao(transacao);

                AlertUtil.showInfo("Sucesso", "Venda registrada com sucesso. Estoque atualizado e receita lançada.");
                carregarDadosDaTabela(); // Recarrega a tabela

            } catch (IllegalStateException e) {
                AlertUtil.showError("Erro de Estoque", e.getMessage());
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível registrar a venda: " + e.getMessage());
            }
        });
    }

    /**
     * Manipulador para o botão "Consumir Item" (Uso Interno).
     * ATUALIZADO: Agora também lança uma despesa no financeiro.
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
        dialog.setTitle("Consumir Item do Estoque (Uso Interno)");
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

                // 1. Dá baixa no estoque
                estoqueDAO.consumirEstoque(selecionado.getId(), qtdAConsumir);
                
                // 2. NOVO: Lança a despesa
                double custoConsumo = qtdAConsumir * selecionado.getValorUnitario();
                if (custoConsumo > 0) {
                    String data = LocalDate.now().format(dateFormatter);
                    String desc = "Consumo (Uso Interno) de " + selecionado.getItemNome();
                    Transacao transacao = new Transacao(desc, -custoConsumo, data, "despesa");
                    financeiroDAO.addTransacao(transacao);
                    
                    AlertUtil.showInfo("Sucesso", "Estoque atualizado e despesa de consumo registrada.");
                } else {
                     AlertUtil.showInfo("Sucesso", "Estoque atualizado (item sem valor de custo).");
                }
                
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
     * Manipulador para o botão "- Remover Item" (Ajuste).
     * Este botão NÃO gera transação financeira.
     */
    @FXML
    private void handleRemoverItem() {
        EstoqueItem selecionado = tabelaEstoque.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um item na tabela para remover.");
            return;
        }

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Remoção (Ajuste)", 
            "Tem certeza que deseja remover o item '" + selecionado.getItemNome() + "' do estoque?\n" +
            "Esta ação é um AJUSTE e NÃO lançará transações financeiras.");

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

