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
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox; // Import para o HBox de alerta
import javafx.scene.layout.VBox; // NOVO: Import para os VBox dos cards

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
 */
public class DashboardController {

    // --- Componentes FXML ---

    // Alertas (Topo)
    @FXML
    private Label lblContasVencidasCount; // NOVO: Para contas vencidas
    @FXML
    private HBox alertaContasVencidasBox; // NOVO: HBox para contas vencidas
    @FXML
    private Label lblContasAVencerCount; // RENOMEADO
    @FXML
    private HBox alertaContasAVencerBox; // RENOMEADO
    @FXML
    private Label lblEstoqueBaixoCount; // NOVO
    @FXML
    private HBox alertaEstoqueBaixoBox; // NOVO

    // KPIs Financeiros
    @FXML
    private Label lblBalanco;
    @FXML
    private Label lblValorEstoque;
    @FXML
    private Label lblValorPatrimonio;
    @FXML
    private Label lblContasPendentes; // NOVO
    
    // NOVO: IDs dos VBox (cards) para clique
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

    // NOVO: IDs dos VBox (cards) para clique
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

    // --- DAOs e Lógica Interna ---
    private final FinanceiroDAO financeiroDAO;
    private final FuncionarioDAO funcionarioDAO;
    private final EstoqueDAO estoqueDAO;
    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO;
    private final PatrimonioDAO patrimonioDAO;
    private final ContaDAO contaDAO; // NOVO

    // Formatador para Reais (R$)
    private final NumberFormat currencyFormatter;
    
    // NOVO: Referência ao MainViewController para navegação
    private MainViewController mainViewController;

