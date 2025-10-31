package com.farmmanager.controller;

import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.model.FinanceiroDAO;
import com.farmmanager.model.FuncionarioDAO;
import com.farmmanager.model.SafraDAO; // NOVO
import com.farmmanager.model.TalhaoDAO; // NOVO
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections; // NOVO
import javafx.collections.ObservableList; // NOVO
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart; // NOVO
import javafx.scene.chart.PieChart; // NOVO
import javafx.scene.chart.XYChart; // NOVO
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map; // NOVO

/**
 * Controller para o DashboardView.fxml.
 * ATUALIZADO:
 * - Carrega dados de Safras e Talhões.
 * - Lógica de 'Safras Ativas' atualizada para usar o novo campo 'status'.
 * - NOVO: Carrega gráfico de Culturas Ativas.
 */
public class DashboardController {

    @FXML
    private Label lblBalanco;
    @FXML
    private Label lblFuncionarios;
    @FXML
    private Label lblEstoque;

    // NOVO: Labels para os novos cards
    @FXML
    private Label lblTalhoes;
    @FXML
    private Label lblSafras;
    @FXML
    private Label lblAreaTotal;

    // NOVO: IDs dos Gráficos
    @FXML
    private PieChart chartDespesas;
    @FXML
    private LineChart<String, Number> chartBalanco;
    @FXML
    private PieChart chartCulturas; // NOVO

    // DAOs necessários para o resumo
    private final FinanceiroDAO financeiroDAO;
    private final FuncionarioDAO funcionarioDAO;
    private final EstoqueDAO estoqueDAO;
    private final SafraDAO safraDAO; // NOVO
    private final TalhaoDAO talhaoDAO; // NOVO

    // Formatador para Reais (R$)
    private final NumberFormat currencyFormatter;

    public DashboardController() {
        // Instancia os DAOs
        financeiroDAO = new FinanceiroDAO();
        funcionarioDAO = new FuncionarioDAO();
        estoqueDAO = new EstoqueDAO();
        safraDAO = new SafraDAO(); // NOVO
        talhaoDAO = new TalhaoDAO(); // NOVO
        
        // Configura o formatador de moeda
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    /**
     * Chamado automaticamente ao carregar o FXML.
     * Busca os dados dos DAOs e preenche os cards.
     */
    @FXML
    public void initialize() {
        carregarBalanco();
        carregarTotalFuncionarios();
        carregarTotalEstoque();
        carregarTotalTalhoes(); // NOVO
        carregarTotalSafras(); // NOVO
        carregarAreaTotal(); // NOVO
        carregarChartDespesas(); // NOVO
        carregarChartBalanco(); // NOVO
        carregarChartCulturas(); // NOVO
    }

    private void carregarBalanco() {
        try {
            double balanco = financeiroDAO.getBalançoFinanceiro();
            String balancoFormatado = currencyFormatter.format(balanco);
            
            lblBalanco.setText(balancoFormatado);

            // Adiciona classe de estilo (CSS) para cor
            lblBalanco.getStyleClass().removeAll("positivo", "negativo");
            if (balanco >= 0) {
                lblBalanco.getStyleClass().add("positivo");
            } else {
                lblBalanco.getStyleClass().add("negativo");
            }
        } catch (SQLException e) {
            lblBalanco.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar balanço financeiro.");
        }
    }

    private void carregarTotalFuncionarios() {
        try {
            int total = funcionarioDAO.getContagemFuncionarios();
            lblFuncionarios.setText(String.valueOf(total));
        } catch (SQLException e) {
            lblFuncionarios.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar total de funcionários.");
        }
    }

    private void carregarTotalEstoque() {
        try {
            int total = estoqueDAO.getContagemItensDistintos();
            lblEstoque.setText(String.valueOf(total));
        } catch (SQLException e) {
            lblEstoque.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar total de itens em estoque.");
        }
    }

    /**
     * NOVO: Carrega o número total de talhões.
     */
    private void carregarTotalTalhoes() {
        try {
            int total = talhaoDAO.getContagemTalhoes();
            lblTalhoes.setText(String.valueOf(total));
        } catch (SQLException e) {
            lblTalhoes.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar total de talhões.");
        }
    }

    /**
     * NOVO: Carrega o número de safras ativas (status != 'Colhida').
     */
    private void carregarTotalSafras() {
        try {
            // Lógica atualizada para usar o novo método do DAO
            int total = safraDAO.getContagemSafrasAtivas();
            lblSafras.setText(String.valueOf(total));
        } catch (SQLException e) {
            lblSafras.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar total de safras ativas.");
        }
    }

    /**
     * NOVO: Carrega a área total da fazenda em hectares.
     */
    private void carregarAreaTotal() {
        try {
            double total = talhaoDAO.getTotalAreaHectares();
            // Formata para "120,5 ha"
            lblAreaTotal.setText(String.format(Locale.forLanguageTag("pt-BR"), "%.1f ha", total));
        } catch (SQLException e) {
            lblAreaTotal.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar área total.");
        }
    }

    /**
     * NOVO: Carrega o gráfico de pizza de Receitas vs. Despesas.
     */
    private void carregarChartDespesas() {
        try {
            // Busca o mapa com "receita" e "despesa"
            Map<String, Double> totais = financeiroDAO.getTotaisReceitaDespesa();
            
            double totalReceitas = totais.getOrDefault("receita", 0.0);
            double totalDespesas = totais.getOrDefault("despesa", 0.0);

            ObservableList<PieChart.Data> pieChartData =
                    FXCollections.observableArrayList(
                            new PieChart.Data("Receitas", totalReceitas),
                            new PieChart.Data("Despesas", totalDespesas)
                    );
            
            chartDespesas.setData(pieChartData);
            chartDespesas.setTitle("Receitas vs. Despesas");
        } catch (SQLException e) {
            AlertUtil.showError("Dashboard", "Erro ao carregar gráfico de financeiro.");
        }
    }

    /**
     * NOVO: Carrega o gráfico de linha do histórico de balanço.
     */
    private void carregarChartBalanco() {
        try {
            // Busca o mapa de histórico mensal
            Map<String, Double> balancoMap = financeiroDAO.getBalancoPorMes();
            
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Balanço Mensal");

            // Limpa dados antigos antes de adicionar novos
            chartBalanco.getData().clear(); 

            for (Map.Entry<String, Double> entry : balancoMap.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            chartBalanco.getData().add(series);
            chartBalanco.setTitle("Histórico do Balanço (Mensal)");
        } catch (SQLException e) {
            AlertUtil.showError("Dashboard", "Erro ao carregar histórico de balanço.");
        }
    }

    /**
     * NOVO: Carrega o gráfico de pizza de culturas ativas.
     */
    private void carregarChartCulturas() {
        try {
            Map<String, Integer> contagemCulturas = safraDAO.getContagemCulturasAtivas();

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : contagemCulturas.entrySet()) {
                // Adiciona a contagem ao label (ex: "Soja (3)")
                pieChartData.add(new PieChart.Data(entry.getKey() + " (" + entry.getValue() + ")", entry.getValue()));
            }

            chartCulturas.setData(pieChartData);
            
            if (contagemCulturas.isEmpty()) {
                chartCulturas.setTitle("Nenhuma cultura ativa");
            } else {
                 chartCulturas.setTitle("Culturas Ativas");
            }

        } catch (SQLException e) {
            AlertUtil.showError("Dashboard", "Erro ao carregar gráfico de culturas.");
            e.printStackTrace(); // Ajuda a depurar
        }
    }
}
