package com.farmmanager.controller;

// DAOs (ATUALIZADO - Novos DAOs)
import com.farmmanager.model.SafraDAO;
import com.farmmanager.model.TalhaoDAO;
import com.farmmanager.model.AtividadeSafraDAO;
import com.farmmanager.model.ContaDAO;
import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.model.FinanceiroDAO;

// Modelos (ATUALIZADO - Novos Modelos)
import com.farmmanager.model.SafraInfo;
import com.farmmanager.model.Talhao;
import com.farmmanager.model.Conta;
import com.farmmanager.model.EstoqueItem;
import com.farmmanager.model.Transacao;

import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task; // NOVO: Import para Task
import javafx.fxml.FXML;
// import javafx.scene.chart.BarChart; // ATUALIZADO (REMOVIDO)
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart; // ATUALIZADO (RE-ADICIONADO)
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker; 
import javafx.scene.control.Label; // NOVO: Import
import javafx.scene.control.ListCell;
import javafx.scene.control.ProgressIndicator; // NOVO: Import
import javafx.scene.control.ScrollPane; // NOVO: Import
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell; 
import javafx.geometry.Pos; 
import javafx.stage.FileChooser; // NOVO: Import para FileChooser
import java.io.File; // NOVO: Import para File
import java.io.IOException; // NOVO: Import para IOException
import java.io.PrintWriter; // NOVO: Import para PrintWriter

import java.sql.SQLException;
import java.text.DecimalFormat; 
import java.text.NumberFormat; 
import java.time.LocalDate; 
import java.time.LocalDateTime; 
import java.time.format.DateTimeFormatter; 
import java.util.ArrayList; // NOVO
import java.util.List;
import java.util.Locale; 
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NOVO: Controller para a tela de Histórico e Relatórios de Safras.
 * Esta classe foca em analisar dados de safras *colhidas*.
 * ATUALIZADO: Adicionado filtro de data.
 * ATUALIZADO (initialize): Adicionada formatação decimal para coluna sc/ha.
 * - MELHORIA CRÍTICA: Carregamento de dados (Safras e Talhões)
 * movido para uma Task em background para não congelar a UI.
 * - ATUALIZADO: Adicionada função de exportar CSV filtrado.
 * - ATUALIZAÇÃO (MELHORIA): Agora calcula e exibe Custo, Receita e Lucro.
 * - ATUALIZAÇÃO (MELHORIA): Adiciona KPIs de resumo financeiro.
 * - ATUALIZAÇÃO (MELHORIA): Gráfico de lucratividade REVERTIDO para produção.
 */
public class HistoricoSafrasController {

    // DAOs (ATUALIZADO - Novos DAOs)
    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO;
    private final AtividadeSafraDAO atividadeSafraDAO;
    private final FinanceiroDAO financeiroDAO;
    private final ContaDAO contaDAO;
    private final EstoqueDAO estoqueDAO;


    // Listas de Dados (ATUALIZADO - Usando novo DTO)
    private List<SafraHistoricoInfo> listaMestraSafrasComInfo; // Lista completa (colhidas e ativas)
    private final ObservableList<SafraHistoricoInfo> dadosTabelaHistorico;
    // ATUALIZADO: Revertido para PieChart.Data
    private final ObservableList<PieChart.Data> dadosChartCultura;
    private final ObservableList<XYChart.Series<String, Number>> dadosChartProducaoMedia;
    
    private final DateTimeFormatter dbTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final NumberFormat decimalFormatter;
    private final NumberFormat currencyFormatter; // NOVO

    // Componentes FXML
    @FXML
    private ComboBox<String> filtroCultura;
    @FXML
    private ComboBox<Talhao> filtroTalhao;
    @FXML
    private Button btnLimparFiltro; 
    @FXML
    private Button btnExportarCsv; // NOVO: FXML para o botão
    @FXML
    private DatePicker filtroDataInicio; 
    @FXML
    private DatePicker filtroDataFim; 

    // KPIs (NOVOS)
    @FXML
    private Label lblProdTotalPeriodo;
    @FXML
    private Label lblProdMediaPeriodo;
    @FXML
    private Label lblCustoTotalPeriodo;
    @FXML
    private Label lblReceitaTotalPeriodo;
    @FXML
    private Label lblLucroTotalPeriodo;


    // Gráficos
    @FXML
    private LineChart<String, Number> chartProducaoMedia;
    @FXML
    private PieChart chartProducaoCultura; // ATUALIZADO (Era BarChart)

