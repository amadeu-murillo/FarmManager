package com.farmmanager.controller;

import com.farmmanager.model.SafraDAO;
import com.farmmanager.model.SafraInfo;
import com.farmmanager.model.Talhao;
import com.farmmanager.model.TalhaoDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task; // NOVO: Import para Task
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
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

import java.sql.SQLException;
import java.text.DecimalFormat; 
import java.text.NumberFormat; 
import java.time.LocalDate; 
import java.time.LocalDateTime; 
import java.time.format.DateTimeFormatter; 
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
 */
public class HistoricoSafrasController {

    // DAOs
    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO;

    // Listas de Dados
    private List<SafraInfo> listaMestraSafrasComInfo; // Lista completa (colhidas e ativas)
    private final ObservableList<SafraInfo> dadosTabelaHistorico;
    private final ObservableList<PieChart.Data> dadosChartCultura;
    private final ObservableList<XYChart.Series<String, Number>> dadosChartProducaoMedia;
    
    private final DateTimeFormatter dbTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final NumberFormat decimalFormatter;

    // Componentes FXML
    @FXML
    private ComboBox<String> filtroCultura;
    @FXML
    private ComboBox<Talhao> filtroTalhao;
    @FXML
    private Button btnAplicarFiltro;
    @FXML
    private Button btnLimparFiltro; 
    @FXML
    private DatePicker filtroDataInicio; 
    @FXML
    private DatePicker filtroDataFim; 

    // Gráficos
    @FXML
    private LineChart<String, Number> chartProducaoMedia;
    @FXML
    private PieChart chartProducaoCultura;

    // Tabela
    @FXML
    private TableView<SafraInfo> tabelaHistorico;
    // ... (colunas tabela)
    @FXML
    private TableColumn<SafraInfo, Integer> colId;
    @FXML
    private TableColumn<SafraInfo, String> colSafraAno;
    @FXML
    private TableColumn<SafraInfo, String> colCultura;
    @FXML
    private TableColumn<SafraInfo, String> colTalhao;
    @FXML
    private TableColumn<SafraInfo, Double> colArea;
    @FXML
    private TableColumn<SafraInfo, String> colDataColheita; 
    @FXML
    private TableColumn<SafraInfo, Double> colProdSacos;
    @FXML
    private TableColumn<SafraInfo, Double> colProdScHa;
    
    // NOVO: Componentes para controle de carregamento
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private ScrollPane contentScrollPane; // Container principal (ScrollPane do FXML)


    /**
     * NOVO: Classe interna para agrupar os resultados da Task
     */
    private static class HistoricoPageData {
        final List<SafraInfo> safras;
        final List<Talhao> talhoes;

        HistoricoPageData(List<SafraInfo> safras, List<Talhao> talhoes) {
            this.safras = safras;
            this.talhoes = talhoes;
        }
    }

    public HistoricoSafrasController() {
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO();
        
        listaMestraSafrasComInfo = FXCollections.observableArrayList();
        dadosTabelaHistorico = FXCollections.observableArrayList();
        dadosChartCultura = FXCollections.observableArrayList();
        dadosChartProducaoMedia = FXCollections.observableArrayList();
        
        decimalFormatter = new DecimalFormat("#,##0.00");
    }

