package com.farmmanager.controller;

import com.farmmanager.model.EstoqueItem;
import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.model.FinanceiroDAO; // NOVO
import com.farmmanager.model.Transacao; // NOVO
import com.farmmanager.model.Conta; // NOVO - PASSO 1
import com.farmmanager.model.ContaDAO; // NOVO - PASSO 1
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox; // <-- IMPORTAÇÃO ADICIONADA
import javafx.geometry.Pos; // <-- IMPORTAÇÃO ADICIONADA
import javafx.util.Pair; // NOVO

import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate; // NOVO
import java.time.format.DateTimeFormatter; // NOVO
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale; 
import java.util.stream.Collectors;

/**
 * Controller para o EstoqueView.fxml.
 * ATUALIZADO:
 * - 'handleAdicionarItem' renomeado para 'handleComprarItem' e agora lança despesa.
 * - 'handleConsumirItem' agora lança uma despesa baseada no custo médio.
 * - NOVO: 'handleVenderItem' para remover estoque e lançar receita.
 * - NOVO: 'handleConsumirItem' agora pede uma descrição de uso.
 * - NOVO: Adicionadas colunas de data na tabela.
 * - NOVO: Adicionado filtro de busca, resumo de valor total, alerta de baixo estoque e função de editar.
 * - ATUALIZAÇÃO (handleVenderItem): Adicionado botão "MAX" para preencher a quantidade total.
 * - ATUALIZAÇÃO (handleComprarItem): Adicionada lógica de "Compra a Prazo" (PASSO 1 a 4).
 * - ATUALIZAÇÃO (handleVenderItem): Adicionada lógica de "Venda a Prazo".
 */
public class EstoqueController {

    // --- Constantes ---
    private static final double LIMITE_BAIXO_ESTOQUE = 10.0;

    // --- Componentes FXML ---
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
    @FXML
    private TableColumn<EstoqueItem, String> colDataCriacao; // NOVO
    @FXML
    private TableColumn<EstoqueItem, String> colDataModificacao; // NOVO

    // NOVO: Componentes de Filtro e Resumo
    @FXML
    private Label lblValorTotalEstoque;
    @FXML
    private TextField filtroNome;

