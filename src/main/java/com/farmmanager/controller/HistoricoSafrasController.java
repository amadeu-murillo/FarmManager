package com.farmmanager.controller;

import com.farmmanager.model.SafraDAO;
import com.farmmanager.model.SafraInfo;
import com.farmmanager.model.Talhao;
import com.farmmanager.model.TalhaoDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker; // NOVO
import javafx.scene.control.ListCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.TableCell; // NOVO
import javafx.geometry.Pos; // NOVO

import java.sql.SQLException;
import java.text.DecimalFormat; // NOVO
import java.text.NumberFormat; // NOVO
import java.time.LocalDate; // NOVO
import java.time.LocalDateTime; // NOVO
import java.time.format.DateTimeFormatter; // NOVO
import java.util.List;
import java.util.Locale; // NOVO
import java.util.Map;
import java.util.stream.Collectors;

/**
 * NOVO: Controller para a tela de Histórico e Relatórios de Safras.
 * Esta classe foca em analisar dados de safras *colhidas*.
 * ATUALIZADO: Adicionado filtro de data.
 * ATUALIZADO (initialize): Adicionada formatação decimal para coluna sc/ha.
 */
public class HistoricoSafrasController {

    // DAOs
    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO;

    // Listas de Dados
    private List<SafraInfo> listaMestraSafrasColhidas; // Apenas safras colhidas
    private final ObservableList<SafraInfo> dadosTabelaHistorico;
    private final ObservableList<PieChart.Data> dadosChartCultura;
    private final ObservableList<XYChart.Series<String, Number>> dadosChartProducaoMedia;
    
    // NOVO: Formatador de data/hora do banco
    private final DateTimeFormatter dbTimestampFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    // NOVO: Formatador decimal
    private final NumberFormat decimalFormatter;

    // Componentes FXML
    @FXML
    private ComboBox<String> filtroCultura;
    @FXML
    private ComboBox<Talhao> filtroTalhao;
    @FXML
    private Button btnAplicarFiltro;
    @FXML
    private Button btnLimparFiltro; // NOVO
    @FXML
    private DatePicker filtroDataInicio; // NOVO
    @FXML
    private DatePicker filtroDataFim; // NOVO

    // Gráficos
    @FXML
    private LineChart<String, Number> chartProducaoMedia;
    @FXML
    private PieChart chartProducaoCultura;

    // Tabela
    @FXML
    private TableView<SafraInfo> tabelaHistorico;
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
    private TableColumn<SafraInfo, String> colDataColheita; // NOVO
    @FXML
    private TableColumn<SafraInfo, Double> colProdSacos;
    @FXML
    private TableColumn<SafraInfo, Double> colProdScHa;

    public HistoricoSafrasController() {
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO();
        
        listaMestraSafrasColhidas = FXCollections.observableArrayList();
        dadosTabelaHistorico = FXCollections.observableArrayList();
        dadosChartCultura = FXCollections.observableArrayList();
        dadosChartProducaoMedia = FXCollections.observableArrayList();
        
        // NOVO: Formatador decimal para produtividade
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
        colDataColheita.setCellValueFactory(new PropertyValueFactory<>("dataModificacao")); // NOVO
        colProdSacos.setCellValueFactory(new PropertyValueFactory<>("producaoTotalSacos"));
        colProdScHa.setCellValueFactory(new PropertyValueFactory<>("producaoSacosPorHectare"));
        tabelaHistorico.setItems(dadosTabelaHistorico);

        // --- NOVO: Formatação da coluna de produtividade (sc/ha) ---
        colProdScHa.setCellFactory(col -> new TableCell<SafraInfo, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0.0) {
                    setText(null);
                    setAlignment(Pos.CENTER_RIGHT);
                } else {
                    // Formata para 2 casas decimais
                    setText(decimalFormatter.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });
        // --- FIM DA FORMATAÇÃO ---

        // 2. Configurar Gráficos
        chartProducaoCultura.setData(dadosChartCultura);
        chartProducaoMedia.setData(dadosChartProducaoMedia);

        // 3. Carregar Dados
        carregarFiltros();
        carregarDadosMestresEAtualizarTudo();
        
        // NOVO: Listeners para os novos filtros
        filtroDataInicio.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroDataFim.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
    }

