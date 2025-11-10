package com.farmmanager.controller;

import com.farmmanager.model.EstoqueItem;
import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.model.FinanceiroDAO; 
import com.farmmanager.model.Transacao; 
import com.farmmanager.model.Conta; 
import com.farmmanager.model.ContaDAO; 
// NOVO: Imports para Histórico
import com.farmmanager.model.AtividadeSafraDAO;
import com.farmmanager.model.AtividadeSafraDAO.ConsumoHistoricoInfo;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task; // NOVO: Import para Task
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.beans.value.ChangeListener;
import javafx.scene.Node; // NOVO IMPORT
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox; 
import javafx.scene.layout.VBox; // NOVO: Import para o VBox
import javafx.geometry.Pos; 
import javafx.util.Pair; 
// NOVO: Import para ScrollPane
import javafx.scene.control.ScrollPane; 
import javafx.stage.FileChooser; // NOVO: Import para FileChooser
import java.io.File; // NOVO: Import para File
import java.io.IOException; // NOVO: Import para IOException
import java.io.PrintWriter; // NOVO: Import para PrintWriter

import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate; 
import java.time.format.DateTimeFormatter; 
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Locale; 
import java.util.stream.Collectors;

/**
 * Controller para o EstoqueView.fxml.
 * ATUALIZADO:
 * - 'handleAdicionarItem' é usado para adicionar itens ao estoque (com ou sem registro financeiro).
 * - 'handleConsumirItem' agora lança uma despesa baseada no custo médio.
 * - NOVO: 'handleVenderItem' para remover estoque e lançar receita.
 * - NOVO: 'handleConsumirItem' agora pede uma descrição de uso.
 * - NOVO: Adicionadas colunas de data na tabela.
 * - NOVO: Adicionado filtro de busca, resumo de valor total, alerta de baixo estoque e função de editar.
 * - ATUALIZAÇÃO (handleVenderItem): Adicionado botão "MAX" para preencher a quantidade total.
 * - ATUALIZAÇÃO (handleAdicionarItem): Adicionada lógica de "Compra a Prazo" (PASSO 1 a 4).
 * - ATUALIZAÇÃO (handleVenderItem): Adicionada lógica de "Venda a Prazo".
 * - MELHORIA CRÍTICA: Carregamento de dados (carregarDadosMestres)
 * movido para uma Task em background para não congelar a UI.
 * - MELHORIA USABILIDADE: 'handleAdicionarItem' agora permite adicionar item sem registro financeiro (ajuste).
 * - ATUALIZADO: Adicionados campos de fornecedor.
 * - ATUALIZADO (handleVenderItem): Adicionados campos de Cliente e validação em tempo real.
 * - ATUALIZADO (handleVenderItem): Diálogo agora usa ScrollPane e é redimensionável.
 * - ATUALIZADO (handleAdicionarItem): Diálogo agora usa ScrollPane e é redimensionável.
 * - NOVO: Implementada Aba de Histórico de Consumo com filtros e exportação CSV.
 */
public class EstoqueController {

    // --- Constantes ---
    private static final double LIMITE_BAIXO_ESTOQUE = 10.0;

    // --- Componentes FXML ---
    
    // Aba 1: Estoque Atual
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
    private TableColumn<EstoqueItem, Double> colItemValorUnit; 
    @FXML
    private TableColumn<EstoqueItem, Double> colItemValorTotal; 
    @FXML
    private TableColumn<EstoqueItem, String> colItemFornecedorNome; // NOVO
    @FXML
    private TableColumn<EstoqueItem, String> colItemFornecedorEmpresa; // NOVO
    @FXML
    private TableColumn<EstoqueItem, String> colDataCriacao; 
    @FXML
    private TableColumn<EstoqueItem, String> colDataModificacao; 
    @FXML
    private TextField filtroNome;
    
    // Aba 2: Histórico de Consumo (NOVO)
    @FXML
    private TableView<ConsumoHistoricoInfo> tabelaHistoricoConsumo;
    @FXML
    private TableColumn<ConsumoHistoricoInfo, String> colHistData;
    @FXML
    private TableColumn<ConsumoHistoricoInfo, String> colHistItem;
    @FXML
    private TableColumn<ConsumoHistoricoInfo, Double> colHistQtd;
    @FXML
    private TableColumn<ConsumoHistoricoInfo, String> colHistUnidade;
    @FXML
    private TableColumn<ConsumoHistoricoInfo, String> colHistDestino;
    @FXML
    private TableColumn<ConsumoHistoricoInfo, String> colHistSafra;
    @FXML
    private TextField filtroHistoricoNome;
    @FXML
    private DatePicker filtroHistoricoDataInicio;
    @FXML
    private DatePicker filtroHistoricoDataFim;
    @FXML
    private Button btnLimparHistorico;
    @FXML
    private Button btnExportarHistoricoCsv;

    // Componentes Gerais
    @FXML
    private Label lblValorTotalEstoque;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private VBox contentVBox; // Container principal (VBox do FXML)