    // Tabela (ATUALIZADO - Tabela e Colunas)
    @FXML
    private TableView<SafraHistoricoInfo> tabelaHistorico;
    @FXML
    private TableColumn<SafraHistoricoInfo, Integer> colId;
    @FXML
    private TableColumn<SafraHistoricoInfo, String> colSafraAno;
    @FXML
    private TableColumn<SafraHistoricoInfo, String> colCultura;
    @FXML
    private TableColumn<SafraHistoricoInfo, String> colTalhao;
    @FXML
    private TableColumn<SafraHistoricoInfo, Double> colArea;
    @FXML
    private TableColumn<SafraHistoricoInfo, String> colDataColheita; 
    @FXML
    private TableColumn<SafraHistoricoInfo, Double> colProdSacos;
    @FXML
    private TableColumn<SafraHistoricoInfo, Double> colProdScHa;
    // Novas Colunas
    @FXML
    private TableColumn<SafraHistoricoInfo, Double> colCustoTotal;
    @FXML
    private TableColumn<SafraHistoricoInfo, Double> colReceitaTotal;
    @FXML
    private TableColumn<SafraHistoricoInfo, Double> colLucro;
    
    // NOVO: Componentes para controle de carregamento
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private ScrollPane contentScrollPane; // Container principal (ScrollPane do FXML)


    /**
     * NOVO: Classe interna (DTO) para agrupar todos os dados necessários
     * para a tabela e gráficos de histórico.
     */
    public static class SafraHistoricoInfo {
        private final SafraInfo safraBase;
        private final double custoTotal;
        private final double receitaTotal; // Vendas (à vista + a prazo)
        private final double valorEmEstoque; // Produto não vendido
        private final double lucro;

        public SafraHistoricoInfo(SafraInfo safraBase, double custoTotal, double receitaTotal, double valorEmEstoque, double lucro) {
            this.safraBase = safraBase;
            this.custoTotal = custoTotal;
            this.receitaTotal = receitaTotal;
            this.valorEmEstoque = valorEmEstoque;
            this.lucro = lucro;
        }

        // Getters da SafraBase (para colunas existentes)
        public int getId() { return safraBase.getId(); }
        public String getAnoInicio() { return safraBase.getAnoInicio(); }
        public String getCultura() { return safraBase.getCultura(); }
        public String getTalhaoNome() { return safraBase.getTalhaoNome(); }
        public double getAreaHectares() { return safraBase.getAreaHectares(); }
        public String getDataModificacao() { return safraBase.getDataModificacao(); } // Data Colheita
        public double getProducaoTotalSacos() { return safraBase.getProducaoTotalSacos(); }
        public double getProducaoSacosPorHectare() { return safraBase.getProducaoSacosPorHectare(); }
        public String getStatus() { return safraBase.getStatus(); }

        // Getters dos novos dados financeiros
        public double getCustoTotal() { return custoTotal; }
        public double getReceitaTotal() { return receitaTotal; }
        public double getValorEmEstoque() { return valorEmEstoque; }
        public double getLucro() { return lucro; }
    }


    /**
     * NOVO: Classe interna para agrupar os resultados da Task
     * ATUALIZADO: Retorna a lista do novo DTO
     */
    private static class HistoricoPageData {
        final List<SafraHistoricoInfo> safrasComFinanceiro;
        final List<Talhao> talhoes;

        HistoricoPageData(List<SafraHistoricoInfo> safras, List<Talhao> talhoes) {
            this.safrasComFinanceiro = safras;
            this.talhoes = talhoes;
        }
    }

    public HistoricoSafrasController() {
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO();
        // NOVOS DAOs
        atividadeSafraDAO = new AtividadeSafraDAO();
        financeiroDAO = new FinanceiroDAO();
        contaDAO = new ContaDAO();
        estoqueDAO = new EstoqueDAO();
        
        listaMestraSafrasComInfo = FXCollections.observableArrayList();
        dadosTabelaHistorico = FXCollections.observableArrayList();
        dadosChartCultura = FXCollections.observableArrayList(); // ATUALIZADO
        dadosChartProducaoMedia = FXCollections.observableArrayList();
        
        decimalFormatter = new DecimalFormat("#,##0.00");
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")); // NOVO
    }