    /**
     * Carrega a lista mestra de safras (apenas colhidas) do banco.
     */
    private void carregarDadosMestresEAtualizarTudo() {
        try {
            // Busca todas as safras e filtra apenas as colhidas
            listaMestraSafrasColhidas = safraDAO.listSafrasComInfo().stream()
                    .filter(s -> s.getStatus().equalsIgnoreCase("Colhida"))
                    .collect(Collectors.toList());
            
            // Aplica os filtros (que por padrão são "Todos") e atualiza a tela
            handleAplicarFiltro();

        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar o histórico de safras.");
            e.printStackTrace();
        }
    }

    /**
     * Carrega as opções para os ComboBoxes de filtro.
     */
    private void carregarFiltros() {
        try {
            // Filtro de Talhão
            Talhao todosTalhoes = new Talhao(0, "Todos os Talhões", 0); // Item "Todos"
            filtroTalhao.getItems().add(todosTalhoes);
            filtroTalhao.getItems().addAll(talhaoDAO.listTalhoes());
            filtroTalhao.getSelectionModel().select(todosTalhoes);
            
            // Configura como o Talhao é exibido no ComboBox
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

            // Filtro de Cultura
            String todasCulturas = "Todas as Culturas";
            filtroCultura.getItems().add(todasCulturas);
            // Pega nomes de culturas únicas da lista de safras
            List<String> culturas = safraDAO.listSafrasComInfo().stream()
                                            .map(SafraInfo::getCultura)
                                            .distinct()
                                            .sorted()
                                            .collect(Collectors.toList());
            filtroCultura.getItems().addAll(culturas);
            filtroCultura.getSelectionModel().select(todasCulturas);

        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os filtros de talhão.");
        }
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
        // handleAplicarFiltro(); // É chamado automaticamente pelos listeners dos DatePickers
    }

    /**
     * Chamado pelo botão "Aplicar Filtros" ou na inicialização.
     * Filtra a lista mestra e atualiza a tabela e os gráficos.
     */
    @FXML
    private void handleAplicarFiltro() {
        String culturaSel = filtroCultura.getSelectionModel().getSelectedItem();
        Talhao talhaoSel = filtroTalhao.getSelectionModel().getSelectedItem();
        LocalDate dataInicio = filtroDataInicio.getValue(); // NOVO
        LocalDate dataFim = filtroDataFim.getValue(); // NOVO

        // 1. Filtrar a lista mestra
        List<SafraInfo> safrasFiltradas = listaMestraSafrasColhidas.stream()
            .filter(safra -> {
                // Filtro de Cultura
                boolean culturaMatch = (culturaSel == null || culturaSel.equals("Todas as Culturas") || safra.getCultura().equals(culturaSel));
                // Filtro de Talhão
                boolean talhaoMatch = (talhaoSel == null || talhaoSel.getId() == 0 || safra.getTalhaoNome().equals(talhaoSel.getNome()));
                
                // NOVO: Filtro de Data (usa dataModificacao como data da colheita)
                boolean dataMatch = true;
                if (safra.getDataModificacao() != null) {
                    try {
                        // Converte o timestamp do banco (String) para LocalDate
                        LocalDate dataColheita = LocalDateTime.parse(safra.getDataModificacao(), dbTimestampFormatter).toLocalDate();
                        
                        if (dataInicio != null && dataColheita.isBefore(dataInicio)) {
                            dataMatch = false;
                        }
                        if (dataFim != null && dataColheita.isAfter(dataFim)) {
                            dataMatch = false;
                        }
                    } catch (Exception e) {
                        // Ignora falha no parse, considera que a data não bate
                        dataMatch = false; 
                    }
                } else {
                    // Se não tem data de modificação, não deve aparecer no filtro de data
                    if(dataInicio != null || dataFim != null) {
                        dataMatch = false;
                    }
                }

                return culturaMatch && talhaoMatch && dataMatch; // NOVO: dataMatch
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
     * (Feature: produção média por data)
     */
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
     * (Feature: histórico de produção por cultura)
     */
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