    public DashboardController() {
        // Instancia os DAOs
        financeiroDAO = new FinanceiroDAO();
        funcionarioDAO = new FuncionarioDAO();
        estoqueDAO = new EstoqueDAO();
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO();
        patrimonioDAO = new PatrimonioDAO();
        contaDAO = new ContaDAO(); // NOVO
        
        // Configura o formatador de moeda
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    /**
     * NOVO: Permite ao MainViewController injetar sua própria referência
     * para que este controller possa chamar os métodos de navegação.
     */
    public void setMainViewController(MainViewController mainViewController) {
        this.mainViewController = mainViewController;
    }

    /**
     * Chamado automaticamente ao carregar o FXML.
     * Busca os dados dos DAOs e preenche os cards.
     */
    @FXML
    public void initialize() {
        // Ordem de carregamento atualizada para refletir o novo layout
        
        // 1. Alertas (Topo) - NOVO
        carregarAlertas();
        
        // 2. KPIs Financeiros
        carregarBalanco();
        carregarValorTotalEstoque();
        carregarValorTotalPatrimonio();
        carregarContasPendentes(); // NOVO
        
        // 3. KPIs Operacionais
        carregarTotalSafras();
        carregarTotalFuncionarios();
        carregarTotalTalhoesEArea(); // Método combinado
        
        // 4. Gráficos
        carregarChartDespesas();
        carregarChartBalanco();
        carregarChartCulturas();
    }
    
    /**
     * ATUALIZADO: Carrega os cards de Alertas Operacionais
     * (Contas Vencidas, Contas a Vencer, Estoque Baixo).
     */
    private void carregarAlertas() {
        // 1. Alerta de Contas VENCIDAS (Perigo - Vermelho)
        try {
            int totalVencidas = contaDAO.getContagemContasVencidas(); 
            
            if (totalVencidas > 0) {
                lblContasVencidasCount.setText(String.valueOf(totalVencidas));
                alertaContasVencidasBox.setVisible(true);
                alertaContasVencidasBox.setManaged(true);
            } else {
                alertaContasVencidasBox.setVisible(false);
                alertaContasVencidasBox.setManaged(false);
            }
        } catch (SQLException e) {
            lblContasVencidasCount.setText("!");
            alertaContasVencidasBox.setVisible(true);
            alertaContasVencidasBox.setManaged(true);
            AlertUtil.showError("Dashboard", "Erro ao carregar alertas de contas vencidas.");
            e.printStackTrace();
        }
        
        // 2. Alerta de Contas A VENCER (Aviso - Amarelo)
        try {
            // Busca contas a vencer nos próximos 7 dias
            int totalAVencer = contaDAO.getContagemContasAVencer(7); 
            
            if (totalAVencer > 0) {
                lblContasAVencerCount.setText(String.valueOf(totalAVencer));
                alertaContasAVencerBox.setVisible(true);
                alertaContasAVencerBox.setManaged(true);
            } else {
                alertaContasAVencerBox.setVisible(false);
                alertaContasAVencerBox.setManaged(false);
            }
        } catch (SQLException e) {
            lblContasAVencerCount.setText("!"); // Mostra um erro
            alertaContasAVencerBox.setVisible(true); // Mostra o card mesmo com erro
            alertaContasAVencerBox.setManaged(true);
            AlertUtil.showError("Dashboard", "Erro ao carregar alertas de contas a vencer.");
            e.printStackTrace();
        }

        // 3. NOVO: Alerta de Estoque Baixo (Aviso - Amarelo)
         try {
            int totalEstoqueBaixo = estoqueDAO.getContagemItensEstoqueBaixo();
            
            if (totalEstoqueBaixo > 0) {
                lblEstoqueBaixoCount.setText(String.valueOf(totalEstoqueBaixo));
                alertaEstoqueBaixoBox.setVisible(true);
                alertaEstoqueBaixoBox.setManaged(true);
            } else {
                alertaEstoqueBaixoBox.setVisible(false);
                alertaEstoqueBaixoBox.setManaged(false);
            }
        } catch (SQLException e) {
            lblEstoqueBaixoCount.setText("!");
            alertaEstoqueBaixoBox.setVisible(true);
            alertaEstoqueBaixoBox.setManaged(true);
            AlertUtil.showError("Dashboard", "Erro ao carregar alertas de estoque baixo.");
            e.printStackTrace();
        }
    }


    private void carregarBalanco() {
        try {
            double balanco = financeiroDAO.getBalançoFinanceiro();
            String balancoFormatado = currencyFormatter.format(balanco);
            
            lblBalanco.setText(balancoFormatado);

            // Adiciona classe de estilo (CSS) para cor
            lblBalanco.getStyleClass().removeAll("positivo-text", "negativo-text"); // Usa novas classes
            if (balanco >= 0) {
                lblBalanco.getStyleClass().add("positivo-text");
            } else {
                lblBalanco.getStyleClass().add("negativo-text");
            }
        } catch (SQLException e) {
            lblBalanco.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar balanço financeiro.");
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
     * NOVO: Carrega o balanço de contas pendentes (Receber - Pagar).
     */
    private void carregarContasPendentes() {
        try {
            double aReceber = contaDAO.getTotalPendente("receber");
            double aPagar = contaDAO.getTotalPendente("pagar");
            double balanco = aReceber - aPagar;
            
            lblContasPendentes.setText(currencyFormatter.format(balanco));
            
            lblContasPendentes.getStyleClass().removeAll("positivo-text", "negativo-text");
            if (balanco >= 0) {
                lblContasPendentes.getStyleClass().add("positivo-text");
            } else {
                lblContasPendentes.getStyleClass().add("negativo-text");
            }
        } catch (SQLException e) {
            lblContasPendentes.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar contas pendentes.");
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
     * ATUALIZADO: Carrega a contagem de talhões E a área total nos labels corretos.
     */
    private void carregarTotalTalhoesEArea() {
        try {
            int totalTalhoes = talhaoDAO.getContagemTalhoes();
            lblTalhoes.setText(String.valueOf(totalTalhoes));
        } catch (SQLException e) {
            lblTalhoes.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar total de talhões.");
        }
        
        try {
            double totalArea = talhaoDAO.getTotalAreaHectares();
            // Formata para "(120,5 ha)"
            lblAreaTotal.setText(String.format(Locale.forLanguageTag("pt-BR"), "(%.1f ha)", totalArea));
        } catch (SQLException e) {
            lblAreaTotal.setText("(Erro)");
            AlertUtil.showError("Dashboard", "Erro ao carregar área total.");
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
            // chartDespesas.setTitle("Receitas vs. Despesas"); // Título já está no FXML
        } catch (SQLException e) {
            AlertUtil.showError("Dashboard", "Erro ao carregar gráfico de financeiro.");
        }
    }

    /**
     * Carrega o gráfico de linha do histórico de balanço (DIÁRIO).
     */
    private void carregarChartBalanco() {
        try {
            // ATUALIZADO: Chama o novo método do DAO
            Map<String, Double> balancoMap = financeiroDAO.getBalancoPorDia(); 
            
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Balanço Diário"); // Título da série atualizado

            chartBalanco.getData().clear(); 

            for (Map.Entry<String, Double> entry : balancoMap.entrySet()) {
                series.getData().add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            chartBalanco.getData().add(series);
            // chartBalanco.setTitle("Histórico do Balanço (Diário)"); // Título já está no FXML
        } catch (SQLException e) {
            AlertUtil.showError("Dashboard", "Erro ao carregar histórico de balanço.");
            e.printStackTrace();
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
                 // chartCulturas.setTitle("Culturas Ativas"); // Título já está no FXML
            }

        } catch (SQLException e) {
            AlertUtil.showError("Dashboard", "Erro ao carregar gráfico de culturas.");
            e.printStackTrace();
        }
    }

    // --- MÉTODOS DE NAVEGAÇÃO (NOVOS) ---

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
