package com.farmmanager.controller;

import com.farmmanager.model.ContaDAO;
import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.model.FinanceiroDAO;
import com.farmmanager.model.FuncionarioDAO;
import com.farmmanager.model.PatrimonioDAO;
import com.farmmanager.model.SafraDAO;
import com.farmmanager.model.TalhaoDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task; // NOVO: Import para Task
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator; // NOVO: Import
import javafx.scene.control.ScrollPane; // NOVO: Import
import javafx.scene.layout.HBox; 
import javafx.scene.layout.VBox; 

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Controller para o DashboardView.fxml.
 * ATUALIZADO (Refatorado):
 * - Adicionado card de Contas Pendentes.
 * - Reorganizada a ordem de carregamento para refletir o novo FXML.
 * - Ajustado `carregarAreaTotal` para popular o novo card de "Área Total".
 * - ATUALIZADO: `carregarAlertas()` e FXML IDs relacionados foram re-adicionados.
 * - ATUALIZADO: `carregarChartBalanco()` para usar `getBalancoPorDia()`.
 * - ATUALIZADO: Alertas agora separam Vencidas de A Vencer.
 * - MELHORIA UX: Adicionado alerta de Estoque Baixo.
 * - MELHORIA UX: Adicionados métodos de navegação (ex: navigateToSafras).
 * - MELHORIA CRÍTICA: Todo o carregamento de dados do initialize()
 * movido para uma Task em background para não congelar a UI.
 */
public class DashboardController {

    // --- Componentes FXML ---

    // Alertas (Topo)
    @FXML
    private Label lblContasVencidasCount; 
    @FXML
    private HBox alertaContasVencidasBox; 
    @FXML
    private Label lblContasAVencerCount; 
    @FXML
    private HBox alertaContasAVencerBox; 
    @FXML
    private Label lblEstoqueBaixoCount; 
    @FXML
    private HBox alertaEstoqueBaixoBox; 

    // KPIs Financeiros
    @FXML
    private Label lblBalanco;
    @FXML
    private Label lblValorEstoque;
    @FXML
    private Label lblValorPatrimonio;
    @FXML
    private Label lblContasPendentes; 
    
    // IDs dos VBox (cards) para clique
    @FXML
    private VBox cardBalanco;
    @FXML
    private VBox cardEstoque;
    @FXML
    private VBox cardPatrimonio;
    @FXML
    private VBox cardContas;

    // KPIs Operacionais
    @FXML
    private Label lblTalhoes;
    @FXML
    private Label lblSafras;
    @FXML
    private Label lblFuncionarios;
    @FXML
    private Label lblAreaTotal;

    // IDs dos VBox (cards) para clique
    @FXML
    private VBox cardSafras;
    @FXML
    private VBox cardFuncionarios;
    @FXML
    private VBox cardTalhoes;

    // Gráficos
    @FXML
    private PieChart chartDespesas;
    @FXML
    private LineChart<String, Number> chartBalanco;
    @FXML
    private PieChart chartCulturas;

    // NOVO: Componentes para controle de carregamento
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private ScrollPane contentScrollPane; // Container principal (ScrollPane do FXML)

    // --- DAOs e Lógica Interna ---
    private final FinanceiroDAO financeiroDAO;
    private final FuncionarioDAO funcionarioDAO;
    private final EstoqueDAO estoqueDAO;
    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO;
    private final PatrimonioDAO patrimonioDAO;
    private final ContaDAO contaDAO; 

    private final NumberFormat currencyFormatter;
    
    private MainViewController mainViewController;

    /**
     * NOVO: Classe interna para agrupar todos os dados
     * buscados na Task de background.
     */
    private static class DashboardData {
        int totalVencidas;
        int totalAVencer;
        int totalEstoqueBaixo;
        double balanco;
        double valorEstoque;
        double valorPatrimonio;
        double contasAReceber;
        double contasAPagar;
        int totalSafras;
        int totalFuncionarios;
        int totalTalhoes;
        double totalArea;
        Map<String, Double> totaisReceitaDespesa;
        Map<String, Double> balancoPorDia;
        Map<String, Integer> contagemCulturas;
    }