    @FXML
    public void initialize() {
        // 1. Configurar Tabela (ATUALIZADO)
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSafraAno.setCellValueFactory(new PropertyValueFactory<>("anoInicio"));
        colCultura.setCellValueFactory(new PropertyValueFactory<>("cultura"));
        colTalhao.setCellValueFactory(new PropertyValueFactory<>("talhaoNome"));
        colArea.setCellValueFactory(new PropertyValueFactory<>("areaHectares"));
        colDataColheita.setCellValueFactory(new PropertyValueFactory<>("dataModificacao")); 
        colProdSacos.setCellValueFactory(new PropertyValueFactory<>("producaoTotalSacos"));
        colProdScHa.setCellValueFactory(new PropertyValueFactory<>("producaoSacosPorHectare"));
        // Novas colunas financeiras
        colCustoTotal.setCellValueFactory(new PropertyValueFactory<>("custoTotal"));
        colReceitaTotal.setCellValueFactory(new PropertyValueFactory<>("receitaTotal"));
        colLucro.setCellValueFactory(new PropertyValueFactory<>("lucro"));
        
        tabelaHistorico.setItems(dadosTabelaHistorico);

        // Formatação de células
        formatarColunaDecimal(colProdScHa);
        formatarColunaCurrency(colCustoTotal, "negativo"); // Custos são negativos
        formatarColunaCurrency(colReceitaTotal, "positivo"); // Receitas são positivas
        formatarColunaCurrency(colLucro, "balanco"); // Lucro pode ser pos/neg


        // 2. Configurar Gráficos (ATUALIZADO)
        chartProducaoCultura.setData(dadosChartCultura); // ATUALIZADO
        chartProducaoMedia.setData(dadosChartProducaoMedia);

        // 3. Carregar Dados (Agora assíncrono)
        carregarDadosPaginaAssincrono();
        
        // Listeners para os filtros (rápidos, rodam na FX Thread)
        filtroDataInicio.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroDataFim.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        // Listeners dos ComboBoxes são adicionados em popularFiltros()
    }

    /**
     * NOVO: Controla a visibilidade do indicador de carregamento.
     */
    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisible(isLoading);
        loadingIndicator.setManaged(isLoading);
        