    // --- Lógica Interna ---
    private final EstoqueDAO estoqueDAO;
    private final FinanceiroDAO financeiroDAO; 
    private final ContaDAO contaDAO; 
    private final AtividadeSafraDAO atividadeSafraDAO; // NOVO: Para histórico
    
    private final ObservableList<EstoqueItem> dadosTabelaFiltrada; 
    private List<EstoqueItem> listaMestraEstoque; 
    // NOVO: Listas para histórico
    private final ObservableList<ConsumoHistoricoInfo> dadosTabelaHistorico;
    private List<ConsumoHistoricoInfo> listaMestraHistorico;
    
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd"); 
    private final NumberFormat currencyFormatter; 

    private boolean isUpdating = false;

    /**
     * NOVO: Classe interna para agrupar os resultados da Task
     * ATUALIZADO: Renomeado para EstoquePageData e adicionado historico
     */
    private static class EstoquePageData {
        final List<EstoqueItem> items;
        final double valorTotal;
        final List<ConsumoHistoricoInfo> historico; // NOVO

        EstoquePageData(List<EstoqueItem> items, double valorTotal, List<ConsumoHistoricoInfo> historico) {
            this.items = items;
            this.valorTotal = valorTotal;
            this.historico = historico; // NOVO
        }
    }

    public EstoqueController() {
        estoqueDAO = new EstoqueDAO();
        financeiroDAO = new FinanceiroDAO(); 
        contaDAO = new ContaDAO(); 
        atividadeSafraDAO = new AtividadeSafraDAO(); // NOVO
        
        dadosTabelaFiltrada = FXCollections.observableArrayList(); 
        listaMestraEstoque = new ArrayList<>(); 
        
        dadosTabelaHistorico = FXCollections.observableArrayList(); // NOVO
        listaMestraHistorico = new ArrayList<>(); // NOVO
        
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")); 
    }