    public DashboardController() {
        // Instancia os DAOs
        financeiroDAO = new FinanceiroDAO();
        funcionarioDAO = new FuncionarioDAO();
        estoqueDAO = new EstoqueDAO();
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO();
        patrimonioDAO = new PatrimonioDAO();
        contaDAO = new ContaDAO(); 
        
        // Configura o formatador de moeda
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    /**
     * ATUALIZADO: Chamado automaticamente ao carregar o FXML.
     * Apenas configura o estado inicial e chama o carregador assíncrono.
     */
    @FXML
    public void initialize() {
        // Garante que os HBox de alerta não ocupem espaço antes do carregamento
        alertaContasVencidasBox.setVisible(false);
        alertaContasVencidasBox.setManaged(false);
        alertaContasAVencerBox.setVisible(false);
        alertaContasAVencerBox.setManaged(false);
        alertaEstoqueBaixoBox.setVisible(false);
        alertaEstoqueBaixoBox.setManaged(false);

        // Chama o novo método de carregamento assíncrono
        carregarDadosDashboardAssincrono();
    }
    
    /**
     * NOVO: Controla a visibilidade do indicador de carregamento.
     */
    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisible(isLoading);
        loadingIndicator.setManaged(isLoading);
        
        contentScrollPane.setDisable(isLoading);
        // O ScrollPane não tem VBox, então o opacity é aplicado nele
        contentScrollPane.setOpacity(isLoading ? 0.5 : 1.0); 
    }

    /**
     * NOVO: Cria uma Task para carregar todos os dados do dashboard
     * em uma thread de background.
     */
    private void carregarDadosDashboardAssincrono() {
        Task<DashboardData> carregarTask = new Task<DashboardData>() {
            @Override
            protected DashboardData call() throws Exception {
                // Esta é a parte demorada (chamadas de DB)
                // É executada em uma thread de background.
                DashboardData data = new DashboardData();

                // Alertas
                data.totalVencidas = contaDAO.getContagemContasVencidas();
                data.totalAVencer = contaDAO.getContagemContasAVencer(7);
                data.totalEstoqueBaixo = estoqueDAO.getContagemItensEstoqueBaixo();
                
                // KPIs Financeiros
                data.balanco = financeiroDAO.getBalançoFinanceiro();
                data.valorEstoque = estoqueDAO.getValorTotalEmEstoque();
                data.valorPatrimonio = patrimonioDAO.getValorTotalPatrimonio();
                data.contasAReceber = contaDAO.getTotalPendente("receber");
                data.contasAPagar = contaDAO.getTotalPendente("pagar");

                // KPIs Operacionais
                data.totalSafras = safraDAO.getContagemSafrasAtivas();
                data.totalFuncionarios = funcionarioDAO.getContagemFuncionarios();
                data.totalTalhoes = talhaoDAO.getContagemTalhoes();
                data.totalArea = talhaoDAO.getTotalAreaHectares();

                // Gráficos
                data.totaisReceitaDespesa = financeiroDAO.getTotaisReceitaDespesa();
                data.balancoPorDia = financeiroDAO.getBalancoPorDia();
                data.contagemCulturas = safraDAO.getContagemCulturasAtivas();
                
                return data;
            }
        };

        // Define o que fazer quando a Task for bem-sucedida (na JavaFX Thread)
        carregarTask.setOnSucceeded(e -> {
            DashboardData data = carregarTask.getValue();
            
            // Agora, chama os métodos de *atualização da UI* (rápidos)
            // passando os dados que foram buscados.
            try {
                atualizarAlertas(data.totalVencidas, data.totalAVencer, data.totalEstoqueBaixo);
                atualizarKPIsFinanceiros(data.balanco, data.valorEstoque, data.valorPatrimonio, data.contasAReceber, data.contasAPagar);
                atualizarKPIsOperacionais(data.totalSafras, data.totalFuncionarios, data.totalTalhoes, data.totalArea);
                atualizarGraficos(data.totaisReceitaDespesa, data.balancoPorDia, data.contagemCulturas);
            } catch (Exception ex) {
                AlertUtil.showError("Erro de UI", "Erro ao exibir dados do dashboard: " + ex.getMessage());
                ex.printStackTrace();
            }
            
            showLoading(false); // Esconde o loading
        });

        // Define o que fazer se a Task falhar (na JavaFX Thread)
        carregarTask.setOnFailed(e -> {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os dados do dashboard.");
            carregarTask.getException().printStackTrace();
            showLoading(false); // Esconde o loading
        });

        // Mostra o loading ANTES de iniciar a Task
        showLoading(true);

        // Inicia a Task em uma nova Thread
        new Thread(carregarTask).start();
    }


    /**
     * ATUALIZADO: Agora apenas atualiza a UI com dados pré-buscados.
     */
    private void atualizarAlertas(int totalVencidas, int totalAVencer, int totalEstoqueBaixo) {
        if (totalVencidas > 0) {
            lblContasVencidasCount.setText(String.valueOf(totalVencidas));
            alertaContasVencidasBox.setVisible(true);
            alertaContasVencidasBox.setManaged(true);
        } else {
            alertaContasVencidasBox.setVisible(false);
            alertaContasVencidasBox.setManaged(false);
        }
        
        if (totalAVencer > 0) {
            lblContasAVencerCount.setText(String.valueOf(totalAVencer));
            alertaContasAVencerBox.setVisible(true);
            alertaContasAVencerBox.setManaged(true);
        } else {
            alertaContasAVencerBox.setVisible(false);
            alertaContasAVencerBox.setManaged(false);
        }

         if (totalEstoqueBaixo > 0) {
            lblEstoqueBaixoCount.setText(String.valueOf(totalEstoqueBaixo));
            alertaEstoqueBaixoBox.setVisible(true);
            alertaEstoqueBaixoBox.setManaged(true);
        } else {
            alertaEstoqueBaixoBox.setVisible(false);
            alertaEstoqueBaixoBox.setManaged(false);
        }
    }