        contentScrollPane.setDisable(isLoading);
        contentScrollPane.setOpacity(isLoading ? 0.5 : 1.0); 
    }

    /**
     * NOVO: Carrega os dados mestres (Safras e Talhões) E CALCULA
     * OS DADOS FINANCEIROS de cada safra colhida, tudo em uma Task.
     */
    private void carregarDadosPaginaAssincrono() {
        Task<HistoricoPageData> carregarTask = new Task<HistoricoPageData>() {
            @Override
            protected HistoricoPageData call() throws Exception {
                // Chamadas de banco de dados (demoradas)
                List<SafraInfo> safrasBase = safraDAO.listSafrasComInfo();
                List<Talhao> talhoes = talhaoDAO.listTalhoes();
                
                List<SafraHistoricoInfo> safrasComFinanceiro = new ArrayList<>();

                // Loop para calcular finanças de cada safra colhida (parte lenta)
                for (SafraInfo safra : safrasBase) {
                    if (safra.getStatus().equalsIgnoreCase("Colhida")) {
                        double custo = atividadeSafraDAO.getCustoTotalPorSafra(safra.getId());
                        double receita = calcularReceitaParaSafra(safra); // Helper
                        double estoqueValor = calcularValorEstoqueParaSafra(safra); // Helper
                        double lucro = (receita + estoqueValor) - custo;
                        
                        safrasComFinanceiro.add(new SafraHistoricoInfo(safra, custo, receita, estoqueValor, lucro));
                    }
                }
                
                return new HistoricoPageData(safrasComFinanceiro, talhoes);
            }
        };

        carregarTask.setOnSucceeded(e -> {
            HistoricoPageData data = carregarTask.getValue();

            // 1. Popula a lista mestra de safras (apenas colhidas com dados financeiros)
            listaMestraSafrasComInfo = data.safrasComFinanceiro;

            // 2. Popula os ComboBoxes de filtro (rápido, UI)
            // (Passa a lista DTO para extrair culturas)
            popularFiltros(data.talhoes, data.safrasComFinanceiro);
            
            // 3. Aplica os filtros (rápido, em memória)
            handleAplicarFiltro();
            
            // 4. Esconde o loading
            showLoading(false);
        });

        carregarTask.setOnFailed(e -> {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar o histórico de safras.");
            carregarTask.getException().printStackTrace();
            showLoading(false);
        });

        showLoading(true);
        new Thread(carregarTask).start();
    }
    
    // --- NOVOS Métodos Helper para cálculos financeiros (usados pela Task) ---

    private double calcularReceitaParaSafra(SafraInfo safra) throws SQLException {
        double receitaTotalVendas = 0;
        String nomeItemColheita = safra.getCultura() + " (Colheita " + safra.getAnoInicio() + ")";
        String descVendaQuery = "Venda de " + nomeItemColheita;
        
        // 1. Soma vendas À VISTA (do Financeiro)
        List<Transacao> vendasAVista = financeiroDAO.listTransacoesPorDescricaoLike(descVendaQuery);
        for (Transacao venda : vendasAVista) {
            receitaTotalVendas += venda.getValor(); // Valores já são positivos
        }

        // 2. Soma vendas A PRAZO (de Contas a Receber)
        List<Conta> vendasAPrazo = contaDAO.listContasPorDescricaoLike(descVendaQuery);
        for (Conta conta : vendasAPrazo) {
            if (conta.getTipo().equals("receber")) {
                receitaTotalVendas += conta.getValor();
            }
        }
        return receitaTotalVendas;
    }
    
    private double calcularValorEstoqueParaSafra(SafraInfo safra) throws SQLException {
        String nomeItemColheita = safra.getCultura() + " (Colheita " + safra.getAnoInicio() + ")";
        EstoqueItem itemColheitaEstoque = estoqueDAO.getEstoqueItemPorNome(nomeItemColheita);
        if (itemColheitaEstoque != null) {
            return itemColheitaEstoque.getValorTotal();
        }
        return 0.0;
    }
    
    // --- Fim dos Métodos Helper ---


    /**
     * ATUALIZADO: Popula os ComboBoxes usando a nova lista de DTOs.
     */
    private void popularFiltros(List<Talhao> talhoes, List<SafraHistoricoInfo> safras) {
        // Filtro de Talhão
        Talhao todosTalhoes = new Talhao(0, "Todos os Talhões", 0); 
        filtroTalhao.getItems().add(todosTalhoes);
        filtroTalhao.getItems().addAll(talhoes);
        filtroTalhao.getSelectionModel().select(todosTalhoes);
        
        filtroTalhao.setCellFactory(lv -> new ListCell<Talhao>() {
            @Override
            protected void updateItem(Talhao item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getNome());
            }
        });
        filtroTalhao.setButtonCell(new ListCell<Talhao>() {
            @Override
            protected void updateItem(Talhao item, boolean empty) {
                // --- CORREÇÃO DO ERRO 1 ---
                super.updateItem(item, empty); // Chamada correta
                setText(item == null || empty ? "" : item.getNome()); // Define o texto
                // --- FIM DA CORREÇÃO ---
            }
        });
        // Adiciona listener
        filtroTalhao.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());


        // Filtro de Cultura
        String todasCulturas = "Todas as Culturas";
        filtroCultura.getItems().add(todasCulturas);
        List<String> culturas = safras.stream() // Usa a lista 'safras' do parâmetro
                                        .map(SafraHistoricoInfo::getCultura)
                                        .distinct()
                                        .sorted()
                                        .collect(Collectors.toList());
        filtroCultura.getItems().addAll(culturas);
        filtroCultura.getSelectionModel().select(todasCulturas);
        // Adiciona listener
        filtroCultura.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
    }

    /**
     * NOVO: Limpa todos os filtros para os valores padrão.
     */
    @FXML
    private void handleLimparFiltro() {
        filtroCultura.getSelectionModel().select("Todas as Culturas");
        filtroTalhao.getSelectionModel().selectFirst(); // Seleciona o "Todos os Talhões"
        filtroDataInicio.setValue(null);
        filtroDataFim.setValue(null); 
        
        // handleAplicarFiltro() é chamado automaticamente pelos listeners
    }

    /**
     * ATUALIZADO: Filtra a listaMestraSafrasComInfo (em memória).
     * Não precisa mais filtrar por "Colhida", pois a lista mestra SÓ TEM colhidas.
     * ATUALIZADO: Agora também calcula e exibe os KPIs de resumo.
     */
    @FXML
    private void handleAplicarFiltro() {
        String culturaSel = filtroCultura.getSelectionModel().getSelectedItem();
        Talhao talhaoSel = filtroTalhao.getSelectionModel().getSelectedItem();
        LocalDate dataInicio = filtroDataInicio.getValue(); 
        LocalDate dataFim = filtroDataFim.getValue(); 

        // 1. Filtrar a lista mestra
        List<SafraHistoricoInfo> safrasFiltradas = listaMestraSafrasComInfo.stream()
            .filter(safra -> {
                // Filtro de Cultura
                boolean culturaMatch = (culturaSel == null || culturaSel.equals("Todas as Culturas") || safra.getCultura().equals(culturaSel));
                // Filtro de Talhão
                boolean talhaoMatch = (talhaoSel == null || talhaoSel.getId() == 0 || safra.getTalhaoNome().equals(talhaoSel.getNome()));
                
                // Filtro de Data (usa dataModificacao como data da colheita)
                boolean dataMatch = true;
                if (safra.getDataModificacao() != null) {
                    try {
                        LocalDate dataColheita = LocalDateTime.parse(safra.getDataModificacao(), dbTimestampFormatter).toLocalDate();
                        
                        if (dataInicio != null && dataColheita.isBefore(dataInicio)) {
                            dataMatch = false;
                        }
                        if (dataFim != null && dataColheita.isAfter(dataFim)) {
                            dataMatch = false;
                        }
                    } catch (Exception e) {
                        dataMatch = false; 
                    }
                } else {
                    if(dataInicio != null || dataFim != null) {
                        dataMatch = false;
                    }
                }

                return culturaMatch && talhaoMatch && dataMatch; 
            })
            .collect(Collectors.toList());

        // 2. Atualizar a Tabela
        dadosTabelaHistorico.setAll(safrasFiltradas);

        // 3. Atualizar os Gráficos
        atualizarChartProducaoMedia(safrasFiltradas);
        atualizarChartProducaoCultura(safrasFiltradas); // ATUALIZADO
        
        // 4. ATUALIZADO: Calcular e Atualizar KPIs
        atualizarKPIs(safrasFiltradas);
    }
    
    /**
     * NOVO: Calcula e exibe os KPIs de resumo para o período filtrado.
     */
    private void atualizarKPIs(List<SafraHistoricoInfo> safrasFiltradas) {
        double prodTotalSacos = 0;
        double prodMediaScHaSoma = 0;
        double receitaTotal = 0;
        double custoTotal = 0;
        double lucroTotal = 0;

        for (SafraHistoricoInfo safra : safrasFiltradas) {
            prodTotalSacos += safra.getProducaoTotalSacos();
            prodMediaScHaSoma += safra.getProducaoSacosPorHectare();
            receitaTotal += safra.getReceitaTotal(); // Receita de Vendas
            custoTotal += safra.getCustoTotal();
            lucroTotal += safra.getLucro(); // Lucro (Receita + Estoque - Custo)
        }
        
        double prodMediaScHa = safrasFiltradas.isEmpty() ? 0 : (prodMediaScHaSoma / safrasFiltradas.size());

        lblProdTotalPeriodo.setText(decimalFormatter.format(prodTotalSacos) + " sc");
        lblProdMediaPeriodo.setText(decimalFormatter.format(prodMediaScHa) + " sc/ha");
        lblReceitaTotalPeriodo.setText(currencyFormatter.format(receitaTotal));
        lblCustoTotalPeriodo.setText(currencyFormatter.format(custoTotal));
        lblLucroTotalPeriodo.setText(currencyFormatter.format(lucroTotal));
        
        // Atualiza cor do Lucro Total
        lblLucroTotalPeriodo.getStyleClass().removeAll("positivo-text", "negativo-text");
        if (lucroTotal > 0) {
            lblLucroTotalPeriodo.getStyleClass().add("positivo-text");
        } else if (lucroTotal < 0) {
            lblLucroTotalPeriodo.getStyleClass().add("negativo-text");
        }
    }

    
    /**
     * NOVO: Exporta os dados atualmente visíveis na tabela (filtrados) para um arquivo CSV.
     * ATUALIZADO: Inclui os novos dados financeiros.
     */
    @FXML
    private void handleExportarCsv() {
        if (dadosTabelaHistorico.isEmpty()) {
            AlertUtil.showInfo("Nada para Exportar", "A tabela está vazia. Não há dados para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Relatório de Histórico de Safras");
        fileChooser.setInitialFileName("Relatorio_Historico_Safras_Financeiro.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(tabelaHistorico.getScene().getWindow());

        if (file == null) {
            return; // Usuário cancelou
        }

        // Tenta escrever o arquivo
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            
            writer.write("\uFEFF"); // Adiciona o BOM do UTF-8 para Excel
            
            StringBuilder sb = new StringBuilder();
            
            // Cabeçalho do CSV (ATUALIZADO)
            sb.append("ID;Safra (Ano);Cultura;Talhão;Área (ha);Data Colheita;Produção Total (Sacos);Produtividade (sc/ha);");
            sb.append("Custo Total (R$);Receita Vendas (R$);Valor Estoque (R$);Lucro/Prejuízo (R$)\n");

            // Escreve os dados (usando a lista filtrada 'dadosTabelaHistorico')
            for (SafraHistoricoInfo safra : dadosTabelaHistorico) {
                sb.append(String.format(Locale.US, "%d;%s;\"%s\";\"%s\";%.2f;%s;%.2f;%.2f;%.2f;%.2f;%.2f;%.2f\n",
                    safra.getId(),
                    safra.getAnoInicio(),
                    safra.getCultura().replace("\"", "\"\""),
                    safra.getTalhaoNome().replace("\"", "\"\""),
                    safra.getAreaHectares(),
                    safra.getDataModificacao(), // Data da colheita
                    safra.getProducaoTotalSacos(),
                    safra.getProducaoSacosPorHectare(),
                    safra.getCustoTotal(),
                    safra.getReceitaTotal(),
                    safra.getValorEmEstoque(),
                    safra.getLucro()
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
     * Atualiza o gráfico de linha (Produção Média por Safra)
     */
    private void atualizarChartProducaoMedia(List<SafraHistoricoInfo> safras) {
        // Agrupa por Ano/Safra e calcula a média de sc/ha
        Map<String, Double> mediaPorSafra = safras.stream()
            .collect(Collectors.groupingBy(
                SafraHistoricoInfo::getAnoInicio,
                Collectors.averagingDouble(SafraHistoricoInfo::getProducaoSacosPorHectare)
            ));

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Média (sc/ha)");

        // Adiciona dados ao gráfico, ordenados por Ano/Safra
        mediaPorSafra.entrySet().stream()
            .sorted(Map.Entry.comparingByKey())
            .forEach(entry -> {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            });

        dadosChartProducaoMedia.setAll(series);
    }

    /**
     * ATUALIZADO: Atualiza o gráfico de pizza (Produção Total por Cultura)
     */
    private void atualizarChartProducaoCultura(List<SafraHistoricoInfo> safras) {
        // --- INÍCIO DA REVERSÃO ---
        
        // 1. Agrupa por Cultura e soma a produção total (em sacos)
        Map<String, Double> producaoPorCultura = safras.stream()
            .collect(Collectors.groupingBy(
                SafraHistoricoInfo::getCultura,
                Collectors.summingDouble(SafraHistoricoInfo::getProducaoTotalSacos)
            ));

        // 2. Limpa dados antigos
        dadosChartCultura.clear();
        
        // 3. Adiciona novos dados
        producaoPorCultura.entrySet().stream()
            .sorted(Map.Entry.comparingByValue()) // Ordena para melhor visualização
            .forEach(entry -> {
                String nome = String.format("%s (%.0f sc)", entry.getKey(), entry.getValue());
                dadosChartCultura.add(new PieChart.Data(nome, entry.getValue()));
            });
        
        // --- FIM DA REVERSÃO ---
    }
    
    // --- NOVOS Helpers de Formatação de Célula ---
    
    private void formatarColunaDecimal(TableColumn<SafraHistoricoInfo, Double> coluna) {
        coluna.setCellFactory(col -> new TableCell<SafraHistoricoInfo, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0.0) {
                    setText(null);
                    setAlignment(Pos.CENTER_RIGHT);
                } else {
                    setText(decimalFormatter.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
    }

    private void formatarColunaCurrency(TableColumn<SafraHistoricoInfo, Double> coluna, String tipo) {
        coluna.setCellFactory(col -> new TableCell<SafraHistoricoInfo, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("positivo-text", "negativo-text");
                
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                    
                    if (tipo.equals("balanco")) {
                        if (item > 0) {
                            getStyleClass().add("positivo-text");
                        } else if (item < 0) {
                            getStyleClass().add("negativo-text");
                        }
                    } else if (tipo.equals("positivo")) {
                         getStyleClass().add("positivo-text");
                    } else if (tipo.equals("negativo")) {
                         getStyleClass().add("negativo-text");
                    }
                }
            }
        });
    }
}