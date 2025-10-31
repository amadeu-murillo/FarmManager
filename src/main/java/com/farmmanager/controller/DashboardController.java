package com.farmmanager.controller;

import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.model.FinanceiroDAO;
import com.farmmanager.model.FuncionarioDAO;
import com.farmmanager.model.PatrimonioDAO; // NOVO
import com.farmmanager.model.SafraDAO;
import com.farmmanager.model.TalhaoDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;

/**
 * Controller para o DashboardView.fxml.
 * ATUALIZADO:
 * - Carrega dados de Safras e Talhões.
 * - Lógica de 'Safras Ativas' atualizada para usar o novo campo 'status'.
 * - NOVO: Carrega gráfico de Culturas Ativas.
 * - NOVO: Adicionados cards de Alertas (Estoque Baixo, Patrimônio em Manutenção).
 * - NOVO: Adicionado card de Valor Total do Patrimônio.
 * - ATUALIZADO: Card de Estoque agora mostra Valor Total (R$) ao invés de contagem.
 * - ATUALIZADO: Card de Talhões e Área agora estão combinados.
 */
public class DashboardController {

    @FXML
    private Label lblBalanco;
    @FXML
    private Label lblFuncionarios;
    @FXML
    private Label lblValorEstoque; // ATUALIZADO (era lblEstoque)

    // Labels para os cards antigos
    @FXML
    private Label lblTalhoes;
    @FXML
    private Label lblSafras;
    @FXML
    private Label lblAreaTotal;

    // NOVO: Labels para os novos cards
    @FXML
    private Label lblValorPatrimonio;
    @FXML
    private Label lblEstoqueBaixo;
    @FXML
    private Label lblPatrimonioManutencao;

    // IDs dos Gráficos
    @FXML
    private PieChart chartDespesas;
    @FXML
    private LineChart<String, Number> chartBalanco;
    @FXML
    private PieChart chartCulturas;

    // DAOs necessários para o resumo
    private final FinanceiroDAO financeiroDAO;
    private final FuncionarioDAO funcionarioDAO;
    private final EstoqueDAO estoqueDAO;
    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO;
    private final PatrimonioDAO patrimonioDAO; // NOVO

    // Formatador para Reais (R$)
    private final NumberFormat currencyFormatter;

    public DashboardController() {
        // Instancia os DAOs
        financeiroDAO = new FinanceiroDAO();
        funcionarioDAO = new FuncionarioDAO();
        estoqueDAO = new EstoqueDAO();
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO();
        patrimonioDAO = new PatrimonioDAO(); // NOVO
        
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
        carregarValorTotalEstoque(); // ATUALIZADO
        carregarValorTotalPatrimonio(); // NOVO
        carregarTotalTalhoes();
        carregarTotalSafras();
        carregarAreaTotal();
        carregarAlertas(); // NOVO
        carregarChartDespesas();
        carregarChartBalanco();
        carregarChartCulturas();
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

    /**
     * ATUALIZADO: Carrega o VALOR TOTAL (R$) do estoque, não a contagem de itens.
     */
    private void carregarValorTotalEstoque() {
        try {
            double total = estoqueDAO.getValorTotalEmEstoque();
            lblValorEstoque.setText(currencyFormatter.format(total));
        } catch (SQLException e) {
            lblValorEstoque.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar valor total do estoque.");
        }
    }

    /**
     * NOVO: Carrega o VALOR TOTAL (R$) do patrimônio.
     */
    private void carregarValorTotalPatrimonio() {
        try {
            double total = patrimonioDAO.getValorTotalPatrimonio();
            lblValorPatrimonio.setText(currencyFormatter.format(total));
        } catch (SQLException e) {
            lblValorPatrimonio.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar valor total do patrimônio.");
        }
    }

    /**
     * Carrega o número total de talhões (para o card combinado).
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
     * Carrega o número de safras ativas (status != 'Colhida').
     */
    private void carregarTotalSafras() {
        try {
            int total = safraDAO.getContagemSafrasAtivas();
            lblSafras.setText(String.valueOf(total));
        } catch (SQLException e) {
            lblSafras.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar total de safras ativas.");
        }
    }

    /**
     * Carrega a área total da fazenda (para o card combinado).
     * ATUALIZADO: Formato de texto sutil.
     */
    private void carregarAreaTotal() {
        try {
            double total = talhaoDAO.getTotalAreaHectares();
            // Formata para "(120,5 ha no total)"
            lblAreaTotal.setText(String.format(Locale.forLanguageTag("pt-BR"), "(%.1f ha no total)", total));
        } catch (SQLException e) {
            lblAreaTotal.setText("(Erro)");
            AlertUtil.showError("Dashboard", "Erro ao carregar área total.");
        }
    }

    /**
     * NOVO: Carrega os cards de Alertas Operacionais.
     */
    private void carregarAlertas() {
        // Alerta 1: Estoque Baixo
        try {
            int total = estoqueDAO.getContagemItensEstoqueBaixo();
            lblEstoqueBaixo.setText(String.valueOf(total));
            // Adiciona/remove classe de alerta (vermelho)
            lblEstoqueBaixo.getStyleClass().remove("negativo-text");
            if (total > 0) {
                lblEstoqueBaixo.getStyleClass().add("negativo-text");
            }
        } catch (SQLException e) {
            lblEstoqueBaixo.setText("!");
            AlertUtil.showError("Dashboard", "Erro ao carregar alertas de estoque.");
        }

        // Alerta 2: Patrimônio em Manutenção
        try {
            int total = patrimonioDAO.getContagemPatrimonioPorStatus("Em Manutenção");
            lblPatrimonioManutencao.setText(String.valueOf(total));
            // Adiciona/remove classe de alerta (vermelho)
            lblPatrimonioManutencao.getStyleClass().remove("negativo-text");
            if (total > 0) {
                lblPatrimonioManutencao.getStyleClass().add("negativo-text");
            }
        } catch (SQLException e) {
            lblPatrimonioManutencao.setText("!");
            AlertUtil.showError("Dashboard", "Erro ao carregar alertas de patrimônio.");
        }
    }

    /**
     * Carrega o gráfico de pizza de Receitas vs. Despesas.
     */
    private void carregarChartDespesas() {
        try {
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
     * Carrega o gráfico de linha do histórico de balanço.
     */
    private void carregarChartBalanco() {
        try {
            Map<String, Double> balancoMap = financeiroDAO.getBalancoPorMes();
            
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Balanço Mensal");

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
     * Carrega o gráfico de pizza de culturas ativas.
     */
    private void carregarChartCulturas() {
        try {
            Map<String, Integer> contagemCulturas = safraDAO.getContagemCulturasAtivas();

            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            for (Map.Entry<String, Integer> entry : contagemCulturas.entrySet()) {
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
            e.printStackTrace();
        }
    }
}