    @FXML
    public void initialize() {
        // --- Aba 1: Estoque Atual ---
        colItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemNome.setCellValueFactory(new PropertyValueFactory<>("itemNome"));
        colItemQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colItemUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));
        colItemValorUnit.setCellValueFactory(new PropertyValueFactory<>("valorUnitario")); 
        colItemValorTotal.setCellValueFactory(new PropertyValueFactory<>("valorTotal")); 
        colItemFornecedorNome.setCellValueFactory(new PropertyValueFactory<>("fornecedorNome"));
        colItemFornecedorEmpresa.setCellValueFactory(new PropertyValueFactory<>("fornecedorEmpresa"));
        colDataCriacao.setCellValueFactory(new PropertyValueFactory<>("dataCriacao")); 
        colDataModificacao.setCellValueFactory(new PropertyValueFactory<>("dataModificacao")); 

        tabelaEstoque.setItems(dadosTabelaFiltrada); 
        filtroNome.textProperty().addListener((obs, oldV, newV) -> aplicarFiltro());

        // Adiciona RowFactory para destacar baixo estoque
        tabelaEstoque.setRowFactory(tv -> new TableRow<EstoqueItem>() {
            @Override
            protected void updateItem(EstoqueItem item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("table-row-cell:warning");

                if (empty || item == null) {
                    setStyle("");
                } else if (item.getQuantidade() <= LIMITE_BAIXO_ESTOQUE) {
                    if (!getStyleClass().contains("table-row-cell:warning")) {
                        getStyleClass().add("table-row-cell:warning");
                    }
                }
            }
        });

        // --- Aba 2: Histórico de Consumo (NOVO) ---
        colHistData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colHistItem.setCellValueFactory(new PropertyValueFactory<>("itemNome"));
        colHistQtd.setCellValueFactory(new PropertyValueFactory<>("quantidadeConsumida"));
        colHistUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));
        colHistDestino.setCellValueFactory(new PropertyValueFactory<>("descricaoAtividade"));
        colHistSafra.setCellValueFactory(new PropertyValueFactory<>("safraDestino"));
        tabelaHistoricoConsumo.setItems(dadosTabelaHistorico);

        // Listeners para filtros do histórico
        filtroHistoricoNome.textProperty().addListener((o, ov, nv) -> aplicarFiltroHistorico());
        filtroHistoricoDataInicio.valueProperty().addListener((o, ov, nv) -> aplicarFiltroHistorico());
        filtroHistoricoDataFim.valueProperty().addListener((o, ov, nv) -> aplicarFiltroHistorico());

        // --- Carregamento Geral ---
        carregarDadosMestres(); 
    }

    /**
     * NOVO: Controla a visibilidade do indicador de carregamento.
     */
    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisible(isLoading);
        loadingIndicator.setManaged(isLoading);
        
        contentVBox.setDisable(isLoading);
        contentVBox.setOpacity(isLoading ? 0.5 : 1.0); 
    }

    /**
     * ATUALIZADO: Carrega todos os dados do banco (Itens, Valor Total, Histórico)
     * em uma Task de background.
     */
    private void carregarDadosMestres() {
        Task<EstoquePageData> carregarTask = new Task<EstoquePageData>() {
            @Override
            protected EstoquePageData call() throws Exception {
                // Chamadas de banco de dados (demoradas)
                List<EstoqueItem> items = estoqueDAO.listEstoque();
                double valorTotal = estoqueDAO.getValorTotalEmEstoque();
                // NOVO: Busca o histórico
                List<ConsumoHistoricoInfo> historico = atividadeSafraDAO.listConsumoHistorico();
                
                return new EstoquePageData(items, valorTotal, historico);
            }
        };

        carregarTask.setOnSucceeded(e -> {
            EstoquePageData data = carregarTask.getValue();

            // 1. Atualiza a lista mestra de estoque
            listaMestraEstoque.clear();
            listaMestraEstoque.addAll(data.items);

            // 2. Atualiza o resumo
            lblValorTotalEstoque.setText(currencyFormatter.format(data.valorTotal));

            // 3. NOVO: Atualiza a lista mestra de histórico
            listaMestraHistorico.clear();
            listaMestraHistorico.addAll(data.historico);

            // 4. Aplica os filtros (rápido, em memória)
            aplicarFiltro();
            aplicarFiltroHistorico(); // NOVO
            
            // 5. Esconde o loading
            showLoading(false);
        });

        carregarTask.setOnFailed(e -> {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar o estoque e o histórico.");
            carregarTask.getException().printStackTrace();
            showLoading(false);
        });

        showLoading(true);
        new Thread(carregarTask).start();
    }


    /**
     * ATUALIZADO: Renomeado. Agora *apenas* filtra a lista mestra (em memória).
     * Não faz mais chamadas ao DAO.
     */
    private void aplicarFiltro() {
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
        
        // 3. O resumo (lblValorTotalEstoque) NÃO é mais atualizado aqui.
        // Ele é atualizado apenas no carregarDadosMestres().
    }
    
    /**
     * NOVO: Filtra a lista mestra de histórico (em memória).
     */
    @FXML
    private void aplicarFiltroHistorico() {
        String filtroNome = filtroHistoricoNome.getText().toLowerCase().trim();
        LocalDate dataInicio = filtroHistoricoDataInicio.getValue();
        LocalDate dataFim = filtroHistoricoDataFim.getValue();

        List<ConsumoHistoricoInfo> listaFiltrada;

        listaFiltrada = listaMestraHistorico.stream()
            .filter(item -> {
                // Filtro por Nome
                boolean nomeMatch = filtroNome.isEmpty() || 
                                    item.getItemNome().toLowerCase().contains(filtroNome);
                
                // Filtro por Data
                boolean dataMatch = true;
                try {
                    LocalDate dataItem = LocalDate.parse(item.getData(), dateFormatter);
                    if (dataInicio != null && dataItem.isBefore(dataInicio)) {
                        dataMatch = false;
                    }
                    if (dataFim != null && dataItem.isAfter(dataFim)) {
                        dataMatch = false;
                    }
                } catch (Exception e) {
                    dataMatch = false; // Ignora se a data for inválida
                }

                return nomeMatch && dataMatch;
            })
            .collect(Collectors.toList());

        dadosTabelaHistorico.setAll(listaFiltrada);
    }
    
    /**
     * NOVO: Limpa os filtros do histórico.
     */
    @FXML
    private void handleLimparFiltroHistorico() {
        filtroHistoricoNome.clear();
        filtroHistoricoDataInicio.setValue(null);
        filtroHistoricoDataFim.setValue(null);
        // aplicarFiltroHistorico() é chamado pelos listeners
    }

    /**
     * NOVO: Exporta o CSV do histórico filtrado.
     */
    @FXML
    private void handleExportarHistoricoCsv() {
        if (dadosTabelaHistorico.isEmpty()) {
            AlertUtil.showInfo("Nada para Exportar", "A tabela de histórico está vazia. Não há dados para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Histórico de Consumo");
        fileChooser.setInitialFileName("Relatorio_Consumo_Insumos.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(tabelaEstoque.getScene().getWindow());

        if (file == null) {
            return; // Usuário cancelou
        }

        // Tenta escrever o arquivo
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            
            writer.write("\uFEFF"); // Adiciona o BOM do UTF-8 para Excel
            
            StringBuilder sb = new StringBuilder();
            
            // Cabeçalho do CSV
            sb.append("Data;Item Consumido;Qtd.;Unidade;Destino (Atividade);Safra\n");

            // Escreve os dados (usando a lista filtrada 'dadosTabelaHistorico')
            for (ConsumoHistoricoInfo info : dadosTabelaHistorico) {
                sb.append(String.format(Locale.US, "%s;\"%s\";%.2f;\"%s\";\"%s\";\"%s\"\n",
                    info.getData(),
                    info.getItemNome().replace("\"", "\"\""),
                    info.getQuantidadeConsumida(),
                    info.getUnidade().replace("\"", "\"\""),
                    info.getDescricaoAtividade().replace("\"", "\"\""),
                    info.getSafraDestino().replace("\"", "\"\"")
                ));
            }

            writer.write(sb.toString());
            AlertUtil.showInfo("Sucesso", "Relatório CSV (Filtrado) exportado com sucesso para:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            AlertUtil.showError("Erro ao Exportar", "Não foi possível gerar o arquivo CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ATUALIZADO: Removido. A lógica foi movida para carregarDadosMestres().
     */
    // private void atualizarResumoEstoque() { ... }


    @FXML
    private void handleAdicionarItem() { // ATUALIZADO: Nome do método
        // ATUALIZADO: Retorna Pair<CompraInfo, Boolean>
        Dialog<Pair<CompraInfo, Boolean>> dialog = new Dialog<>(); 
        dialog.setTitle("Adicionar Item ao Estoque"); // Texto ATUALIZADO
        dialog.setHeaderText("Preencha os dados da entrada.\nSe o item já existir, os valores serão somados (custo médio).");
        dialog.setResizable(true); // NOVO: Permite redimensionar

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

        // NOVOS CAMPOS
        TextField fornecedorNomeField = new TextField();
        fornecedorNomeField.setPromptText("Ex: João da Silva");
        TextField fornecedorEmpresaField = new TextField();
        fornecedorEmpresaField.setPromptText("Ex: Agropecuária XYZ");


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
        
        // NOVOS LABELS E FIELDS
        grid.add(new Label("Fornecedor (Nome):"), 0, 5);
        grid.add(fornecedorNomeField, 1, 5);
        grid.add(new Label("Fornecedor (Empresa):"), 0, 6);
        grid.add(fornecedorEmpresaField, 1, 6);
        
        
        // --- INÍCIO DA MELHORIA DE USABILIDADE ---
        CheckBox registrarFinanceiroCheck = new CheckBox("Registrar no financeiro (como compra)?");
        registrarFinanceiroCheck.setSelected(true);
        
        Label tipoPagLabel = new Label("Tipo de Pagamento:");
        ComboBox<String> tipoPagCombo = new ComboBox<>(
                FXCollections.observableArrayList("À Vista", "A Prazo")
        );
        tipoPagCombo.getSelectionModel().select("À Vista");

        Label vencimentoLabel = new Label("Data Vencimento:");
        DatePicker vencimentoPicker = new DatePicker(LocalDate.now().plusDays(30));

        grid.add(registrarFinanceiroCheck, 0, 7, 2, 1); // Checkbox (índice atualizado)
        grid.add(tipoPagLabel, 0, 8); // Linha movida
        grid.add(tipoPagCombo, 1, 8); // Linha movida
        grid.add(vencimentoLabel, 0, 9); // Linha movida
        grid.add(vencimentoPicker, 1, 9); // Linha movida
        // --- FIM DA MELHORIA DE USABILIDADE ---


        // --- Lógica de Cálculo Automático ---
        qtdField.textProperty().addListener((obs, oldV, newV) -> calcularTotal(qtdField, valorUnitarioField, valorTotalField));
        valorUnitarioField.textProperty().addListener((obs, oldV, newV) -> calcularTotal(qtdField, valorUnitarioField, valorTotalField));
        valorTotalField.textProperty().addListener((obs, oldV, newV) -> calcularUnitario(qtdField, valorUnitarioField, valorTotalField));
        
        // --- Lógica de Visibilidade ---
        vencimentoLabel.setVisible(false);
        vencimentoPicker.setVisible(false);
        vencimentoLabel.setManaged(false);
        vencimentoPicker.setManaged(false);

        // Esconde campos de pagamento se o financeiro não for registrado
        registrarFinanceiroCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            boolean registrar = newVal;
            tipoPagLabel.setVisible(registrar);
            tipoPagCombo.setVisible(registrar);
            tipoPagLabel.setManaged(registrar);
            tipoPagCombo.setManaged(registrar);

            // Só mostra vencimento se for A Prazo E for registrar
            boolean aPrazo = tipoPagCombo.getSelectionModel().getSelectedItem().equals("A Prazo");
            vencimentoLabel.setVisible(registrar && aPrazo);
            vencimentoPicker.setVisible(registrar && aPrazo);
            vencimentoLabel.setManaged(registrar && aPrazo);
            vencimentoPicker.setManaged(registrar && aPrazo);
        });

        // NOVO: Coloca o GridPane dentro de um ScrollPane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(grid);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        scrollPane.setPrefHeight(450); // Define uma altura preferencial

        dialog.getDialogPane().setContent(scrollPane); // ATUALIZADO: Define o ScrollPane
        AlertUtil.setDialogIcon(dialog); // CORREÇÃO: Adiciona o ícone
        
        // --- Validação em Tempo Real (similar ao PatrimonioController) ---
        Node adicionarButtonNode = dialog.getDialogPane().lookupButton(adicionarButtonType);
        adicionarButtonNode.setDisable(true); // Começa desabilitado

        Runnable validadorEstoque = () -> {
            boolean nomeOk = !nomeField.getText().trim().isEmpty();
            boolean unidadeOk = !unidadeField.getText().trim().isEmpty();
            boolean qtdOk = false;
            boolean valorTotalOk = false;
            boolean registrar = registrarFinanceiroCheck.isSelected();

            try {
                double qtd = parseDouble(qtdField.getText());
                qtdOk = qtd > 0; // Quantidade deve ser sempre positiva
            } catch (NumberFormatException e) {
                qtdOk = false;
            }
            
            try {
                double valorTotal = parseDouble(valorTotalField.getText());
                // Se for registrar, valor > 0. Se for ajuste, valor >= 0 (permite 0).
                valorTotalOk = registrar ? valorTotal > 0 : valorTotal >= 0;
            } catch (NumberFormatException e) {
                 valorTotalOk = false;
            }

            adicionarButtonNode.setDisable(!nomeOk || !unidadeOk || !qtdOk || !valorTotalOk);
        };
        
        nomeField.textProperty().addListener((obs, o, n) -> validadorEstoque.run());
        unidadeField.textProperty().addListener((obs, o, n) -> validadorEstoque.run());
        qtdField.textProperty().addListener((obs, o, n) -> validadorEstoque.run());
        valorTotalField.textProperty().addListener((obs, o, n) -> validadorEstoque.run());
        valorUnitarioField.textProperty().addListener((obs, o, n) -> validadorEstoque.run());
        registrarFinanceiroCheck.selectedProperty().addListener((obs, o, n) -> validadorEstoque.run());
        validadorEstoque.run(); // Validação inicial
        // --- Fim da Validação ---

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    String unidade = unidadeField.getText();
                    double qtd = parseDouble(qtdField.getText());
                    double valorUnitario = parseDouble(valorUnitarioField.getText());
                    double valorTotal = parseDouble(valorTotalField.getText());
                    String fornecedorNome = fornecedorNomeField.getText(); // NOVO
                    String fornecedorEmpresa = fornecedorEmpresaField.getText(); // NOVO
                    boolean deveRegistrar = registrarFinanceiroCheck.isSelected();

                    // Validações (já cobertas pelos listeners, mas mantidas por segurança)
                    if (nome.isEmpty() || unidade.isEmpty() || qtd <= 0) {
                         AlertUtil.showError("Erro de Validação", "Nome, Unidade e Quantidade (> 0) são obrigatórios.");
                         return null; // Retorna nulo para não fechar o diálogo
                    }
                    if (deveRegistrar && valorTotal <= 0) {
                         AlertUtil.showError("Erro de Validação", "O Valor Total deve ser positivo ao registrar no financeiro.");
                         return null;
                    }
                    if (!deveRegistrar && valorTotal < 0) {
                        AlertUtil.showError("Erro de Validação", "O Valor Total não pode ser negativo.");
                        return null;
                    }
                    
                    EstoqueItem item = new EstoqueItem(nome, qtd, unidade, valorUnitario, valorTotal, fornecedorNome, fornecedorEmpresa);

                    String tipoPagamento = tipoPagCombo.getSelectionModel().getSelectedItem();
                    LocalDate dataVencimento = vencimentoPicker.getValue();

                    if (deveRegistrar && tipoPagamento.equals("A Prazo") && dataVencimento == null) {
                        AlertUtil.showError("Erro de Validação", "A Data de Vencimento é obrigatória para compras 'A Prazo'.");
                        return null;
                    }

                    return new Pair<>(new CompraInfo(item, tipoPagamento, dataVencimento), deveRegistrar);

                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valores de quantidade ou R$ inválidos.");
                    return null;
                }
            }
            return null;
        });

        // ATUALIZADO: Espera um Pair
        Optional<Pair<CompraInfo, Boolean>> result = dialog.showAndWait(); 

        // ATUALIZADO: Processa o Pair
        result.ifPresent(pair -> {
            try {
                CompraInfo compraInfo = pair.getKey();
                boolean deveRegistrar = pair.getValue();

                // 1. Adiciona ao estoque (SEMPRE)
                estoqueDAO.addEstoque(compraInfo.item);

                // 2. Lógica financeira (CONDICIONAL)
                if (deveRegistrar) {
                    if (compraInfo.tipoPagamento.equals("À Vista")) {
                        String data = LocalDate.now().format(dateFormatter);
                        String desc = "Compra (à vista): " + compraInfo.item.getItemNome();
                        double valor = -compraInfo.item.getValorTotal(); 
                        
                        Transacao transacao = new Transacao(desc, valor, data, "despesa");
                        financeiroDAO.addTransacao(transacao);
                        
                        AlertUtil.showInfo("Sucesso", "Item comprado (à vista) e despesa registrada no financeiro.");

                    } else { // A Prazo
                        String desc = "Compra (a prazo): " + compraInfo.item.getItemNome();
                        Conta conta = new Conta(
                            desc,
                            compraInfo.item.getValorTotal(), 
                            compraInfo.dataVencimento.toString(),
                            "pagar", 
                            "pendente",
                            compraInfo.item.getFornecedorNome(), // NOVO
                            compraInfo.item.getFornecedorEmpresa() // NOVO
                        );
                        contaDAO.addConta(conta); 
                        AlertUtil.showInfo("Sucesso", "Item comprado (a prazo) e 'Conta a Pagar' registrada com sucesso.");
                    }
                } else {
                    // Mensagem para ajuste de estoque
                    AlertUtil.showInfo("Sucesso", "Item adicionado/atualizado no estoque (ajuste manual).");
                }

                // Recarrega os dados (assíncrono), independentemente do tipo
                carregarDadosMestres(); 

            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível registrar a entrada: " + e.getMessage());
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
        return String.format(Locale.US, "%.2f", value);
    }

    // Calcula Vlr. Total
    private void calcularTotal(TextField qtdField, TextField valorUnitarioField, TextField valorTotalField) {
        if (isUpdating) return; 
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
        if (isUpdating) return; 
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


    @FXML
    private void handleVenderItem() {
        EstoqueItem selecionado = tabelaEstoque.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um item na tabela para vender.");
            return;
        }

        Dialog<VendaInfo> dialog = new Dialog<>(); 
        dialog.setTitle("Vender Item do Estoque");
        dialog.setHeaderText("Item: " + selecionado.getItemNome() + " (Disponível: " + selecionado.getQuantidade() + " " + selecionado.getUnidade() + ")");
        dialog.setResizable(true); // NOVO: Permite redimensionar

        ButtonType venderButtonType = new ButtonType("Vender", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(venderButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField qtdField = new TextField("1.0");
        Button maxButton = new Button("MAX");
        maxButton.setOnAction(e -> {
            qtdField.setText(String.format(Locale.US, "%.2f", selecionado.getQuantidade()));
        });
        HBox qtdBox = new HBox(5, qtdField, maxButton); 
        qtdBox.setAlignment(Pos.CENTER_LEFT);

        TextField precoVendaField = new TextField(String.format(Locale.US, "%.2f", selecionado.getValorUnitario()));
        Label valorTotalVendaLabel = new Label("Total da Venda: R$ 0,00");

        Label tipoPagLabel = new Label("Recebimento:");
        ComboBox<String> tipoPagCombo = new ComboBox<>(
                FXCollections.observableArrayList("À Vista", "A Prazo")
        );
        tipoPagCombo.getSelectionModel().select("À Vista");

        Label vencimentoLabel = new Label("Data Recebimento:");
        DatePicker vencimentoPicker = new DatePicker(LocalDate.now().plusDays(30));
        
        // --- NOVOS CAMPOS PARA CLIENTE ---
        TextField clienteNomeField = new TextField();
        clienteNomeField.setPromptText("Ex: João da Silva (Opcional)");
        TextField clienteEmpresaField = new TextField();
        clienteEmpresaField.setPromptText("Ex: Fazenda Bela Vista (Opcional)");
        // --- FIM NOVOS CAMPOS ---

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

        grid.add(new Label("Quantidade a Vender:"), 0, 0);
        grid.add(qtdBox, 1, 0); 
        grid.add(new Label("Preço de Venda (unitário):"), 0, 1);
        grid.add(precoVendaField, 1, 1);
        grid.add(tipoPagLabel, 0, 2);
        grid.add(tipoPagCombo, 1, 2);
        grid.add(vencimentoLabel, 0, 3);
        grid.add(vencimentoPicker, 1, 3);
        
        // --- ADICIONA NOVOS CAMPOS AO GRID ---
        grid.add(new Label("Cliente (Nome):"), 0, 4);
        grid.add(clienteNomeField, 1, 4);
        grid.add(new Label("Cliente (Empresa):"), 0, 5);
        grid.add(clienteEmpresaField, 1, 5);
        // --- FIM ADIÇÃO ---
        
        grid.add(valorTotalVendaLabel, 1, 6); // Índice atualizado

        // --- VALIDAÇÃO EM TEMPO REAL ---
        Node venderButtonNode = dialog.getDialogPane().lookupButton(venderButtonType);
        venderButtonNode.setDisable(true); // Começa desabilitado

        Runnable validadorVenda = () -> {
            boolean qtdOk = false;
            boolean precoOk = false;
            boolean dataVencOk = true; // Assume OK

            try {
                double qtd = parseDouble(qtdField.getText());
                qtdOk = qtd > 0 && qtd <= selecionado.getQuantidade();
            } catch (NumberFormatException e) {
                qtdOk = false;
            }
            
            try {
                double preco = parseDouble(precoVendaField.getText());
                precoOk = preco >= 0; // Preço pode ser zero (doação/brinde)
            } catch (NumberFormatException e) {
                precoOk = false;
            }

            if (tipoPagCombo.getSelectionModel().getSelectedItem().equals("A Prazo")) {
                dataVencOk = vencimentoPicker.getValue() != null;
            }
            
            venderButtonNode.setDisable(!qtdOk || !precoOk || !dataVencOk);
            
            // Atualiza o Label de total (movido para dentro do listener)
             try {
                double qtd = parseDouble(qtdField.getText());
                double preco = parseDouble(precoVendaField.getText());
                valorTotalVendaLabel.setText(String.format(Locale.US, "Total da Venda: R$ %.2f", qtd * preco));
            } catch (NumberFormatException e) {
                valorTotalVendaLabel.setText("Total da Venda: R$ ---");
            }
        };

        qtdField.textProperty().addListener((obs, o, n) -> validadorVenda.run());
        precoVendaField.textProperty().addListener((obs, o, n) -> validadorVenda.run());
        tipoPagCombo.valueProperty().addListener((obs, o, n) -> validadorVenda.run());
        vencimentoPicker.valueProperty().addListener((obs, o, n) -> validadorVenda.run());
        validadorVenda.run(); // Executa validação inicial
        // --- FIM DA VALIDAÇÃO ---

        // NOVO: Coloca o GridPane dentro de um ScrollPane para melhor usabilidade
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(grid);
        scrollPane.setFitToWidth(true);
        // Remove fundo e borda do scrollpane para integrar com o diálogo
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        // Define uma altura preferencial para o scrollpane
        scrollPane.setPrefHeight(450); 

        dialog.getDialogPane().setContent(scrollPane); // ATUALIZADO: Define o ScrollPane como conteúdo
        AlertUtil.setDialogIcon(dialog); 

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == venderButtonType) {
                try {
                    double qtd = parseDouble(qtdField.getText());
                    double preco = parseDouble(precoVendaField.getText());
                    String tipoRecebimento = tipoPagCombo.getSelectionModel().getSelectedItem(); 
                    LocalDate dataVencimento = vencimentoPicker.getValue(); 
                    String clienteNome = clienteNomeField.getText(); // NOVO
                    String clienteEmpresa = clienteEmpresaField.getText(); // NOVO

                    // Validações (já cobertas pelo listener, mas mantidas por segurança)
                    if (qtd <= 0 || preco < 0) {
                        AlertUtil.showError("Valor Inválido", "Quantidade deve ser positiva e preço não pode ser negativo.");
                        return null;
                    }
                    if (qtd > selecionado.getQuantidade()) {
                         AlertUtil.showError("Estoque Insuficiente", "Disponível: " + selecionado.getQuantidade() + ". Solicitado: " + qtd);
                         return null;
                    }
                    if (tipoRecebimento.equals("A Prazo") && dataVencimento == null) {
                        AlertUtil.showError("Erro de Validação", "A Data de Recebimento é obrigatória para vendas 'A Prazo'.");
                        return null;
                    }
                    
                    return new VendaInfo(qtd, preco, tipoRecebimento, dataVencimento, clienteNome, clienteEmpresa); 
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valores de quantidade ou R$ inválidos.");
                    return null;
                }
            }
            return null;
        });

        Optional<VendaInfo> result = dialog.showAndWait(); 

        result.ifPresent(vendaInfo -> { 
            try {
                // Operação de escrita (rápida)
                estoqueDAO.consumirEstoque(selecionado.getId(), vendaInfo.qtdAVender);

                double valorReceita = vendaInfo.qtdAVender * vendaInfo.precoVendaUnitario;
                
                // DESCRIÇÃO ATUALIZADA
                String desc = "Venda de " + selecionado.getItemNome();
                // Usa os prefixos consistentes com o resto do app
                if (vendaInfo.clienteNome != null && !vendaInfo.clienteNome.isEmpty()) {
                    desc += " (Fornec: " + vendaInfo.clienteNome + ")";
                }
                if (vendaInfo.clienteEmpresa != null && !vendaInfo.clienteEmpresa.isEmpty()) {
                    desc += " (Empresa: " + vendaInfo.clienteEmpresa + ")";
                }


                if (vendaInfo.tipoRecebimento.equals("À Vista")) {
                    String data = LocalDate.now().format(dateFormatter);
                    Transacao transacao = new Transacao(desc, valorReceita, data, "receita"); 
                    financeiroDAO.addTransacao(transacao);
                    AlertUtil.showInfo("Sucesso", "Venda (à vista) registrada. Estoque atualizado e receita lançada.");

                } else {
                    // CONTA ATUALIZADA
                    Conta conta = new Conta(
                        desc,
                        valorReceita, 
                        vendaInfo.dataVencimento.toString(),
                        "receber", 
                        "pendente",
                        vendaInfo.clienteNome, // Passa o nome do cliente
                        vendaInfo.clienteEmpresa // Passa a empresa do cliente
                    );
                    contaDAO.addConta(conta);
                    AlertUtil.showInfo("Sucesso", "Venda (a prazo) registrada. Estoque atualizado e 'Conta a Receber' criada.");
                }

                carregarDadosMestres(); // Recarrega (assíncrono)

            } catch (IllegalStateException e) {
                AlertUtil.showError("Erro de Estoque", e.getMessage());
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível registrar a venda: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleConsumirItem() {
        EstoqueItem selecionado = tabelaEstoque.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um item na tabela para consumir.");
            return;
        }

        String qtdAtualStr = String.format(Locale.US, "%.2f", selecionado.getQuantidade());

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
        AlertUtil.setDialogIcon(dialog); // CORREÇÃO: Adiciona o ícone

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
                // String descricaoUso = pair.getValue(); // Não usado mais para despesa

                // Operação de escrita (rápida)
                estoqueDAO.consumirEstoque(selecionado.getId(), qtdAConsumir);
                
                AlertUtil.showInfo("Sucesso", "Estoque atualizado com sucesso.");
                
                carregarDadosMestres(); // Recarrega (assíncrono)

            } catch (IllegalStateException e) {
                AlertUtil.showError("Erro de Estoque", e.getMessage());
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível consumir o item: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleEditarItem() {
        EstoqueItem selecionado = tabelaEstoque.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um item na tabela para editar.");
            return;
        }

        Dialog<EstoqueItem> dialog = new Dialog<>(); // ATUALIZADO
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
        TextField fornecedorNomeField = new TextField(selecionado.getFornecedorNome()); // NOVO
        TextField fornecedorEmpresaField = new TextField(selecionado.getFornecedorEmpresa()); // NOVO

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Unidade:"), 0, 1);
        grid.add(unidadeField, 1, 1);
        grid.add(new Label("Fornecedor (Nome):"), 0, 2); // NOVO
        grid.add(fornecedorNomeField, 1, 2); // NOVO
        grid.add(new Label("Fornecedor (Empresa):"), 0, 3); // NOVO
        grid.add(fornecedorEmpresaField, 1, 3); // NOVO

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // CORREÇÃO: Adiciona o ícone

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == salvarButtonType) {
                String nome = nomeField.getText().trim();
                String unidade = unidadeField.getText().trim();
                String fornNome = fornecedorNomeField.getText().trim(); // NOVO
                String fornEmpresa = fornecedorEmpresaField.getText().trim(); // NOVO

                if (nome.isEmpty() || unidade.isEmpty()) {
                    AlertUtil.showError("Erro de Validação", "Nome e Unidade são obrigatórios.");
                    return null;
                }
                // Retorna um novo objeto EstoqueItem apenas com os dados atualizados
                // O ID será pego do 'selecionado'
                return new EstoqueItem(nome, 0, unidade, 0, 0, fornNome, fornEmpresa);
            }
            return null;
        });

        Optional<EstoqueItem> result = dialog.showAndWait(); // ATUALIZADO

        result.ifPresent(itemEditado -> { // ATUALIZADO
            try {
                // Operação de escrita (rápida)
                estoqueDAO.updateEstoqueItem(
                    selecionado.getId(), 
                    itemEditado.getItemNome(), 
                    itemEditado.getUnidade(),
                    itemEditado.getFornecedorNome(), // NOVO
                    itemEditado.getFornecedorEmpresa() // NOVO
                );
                
                AlertUtil.showInfo("Sucesso", "Item atualizado com sucesso.");
                carregarDadosMestres(); // Recarrega (assíncrono)

            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar o item: " + e.getMessage());
            }
        });
    }


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
                // Operação de escrita (rápida)
                if (estoqueDAO.removerItemEstoque(selecionado.getId())) {
                    AlertUtil.showInfo("Removido", "Item removido do estoque com sucesso.");
                    carregarDadosMestres(); // Recarrega (assíncrono)
                } else {
                    AlertUtil.showError("Erro ao Remover", "O item não pôde ser removido.");
                }
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover o item: " + e.getMessage());
            }
        }
    }
    
    // Classe interna - CompraInfo
    private static class CompraInfo {
        final EstoqueItem item;
        final String tipoPagamento; 
        final LocalDate dataVencimento; 

        CompraInfo(EstoqueItem item, String tipo, LocalDate data) {
            this.item = item;
            this.tipoPagamento = tipo;
            this.dataVencimento = data;
        }
    }
    
    // Classe interna - VendaInfo
    // ATUALIZADA para incluir dados do cliente
    private static class VendaInfo {
        final double qtdAVender;
        final double precoVendaUnitario;
        final String tipoRecebimento; 
        final LocalDate dataVencimento; 
        final String clienteNome; // NOVO
        final String clienteEmpresa; // NOVO

        VendaInfo(double qtd, double preco, String tipo, LocalDate data, String clienteNome, String clienteEmpresa) {
            this.qtdAVender = qtd;
            this.precoVendaUnitario = preco;
            this.tipoRecebimento = tipo;
            this.dataVencimento = data;
            this.clienteNome = clienteNome; // NOVO
            this.clienteEmpresa = clienteEmpresa; // NOVO
        }
    }
}