    // --- Lógica Interna ---
    private final EstoqueDAO estoqueDAO;
    private final FinanceiroDAO financeiroDAO; // NOVO
    private final ContaDAO contaDAO; // NOVO - PASSO 1
    private final ObservableList<EstoqueItem> dadosTabelaFiltrada; // O que está visível na tabela
    private List<EstoqueItem> listaMestraEstoque; // Lista completa do banco
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); // NOVO
    private final NumberFormat currencyFormatter; // NOVO

    // Flag para evitar loops nos listeners
    private boolean isUpdating = false;

    public EstoqueController() {
        estoqueDAO = new EstoqueDAO();
        financeiroDAO = new FinanceiroDAO(); // NOVO
        contaDAO = new ContaDAO(); // NOVO - PASSO 1
        dadosTabelaFiltrada = FXCollections.observableArrayList(); // ATUALIZADO
        listaMestraEstoque = new ArrayList<>(); // NOVO
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")); // NOVO
    }

    @FXML
    public void initialize() {
        colItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemNome.setCellValueFactory(new PropertyValueFactory<>("itemNome"));
        colItemQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colItemUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));
        colItemValorUnit.setCellValueFactory(new PropertyValueFactory<>("valorUnitario")); // NOVO
        colItemValorTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal")); // NOVO
        colDataCriacao.setCellValueFactory(new PropertyValueFactory<>("dataCriacao")); // NOVO
        colDataModificacao.setCellValueFactory(new PropertyValueFactory<>("dataModificacao")); // NOVO

        tabelaEstoque.setItems(dadosTabelaFiltrada); // ATUALIZADO

        // NOVO: Adiciona listener para o campo de filtro
        filtroNome.textProperty().addListener((obs, oldV, newV) -> aplicarFiltroEAtualizarResumo());

        // NOVO: Adiciona RowFactory para destacar baixo estoque
        tabelaEstoque.setRowFactory(tv -> new TableRow<EstoqueItem>() {
            @Override
            protected void updateItem(EstoqueItem item, boolean empty) {
                super.updateItem(item, empty);
                // Limpa estilos anteriores
                getStyleClass().remove("table-row-cell:warning");

                if (empty || item == null) {
                    setStyle("");
                } else if (item.getQuantidade() <= LIMITE_BAIXO_ESTOQUE) {
                    // Aplica a classe CSS de aviso
                    if (!getStyleClass().contains("table-row-cell:warning")) {
                        getStyleClass().add("table-row-cell:warning");
                    }
                }
            }
        });

        carregarDadosMestres(); // ATUALIZADO
    }

    /**
     * NOVO: Carrega todos os dados do banco para a lista mestra e atualiza a tela.
     */
    private void carregarDadosMestres() {
        try {
            listaMestraEstoque.clear();
            listaMestraEstoque.addAll(estoqueDAO.listEstoque());
            aplicarFiltroEAtualizarResumo(); // Aplica filtro (vazio) e atualiza resumo
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar o estoque.");
        }
    }

    /**
     * NOVO: Filtra a lista mestra com base no campo de busca e atualiza a tabela.
     */
    private void aplicarFiltroEAtualizarResumo() {
        String filtro = filtroNome.getText().toLowerCase().trim();

        // 1. Filtra a lista
        List<EstoqueItem> listaFiltrada;
        if (filtro.isEmpty()) {
            listaFiltrada = new ArrayList<>(listaMestraEstoque);
        } else {
            listaFiltrada = listaMestraEstoque.stream()
                .filter(item -> item.getItemNome().toLowerCase().contains(filtro))
                .collect(Collectors.toList());
        }

        // 2. Atualiza a tabela
        dadosTabelaFiltrada.setAll(listaFiltrada);

        // 3. Atualiza o resumo
        atualizarResumoEstoque();
    }

    /**
     * NOVO: Busca o valor total do DAO e atualiza o label.
     */
    private void atualizarResumoEstoque() {
        try {
            double valorTotal = estoqueDAO.getValorTotalEmEstoque();
            lblValorTotalEstoque.setText(currencyFormatter.format(valorTotal));
        } catch (SQLException e) {
            lblValorTotalEstoque.setText("Erro ao carregar");
            e.printStackTrace();
        }
    }


    /**
     * Manipulador para o botão "+ Comprar Item" (antigo Adicionar Item).
     * ATUALIZADO: Agora também lança uma despesa no financeiro.
     */
    @FXML
    private void handleComprarItem() {
        Dialog<CompraInfo> dialog = new Dialog<>(); // ALTERADO - PASSO 2
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
        
        // NOVO: Tipo de Pagamento - PASSO 2
        Label tipoPagLabel = new Label("Tipo de Pagamento:");
        ComboBox<String> tipoPagCombo = new ComboBox<>(
                FXCollections.observableArrayList("À Vista", "A Prazo")
        );
        tipoPagCombo.getSelectionModel().select("À Vista");

        // NOVO: Data de Vencimento - PASSO 2
        Label vencimentoLabel = new Label("Data Vencimento:");
        DatePicker vencimentoPicker = new DatePicker(LocalDate.now().plusDays(30));

        // Adiciona ao grid - PASSO 2
        grid.add(tipoPagLabel, 0, 5);
        grid.add(tipoPagCombo, 1, 5);
        grid.add(vencimentoLabel, 0, 6);
        grid.add(vencimentoPicker, 1, 6);


        // --- Lógica de Cálculo Automático ---

        // Se Qtd ou Vlr. Unitário mudam, calcula o Vlr. Total
        qtdField.textProperty().addListener((obs, oldV, newV) -> calcularTotal(qtdField, valorUnitarioField, valorTotalField));
        valorUnitarioField.textProperty().addListener((obs, oldV, newV) -> calcularTotal(qtdField, valorUnitarioField, valorTotalField));

        // Se Vlr. Total muda, calcula o Vlr. Unitário
        valorTotalField.textProperty().addListener((obs, oldV, newV) -> calcularUnitario(qtdField, valorUnitarioField, valorTotalField));
        
        // --- Fim da Lógica ---

        // NOVO: Lógica de Visibilidade - PASSO 2
        vencimentoLabel.setVisible(false);
        vencimentoPicker.setVisible(false);
        vencimentoLabel.setManaged(false);
        vencimentoPicker.setManaged(false);

        tipoPagCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean aPrazo = newVal.equals("A Prazo");
            vencimentoLabel.setVisible(aPrazo);
            vencimentoPicker.setVisible(aPrazo);
            vencimentoLabel.setManaged(aPrazo);
            vencimentoPicker.setManaged(aPrazo);
        });
        // --- Fim da Lógica de Visibilidade ---

        dialog.getDialogPane().setContent(grid);

        // ATUALIZADO - PASSO 3
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    String unidade = unidadeField.getText();
                    double qtd = parseDouble(qtdField.getText());
                    double valorUnitario = parseDouble(valorUnitarioField.getText());
                    double valorTotal = parseDouble(valorTotalField.getText());

                    if (nome.isEmpty() || unidade.isEmpty() || qtd <= 0 || valorTotal <= 0) {
                         AlertUtil.showError("Erro de Validação", "Todos os campos de item são obrigatórios e valores devem ser positivos.");
                         return null;
                    }
                    
                    EstoqueItem item = new EstoqueItem(nome, qtd, unidade, valorUnitario, valorTotal);

                    // Lógica dos novos campos
                    String tipoPagamento = tipoPagCombo.getSelectionModel().getSelectedItem();
                    LocalDate dataVencimento = vencimentoPicker.getValue();

                    if (tipoPagamento.equals("A Prazo") && dataVencimento == null) {
                        AlertUtil.showError("Erro de Validação", "A Data de Vencimento é obrigatória para compras 'A Prazo'.");
                        return null;
                    }

                    return new CompraInfo(item, tipoPagamento, dataVencimento);

                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valores de quantidade ou R$ inválidos.");
                    return null;
                }
            }
            return null;
        });

        Optional<CompraInfo> result = dialog.showAndWait(); // ALTERADO - PASSO 4

        // ATUALIZADO - PASSO 4
        result.ifPresent(compraInfo -> {
            try {
                // 1. Adicionar ao estoque (Isto acontece em ambos os casos)
                estoqueDAO.addEstoque(compraInfo.item);

                // 2. Lógica de Pagamento
                if (compraInfo.tipoPagamento.equals("À Vista")) {
                    // LÓGICA ANTIGA: Lançar despesa imediata
                    String data = LocalDate.now().format(dateFormatter);
                    String desc = "Compra (à vista): " + compraInfo.item.getItemNome();
                    double valor = -compraInfo.item.getValorTotal(); // Despesa é negativa
                    
                    Transacao transacao = new Transacao(desc, valor, data, "despesa");
                    financeiroDAO.addTransacao(transacao);
                    
                    AlertUtil.showInfo("Sucesso", "Item comprado (à vista) e despesa registrada no financeiro.");

                } else {
                    // LÓGICA NOVA: Lançar Conta a Pagar
                    String desc = "Compra (a prazo): " + compraInfo.item.getItemNome();
                    
                    Conta conta = new Conta(
                        desc,
                        compraInfo.item.getValorTotal(), // ContaDAO espera valor positivo
                        compraInfo.dataVencimento.toString(),
                        "pagar", // Tipo da conta
                        "pendente" // Status inicial
                    );
                    
                    contaDAO.addConta(conta); // Usa o DAO adicionado no Passo 1
                    
                    AlertUtil.showInfo("Sucesso", "Item comprado (a prazo) e 'Conta a Pagar' registrada com sucesso.");
                }

                // 3. Atualizar a tela
                carregarDadosMestres(); 

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
     * ATUALIZADO: Adicionado botão "MAX".
     */
    @FXML
    private void handleVenderItem() {
        EstoqueItem selecionado = tabelaEstoque.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um item na tabela para vender.");
            return;
        }

        // Dialog<Pair<Double, Double>> dialog = new Dialog<>(); // Antigo
        Dialog<VendaInfo> dialog = new Dialog<>(); // NOVO
        dialog.setTitle("Vender Item do Estoque");
        dialog.setHeaderText("Item: " + selecionado.getItemNome() + " (Disponível: " + selecionado.getQuantidade() + " " + selecionado.getUnidade() + ")");

        ButtonType venderButtonType = new ButtonType("Vender", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(venderButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField qtdField = new TextField("1.0");

        // --- INÍCIO DA MODIFICAÇÃO ---
        
        // NOVO: Criar botão MAX
        Button maxButton = new Button("MAX");
        maxButton.setOnAction(e -> {
            // Define o texto do qtdField para a quantidade total disponível
            // Usando Locale.US para garantir o formato com ponto decimal
            qtdField.setText(String.format(Locale.US, "%.2f", selecionado.getQuantidade()));
        });

        // NOVO: Criar HBox para agrupar TextField e Botão
        HBox qtdBox = new HBox(5, qtdField, maxButton); // 5 é o espaçamento
        qtdBox.setAlignment(Pos.CENTER_LEFT);

        // --- FIM DA MODIFICAÇÃO ---

        // Sugere o preço de venda baseado no custo unitário (valor de estoque)
        TextField precoVendaField = new TextField(String.format(Locale.US, "%.2f", selecionado.getValorUnitario()));
        Label valorTotalVendaLabel = new Label("Total da Venda: R$ 0,00");

        // --- NOVO: Campos para Venda a Prazo ---
        Label tipoPagLabel = new Label("Recebimento:");
        ComboBox<String> tipoPagCombo = new ComboBox<>(
                FXCollections.observableArrayList("À Vista", "A Prazo")
        );
        tipoPagCombo.getSelectionModel().select("À Vista");

        Label vencimentoLabel = new Label("Data Recebimento:");
        DatePicker vencimentoPicker = new DatePicker(LocalDate.now().plusDays(30));
        
        vencimentoLabel.setVisible(false);
        vencimentoPicker.setVisible(false);
        vencimentoLabel.setManaged(false);
        vencimentoPicker.setManaged(false);

        tipoPagCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean aPrazo = newVal.equals("A Prazo");
            vencimentoLabel.setVisible(aPrazo);
            vencimentoPicker.setVisible(aPrazo);
            vencimentoLabel.setManaged(aPrazo);
            vencimentoPicker.setManaged(aPrazo);
        });
        // --- Fim Venda a Prazo ---

        grid.add(new Label("Quantidade a Vender:"), 0, 0);
        grid.add(qtdBox, 1, 0); // ATUALIZADO: Adiciona o HBox
        grid.add(new Label("Preço de Venda (unitário):"), 0, 1);
        grid.add(precoVendaField, 1, 1);
        
        // --- NOVOS CAMPOS NO GRID ---
        grid.add(tipoPagLabel, 0, 2);
        grid.add(tipoPagCombo, 1, 2);
        grid.add(vencimentoLabel, 0, 3);
        grid.add(vencimentoPicker, 1, 3);
        // --- FIM NOVOS CAMPOS ---
        
        grid.add(valorTotalVendaLabel, 1, 4); // Posição atualizada

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

        // Converte o resultado para o objeto VendaInfo
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == venderButtonType) {
                try {
                    double qtd = parseDouble(qtdField.getText());
                    double preco = parseDouble(precoVendaField.getText());
                    String tipoRecebimento = tipoPagCombo.getSelectionModel().getSelectedItem(); // NOVO
                    LocalDate dataVencimento = vencimentoPicker.getValue(); // NOVO

                    if (qtd <= 0 || preco < 0) {
                        AlertUtil.showError("Valor Inválido", "Quantidade deve ser positiva e preço não pode ser negativo.");
                        return null;
                    }
                    if (qtd > selecionado.getQuantidade()) {
                         AlertUtil.showError("Estoque Insuficiente", "Disponível: " + selecionado.getQuantidade() + ". Solicitado: " + qtd);
                         return null;
                    }
                    // NOVO: Validação da data a prazo
                    if (tipoRecebimento.equals("A Prazo") && dataVencimento == null) {
                        AlertUtil.showError("Erro de Validação", "A Data de Recebimento é obrigatória para vendas 'A Prazo'.");
                        return null;
                    }
                    
                    // return new Pair<>(qtd, preco); // Antigo
                    return new VendaInfo(qtd, preco, tipoRecebimento, dataVencimento); // NOVO
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valores de quantidade ou R$ inválidos.");
                    return null;
                }
            }
            return null;
        });

        // Optional<Pair<Double, Double>> result = dialog.showAndWait(); // Antigo
        Optional<VendaInfo> result = dialog.showAndWait(); // NOVO

        result.ifPresent(vendaInfo -> { // Atualizado
            // double qtdAVender = par.getKey(); // Antigo
            // double precoVendaUnitario = par.getValue(); // Antigo
            
            try {
                // 1. Dá baixa no estoque (Sempre)
                estoqueDAO.consumirEstoque(selecionado.getId(), vendaInfo.qtdAVender);

                double valorReceita = vendaInfo.qtdAVender * vendaInfo.precoVendaUnitario;
                String desc = "Venda de " + selecionado.getItemNome();

                // 2. Lança a receita (À Vista ou A Prazo)
                if (vendaInfo.tipoRecebimento.equals("À Vista")) {
                    // Lógica antiga: Lança no financeiro
                    String data = LocalDate.now().format(dateFormatter);
                    Transacao transacao = new Transacao(desc, valorReceita, data, "receita"); // Receita é positiva
                    financeiroDAO.addTransacao(transacao);
                    AlertUtil.showInfo("Sucesso", "Venda (à vista) registrada. Estoque atualizado e receita lançada.");

                } else {
                    // Lógica nova: Lança em Contas a Receber
                    Conta conta = new Conta(
                        desc,
                        valorReceita, // Valor positivo
                        vendaInfo.dataVencimento.toString(),
                        "receber", // Tipo
                        "pendente" // Status
                    );
                    contaDAO.addConta(conta);
                    AlertUtil.showInfo("Sucesso", "Venda (a prazo) registrada. Estoque atualizado e 'Conta a Receber' criada.");
                }

                carregarDadosMestres(); // ATUALIZADO

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
     * NOVO: Pede uma descrição de uso.
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

        // NOVO: Diálogo customizado para quantidade e descrição
        Dialog<Pair<Double, String>> dialog = new Dialog<>();
        dialog.setTitle("Consumir Item do Estoque (Uso Interno)");
        dialog.setHeaderText("Item: " + selecionado.getItemNome() + " (Disponível: " + qtdAtualStr + " " + selecionado.getUnidade() + ")");

        ButtonType consumirButtonType = new ButtonType("Consumir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(consumirButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField qtdField = new TextField("1.0");
        TextField descField = new TextField();
        descField.setPromptText("Ex: Manutenção do trator");

        grid.add(new Label("Quantidade a consumir:"), 0, 0);
        grid.add(qtdField, 1, 0);
        grid.add(new Label("Descrição de uso:"), 0, 1);
        grid.add(descField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Converte o resultado para um Par (Qtd, Descricao)
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == consumirButtonType) {
                try {
                    double qtd = parseDouble(qtdField.getText());
                    String desc = descField.getText();

                    if (qtd <= 0) {
                        AlertUtil.showError("Valor Inválido", "A quantidade a consumir deve ser positiva.");
                        return null;
                    }
                    if (desc.isEmpty()) {
                        AlertUtil.showError("Descrição Obrigatória", "Por favor, insira uma descrição de uso.");
                        return null;
                    }
                     if (qtd > selecionado.getQuantidade()) {
                         AlertUtil.showError("Estoque Insuficiente", "Disponível: " + selecionado.getQuantidade() + ". Solicitado: " + qtd);
                         return null;
                    }
                    return new Pair<>(qtd, desc);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor de quantidade inválido. Use apenas números (ex: 10.5).");
                    return null;
                }
            }
            return null;
        });

        Optional<Pair<Double, String>> result = dialog.showAndWait();

        result.ifPresent(pair -> {
            try {
                double qtdAConsumir = pair.getKey();
                String descricaoUso = pair.getValue();

                // 1. Dá baixa no estoque
                estoqueDAO.consumirEstoque(selecionado.getId(), qtdAConsumir);
                
                // 2. NOVO: Lança a despesa <-- REMOVIDO
                // O custo já foi registrado na COMPRA do insumo.
                // double custoConsumo = qtdAConsumir * selecionado.getValorUnitario();
                // if (custoConsumo > 0) {
                //     String data = LocalDate.now().format(dateFormatter);
                //     // NOVO: Usa a descrição do diálogo
                //     String desc = "Consumo (" + descricaoUso + "): " + selecionado.getItemNome();
                //     Transacao transacao = new Transacao(desc, -custoConsumo, data, "despesa");
                //     financeiroDAO.addTransacao(transacao);
                //     
                //     AlertUtil.showInfo("Sucesso", "Estoque atualizado e despesa de consumo registrada.");
                // } else {
                //      AlertUtil.showInfo("Sucesso", "Estoque atualizado (item sem valor de custo).");
                // }
                
                // Nova mensagem de sucesso (sem mencionar despesa)
                AlertUtil.showInfo("Sucesso", "Estoque atualizado com sucesso.");
                
                carregarDadosMestres(); // ATUALIZADO

            } catch (IllegalStateException e) {
                // Captura a exceção de estoque insuficiente lançada pelo DAO
                AlertUtil.showError("Erro de Estoque", e.getMessage());
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível consumir o item: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Manipulador para o botão "Editar Item".
     * Permite alterar o nome e a unidade de um item.
     */
    @FXML
    private void handleEditarItem() {
        EstoqueItem selecionado = tabelaEstoque.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um item na tabela para editar.");
            return;
        }

        Dialog<Pair<String, String>> dialog = new Dialog<>();
        dialog.setTitle("Editar Item");
        dialog.setHeaderText("Editar dados de: " + selecionado.getItemNome());

        ButtonType salvarButtonType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(salvarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomeField = new TextField(selecionado.getItemNome());
        TextField unidadeField = new TextField(selecionado.getUnidade());

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Unidade:"), 0, 1);
        grid.add(unidadeField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        // Converte o resultado para um Par (Nome, Unidade)
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == salvarButtonType) {
                String nome = nomeField.getText().trim();
                String unidade = unidadeField.getText().trim();

                if (nome.isEmpty() || unidade.isEmpty()) {
                    AlertUtil.showError("Erro de Validação", "Nome e Unidade são obrigatórios.");
                    return null;
                }
                return new Pair<>(nome, unidade);
            }
            return null;
        });

        Optional<Pair<String, String>> result = dialog.showAndWait();

        result.ifPresent(pair -> {
            try {
                String novoNome = pair.getKey();
                String novaUnidade = pair.getValue();

                estoqueDAO.updateEstoqueItem(selecionado.getId(), novoNome, novaUnidade);
                
                AlertUtil.showInfo("Sucesso", "Item atualizado com sucesso.");
                carregarDadosMestres(); // ATUALIZADO

            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar o item: " + e.getMessage());
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
                    carregarDadosMestres(); // ATUALIZADO
                } else {
                    AlertUtil.showError("Erro ao Remover", "O item não pôde ser removido.");
                }
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover o item: " + e.getMessage());
            }
        }
    }
    
    // NOVO - PASSO 2: Classe interna simples para guardar o resultado do diálogo
    private static class CompraInfo {
        final EstoqueItem item;
        final String tipoPagamento; // "avista" ou "aprazo"
        final LocalDate dataVencimento; // Pode ser nulo se for "avista"

        CompraInfo(EstoqueItem item, String tipo, LocalDate data) {
            this.item = item;
            this.tipoPagamento = tipo;
            this.dataVencimento = data;
        }
    }
    
    // NOVO: Classe interna simples para guardar o resultado do diálogo de Venda
    private static class VendaInfo {
        final double qtdAVender;
        final double precoVendaUnitario;
        final String tipoRecebimento; // "À Vista" ou "A Prazo"
        final LocalDate dataVencimento; // Pode ser nulo se for "À Vista"

        VendaInfo(double qtd, double preco, String tipo, LocalDate data) {
            this.qtdAVender = qtd;
            this.precoVendaUnitario = preco;
            this.tipoRecebimento = tipo;
            this.dataVencimento = data;
        }
    }
}