    @FXML
    public void initialize() {
        // 1. Configurar Tabela
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSafraAno.setCellValueFactory(new PropertyValueFactory<>("anoInicio"));
        colCultura.setCellValueFactory(new PropertyValueFactory<>("cultura"));
        colTalhao.setCellValueFactory(new PropertyValueFactory<>("talhaoNome"));
        colArea.setCellValueFactory(new PropertyValueFactory<>("areaHectares"));
        colDataColheita.setCellValueFactory(new PropertyValueFactory<>("dataModificacao")); 
        colProdSacos.setCellValueFactory(new PropertyValueFactory<>("producaoTotalSacos"));
        colProdScHa.setCellValueFactory(new PropertyValueFactory<>("producaoSacosPorHectare"));
        tabelaHistorico.setItems(dadosTabelaHistorico);

        // Formatação da coluna de produtividade (sc/ha)
        colProdScHa.setCellFactory(col -> new TableCell<SafraInfo, Double>() {
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

        // 2. Configurar Gráficos
        chartProducaoCultura.setData(dadosChartCultura);
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
     * NOVO: Carrega os dados mestres (Safras e Talhões) em uma Task.
     */
    private void carregarDadosPaginaAssincrono() {
        Task<HistoricoPageData> carregarTask = new Task<HistoricoPageData>() {
            @Override
            protected HistoricoPageData call() throws Exception {
                // Chamadas de banco de dados (demoradas)
                List<SafraInfo> safras = safraDAO.listSafrasComInfo();
                List<Talhao> talhoes = talhaoDAO.listTalhoes();
                return new HistoricoPageData(safras, talhoes);
            }
        };

        carregarTask.setOnSucceeded(e -> {
            HistoricoPageData data = carregarTask.getValue();

            // 1. Popula a lista mestra de safras (colhidas e ativas)
            listaMestraSafrasComInfo = data.safras;

            // 2. Popula os ComboBoxes de filtro (rápido, UI)
            popularFiltros(data.talhoes, data.safras);
            
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


    /**
     * ATUALIZADO: Este método foi removido pois sua lógica
     * foi incorporada em carregarDadosPaginaAssincrono().
     */
    // private void carregarDadosMestresEAtualizarTudo() { ... }


    /**
     * ATUALIZADO: Renomeado de carregarFiltros().
     * Agora popula os ComboBoxes usando dados (listas) já carregados,
     * sem fazer novas chamadas ao DAO.
     */
    private void popularFiltros(List<Talhao> talhoes, List<SafraInfo> safras) {
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
                super.updateItem(item, empty);
                setText(item == null || empty ? "" : item.getNome());
            }
        });
        // Adiciona listener
        filtroTalhao.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());


        // Filtro de Cultura
        String todasCulturas = "Todas as Culturas";
        filtroCultura.getItems().add(todasCulturas);
        List<String> culturas = safras.stream() // Usa a lista 'safras' do parâmetro
                                        .map(SafraInfo::getCultura)
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
        // Limpar os listeners temporariamente para evitar múltiplas chamadas
        // (Não é estritamente necessário, mas pode evitar chamadas redundantes)
        filtroCultura.getSelectionModel().select("Todas as Culturas");
        filtroTalhao.getSelectionModel().selectFirst(); // Seleciona o "Todos os Talhões"
        filtroDataInicio.setValue(null);
        filtroDataFim.setValue(null); 
        
        // handleAplicarFiltro() é chamado automaticamente pelos listeners
    }

    /**
     * ATUALIZADO: Agora filtra a listaMestraSafrasComInfo (em memória).
     * Filtra apenas as "Colhidas" para exibição nesta tela.
     */
    @FXML
    private void handleAplicarFiltro() {
        String culturaSel = filtroCultura.getSelectionModel().getSelectedItem();
        Talhao talhaoSel = filtroTalhao.getSelectionModel().getSelectedItem();
        LocalDate dataInicio = filtroDataInicio.getValue(); 
        LocalDate dataFim = filtroDataFim.getValue(); 

        // 1. Filtrar a lista mestra
        List<SafraInfo> safrasFiltradas = listaMestraSafrasComInfo.stream()
            .filter(safra -> {
                // --- FILTRO PRINCIPAL: APENAS SAFRAS COLHIDAS ---
                if (!safra.getStatus().equalsIgnoreCase("Colhida")) {
                    return false;
                }
                
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
        atualizarChartProducaoCultura(safrasFiltradas);
    }

    /**
     * Atualiza o gráfico de linha (Produção Média por Safra)
     */
// ... (código existente) ...
    private void atualizarChartProducaoMedia(List<SafraInfo> safras) {
        // Agrupa por Ano/Safra e calcula a média de sc/ha
        Map<String, Double> mediaPorSafra = safras.stream()
            .collect(Collectors.groupingBy(
                SafraInfo::getAnoInicio,
                Collectors.averagingDouble(SafraInfo::getProducaoSacosPorHectare)
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
     * Atualiza o gráfico de pizza (Produção Total por Cultura)
     */
// ... (código existente) ...
    private void atualizarChartProducaoCultura(List<SafraInfo> safras) {
        // Agrupa por Cultura e soma a produção total (em sacos)
        Map<String, Double> producaoPorCultura = safras.stream()
            .collect(Collectors.groupingBy(
                SafraInfo::getCultura,
                Collectors.summingDouble(SafraInfo::getProducaoTotalSacos)
            ));

        dadosChartCultura.clear();
        producaoPorCultura.entrySet().stream()
            .sorted(Map.Entry.comparingByValue()) // Ordena para melhor visualização
            .forEach(entry -> {
                String nome = String.format("%s (%.0f sc)", entry.getKey(), entry.getValue());
                dadosChartCultura.add(new PieChart.Data(nome, entry.getValue()));
            });
    }
}