    /**
     * NOVO: Método unificado para atualizar KPIs financeiros com dados pré-buscados.
     */
    private void atualizarKPIsFinanceiros(double balanco, double valorEstoque, double valorPatrimonio, double aReceber, double aPagar) {
        // Balanço
        String balancoFormatado = currencyFormatter.format(balanco);
        lblBalanco.setText(balancoFormatado);
        lblBalanco.getStyleClass().removeAll("positivo-text", "negativo-text");
        lblBalanco.getStyleClass().add(balanco >= 0 ? "positivo-text" : "negativo-text");

        // Estoque
        lblValorEstoque.setText(currencyFormatter.format(valorEstoque));
        // Patrimonio
        lblValorPatrimonio.setText(currencyFormatter.format(valorPatrimonio));
        
        // Contas Pendentes
        double balancoContas = aReceber - aPagar;
        lblContasPendentes.setText(currencyFormatter.format(balancoContas));
        lblContasPendentes.getStyleClass().removeAll("positivo-text", "negativo-text");
        lblContasPendentes.getStyleClass().add(balancoContas >= 0 ? "positivo-text" : "negativo-text");
    }

    /**
     * NOVO: Método unificado para atualizar KPIs operacionais com dados pré-buscados.
     */
    private void atualizarKPIsOperacionais(int totalSafras, int totalFuncionarios, int totalTalhoes, double totalArea) {
        lblSafras.setText(String.valueOf(totalSafras));
        lblFuncionarios.setText(String.valueOf(totalFuncionarios));
        lblTalhoes.setText(String.valueOf(totalTalhoes));
        lblAreaTotal.setText(String.format(Locale.forLanguageTag("pt-BR"), "(%.1f ha)", totalArea));
    }

    /**
     * NOVO: Método unificado para atualizar todos os gráficos com dados pré-buscados.
     */
    private void atualizarGraficos(Map<String, Double> totaisReceitaDespesa, Map<String, Double> balancoPorDia, Map<String, Integer> contagemCulturas) {
        // Gráfico Despesas (Receitas vs. Despesas)
        double totalReceitas = totaisReceitaDespesa.getOrDefault("receita", 0.0);
        double totalDespesas = totaisReceitaDespesa.getOrDefault("despesa", 0.0);
        ObservableList<PieChart.Data> pieChartData =
                FXCollections.observableArrayList(
                        new PieChart.Data("Receitas", totalReceitas),
                        new PieChart.Data("Despesas", totalDespesas)
                );
        chartDespesas.setData(pieChartData);

        // Gráfico Balanço Diário
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Balanço Diário");
        chartBalanco.getData().clear();
        for (Map.Entry<String, Double> entry : balancoPorDia.entrySet()) {
            series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
        }
        chartBalanco.getData().add(series);

        // Gráfico Culturas Ativas
        ObservableList<PieChart.Data> pieChartDataCulturas = FXCollections.observableArrayList();
        for (Map.Entry<String, Integer> entry : contagemCulturas.entrySet()) {
            pieChartDataCulturas.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
        }
        chartCulturas.setData(pieChartDataCulturas);
        if (contagemCulturas.isEmpty()) {
            chartCulturas.setTitle("Nenhuma cultura ativa");
        }
    }


    // --- MÉTODOS DE NAVEGAÇÃO (NOVOS) ---
    // (Estes não precisam de Task pois são apenas trocas de tela)

    @FXML
    private void navigateToFinanceiro() {
        if (mainViewController != null) {
            mainViewController.handleShowFinanceiro();
        }
    }

    @FXML
    private void navigateToEstoque() {
        if (mainViewController != null) {
            mainViewController.handleShowEstoque();
        }
    }

    @FXML
    private void navigateToPatrimonio() {
        if (mainViewController != null) {
            mainViewController.handleShowPatrimonio();
        }
    }

    @FXML
    private void navigateToContas() {
        if (mainViewController != null) {
            mainViewController.handleShowContas();
        }
    }

    @FXML
    private void navigateToSafras() {
        if (mainViewController != null) {
            mainViewController.handleShowSafras();
        }
    }

    @FXML
    private void navigateToFuncionarios() {
        if (mainViewController != null) {
            mainViewController.handleShowFuncionarios();
        }
    }
}
