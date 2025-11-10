package com.farmmanager.controller;

import com.farmmanager.model.Safra;
import com.farmmanager.model.SafraInfo;
import com.farmmanager.model.SafraDAO;
import com.farmmanager.model.Talhao;
import com.farmmanager.model.TalhaoDAO;
import com.farmmanager.model.EstoqueDAO; 
import com.farmmanager.model.EstoqueItem; 
import com.farmmanager.model.AtividadeSafra; 
import com.farmmanager.model.AtividadeSafraDAO; 
import com.farmmanager.model.FinanceiroDAO; 
import com.farmmanager.model.Transacao; 
import com.farmmanager.model.Conta; // NOVO: Import para Contas
import com.farmmanager.model.ContaDAO; // NOVO: Import para ContaDAO
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task; // NOVO: Import para Task
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox; 
import javafx.scene.layout.VBox; // NOVO: Import para o VBox
import javafx.geometry.Pos; 
import javafx.beans.value.ChangeListener; 
import javafx.beans.value.ObservableValue;
import javafx.stage.FileChooser; 
import java.io.File; 
import java.io.IOException; 
import java.io.PrintWriter; 
import java.sql.SQLException;
import java.text.DecimalFormat; 
import java.text.NumberFormat; 
import java.time.LocalDate; 
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors; 
import java.util.Optional;
import java.util.Locale;

/**
 * Controller para o SafrasView.fxml.
 * ATUALIZAÇÃO:
 * - Adicionada visualização de detalhes (atividades/custos) ao selecionar safra.
 * - Adicionada funcionalidade de exportação de CSV para safras colhidas.
 * - NOVO: Lançamento de atividade agora também gera "despesa" no financeiro.
 * - ATUALIZADO: Exportação CSV agora busca vendas reais e calcula o lucro.
 * - ATUALIZADO: Registro de colheita agora apenas adiciona ao estoque (ativo), não lança receita.
 * - ATUALIZADO: Painel de detalhes agora inclui resumo financeiro (Receita, Estoque, Lucro).
 * - ATUALIZAÇÃO (handleLancarAtividade): Adicionado botão "MAX" para preencher a quantidade total.
 * - ATUALIZAÇÃO (handleLancarAtividade): Adicionada opção de "Custo Manual".
 * - ATUALIZAÇÃO (handleLancarAtividade): Custo manual agora lança no financeiro.
 * - ATUALIZAÇÃO (initialize): Adicionada formatação decimal para coluna sc/ha.
 * - MELHORIA CRÍTICA: Carregamento de dados (Safras e Talhões)
 * movido para uma Task em background para não congelar a UI.
 * - CORREÇÃO (handleRegistrarColheita): Corrigida chamada ao construtor de EstoqueItem.
 * - CORREÇÃO (handleExportarCsv): Adicionado BOM UTF-8 para corrigir acentuação no Excel.
 * - ATUALIZAÇÃO (handleSafraSelectionChanged, handleExportarCsv): Agora somam vendas "À Vista" (Financeiro) 
 * e "A Prazo" (Contas a Receber) para calcular a receita total da safra.
 * - MELHORIA (handleNovoTalhao): Adicionado cálculo automático entre Hectares e Alqueires.
 */
public class SafrasController {

    // Tabela Safras
    @FXML
    private TableView<SafraInfo> tabelaSafras;
    // ... (colunas safras)
    @FXML
    private TableColumn<SafraInfo, Integer> colSafraId;
    @FXML
    private TableColumn<SafraInfo, String> colSafraCultura;
    @FXML
    private TableColumn<SafraInfo, String> colSafraAno; 
    @FXML
    private TableColumn<SafraInfo, String> colSafraTalhao;
    @FXML
    private TableColumn<SafraInfo, String> colSafraStatus; 
    @FXML
    private TableColumn<SafraInfo, Double> colSafraProd;
    @FXML
    private TableColumn<SafraInfo, Double> colSafraProdTotalSacos;
    @FXML
    private TableColumn<SafraInfo, Double> colSafraProdTotalKg; 

    // Tabela Talhões
    @FXML
    private TableView<Talhao> tabelaTalhoes;
    // ... (colunas talhoes)
    @FXML
    private TableColumn<Talhao, Integer> colTalhaoId;
    @FXML
    private TableColumn<Talhao, String> colTalhaoNome;
    @FXML
    private TableColumn<Talhao, Double> colTalhaoArea;

    // Filtro
    @FXML
    private ComboBox<String> filtroStatusSafra;

    // Painel de Detalhes
    @FXML
    private Label lblDetalhesTitulo;
    @FXML
    private TableView<AtividadeSafraInfo> tabelaAtividades; 
    @FXML
    private TableColumn<AtividadeSafraInfo, String> colAtvData;
    @FXML
    private TableColumn<AtividadeSafraInfo, String> colAtvDesc;
    @FXML
    private TableColumn<AtividadeSafraInfo, String> colAtvInsumo;
    @FXML
    private TableColumn<AtividadeSafraInfo, Double> colAtvQtd;
    @FXML
    private TableColumn<AtividadeSafraInfo, Double> colAtvCusto;
    
    // Resumo Financeiro
    @FXML
    private Label lblCustoTotalSafra;
    @FXML
    private Label lblReceitaTotalSafra;
    @FXML
    private Label lblEstoqueTotalSafra;
    @FXML
    private Label lblBalancoSafra;
    
    @FXML
    private Button btnExportarCsv; 

    // NOVO: Componentes para controle de carregamento
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private VBox contentVBox; // Container principal (VBox do FXML)

    // --- Lógica Interna ---
    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO;
    private final EstoqueDAO estoqueDAO; 
    private final AtividadeSafraDAO atividadeSafraDAO; 
    private final FinanceiroDAO financeiroDAO; 
    private final ContaDAO contaDAO; // NOVO: DAO para Contas
    
    private final ObservableList<SafraInfo> dadosTabelaSafras; 
    private List<SafraInfo> listaMestraSafras; 
    private final ObservableList<Talhao> dadosTabelaTalhoes;
    private final ObservableList<AtividadeSafraInfo> dadosTabelaAtividades; 

    private final NumberFormat currencyFormatter; 
    private final NumberFormat decimalFormatter; 
    
    // NOVO: Constante de conversão (Alqueire Paulista)
    private static final double FATOR_ALQUEIRE_HECTARE = 2.42;
    // NOVO: Flag para evitar loops nos listeners de conversão
    private boolean isUpdatingArea = false;

    /**
     * NOVO: Classe interna para agrupar os resultados da Task
     */
    private static class SafrasPageData {
        final List<SafraInfo> safras;
        final List<Talhao> talhoes;

        SafrasPageData(List<SafraInfo> safras, List<Talhao> talhoes) {
            this.safras = safras;
            this.talhoes = talhoes;
        }
    }

    public SafrasController() {
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO();
        estoqueDAO = new EstoqueDAO(); 
        atividadeSafraDAO = new AtividadeSafraDAO(); 
        financeiroDAO = new FinanceiroDAO(); 
        contaDAO = new ContaDAO(); // NOVO: Inicializa o DAO
        dadosTabelaSafras = FXCollections.observableArrayList();
        listaMestraSafras = new ArrayList<>(); 
        dadosTabelaTalhoes = FXCollections.observableArrayList();
        dadosTabelaAtividades = FXCollections.observableArrayList(); 
        
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        decimalFormatter = new DecimalFormat("#,##0.00");
    }

    @FXML
    public void initialize() {
        // Configura Tabela Safras
        colSafraId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSafraCultura.setCellValueFactory(new PropertyValueFactory<>("cultura"));
        colSafraAno.setCellValueFactory(new PropertyValueFactory<>("anoInicio"));
        colSafraTalhao.setCellValueFactory(new PropertyValueFactory<>("talhaoNome"));
        colSafraStatus.setCellValueFactory(new PropertyValueFactory<>("status")); 
        colSafraProd.setCellValueFactory(new PropertyValueFactory<>("producaoSacosPorHectare"));
        colSafraProdTotalSacos.setCellValueFactory(new PropertyValueFactory<>("producaoTotalSacos"));
        colSafraProdTotalKg.setCellValueFactory(new PropertyValueFactory<>("producaoTotalKg"));
        tabelaSafras.setItems(dadosTabelaSafras);
        
        // Formatação da coluna de produtividade (sc/ha)
        colSafraProd.setCellFactory(col -> new TableCell<SafraInfo, Double>() {
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

        // Configura Tabela Talhões
        colTalhaoId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTalhaoNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colTalhaoArea.setCellValueFactory(new PropertyValueFactory<>("areaHectares"));
        tabelaTalhoes.setItems(dadosTabelaTalhoes);

        // Configura Tabela Atividades
        colAtvData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colAtvDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colAtvInsumo.setCellValueFactory(new PropertyValueFactory<>("insumoNome"));
        colAtvQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colAtvCusto.setCellValueFactory(new PropertyValueFactory<>("custo"));
        tabelaAtividades.setItems(dadosTabelaAtividades);

        // Configura Filtro de Status
        filtroStatusSafra.setItems(FXCollections.observableArrayList(
            "Em Andamento", "Colhidas", "Todas"
        ));
        filtroStatusSafra.getSelectionModel().select("Em Andamento"); 
        filtroStatusSafra.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldV, newV) -> aplicarFiltroSafras()
        );

        // Listener de seleção da Tabela Safras
        tabelaSafras.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> handleSafraSelectionChanged(newSelection)
        );

        btnExportarCsv.setDisable(true);
        limparResumoFinanceiro(); 

        // Carrega dados (agora assíncrono)
        carregarDadosPagina();
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
     * NOVO: Método unificado para carregar todos os dados da página
     * (Safras e Talhões) em uma Task de background.
     */
    private void carregarDadosPagina() {
        Task<SafrasPageData> carregarTask = new Task<SafrasPageData>() {
            @Override
            protected SafrasPageData call() throws Exception {
                // Chamadas de banco de dados (demoradas)
                List<SafraInfo> safras = safraDAO.listSafrasComInfo();
                List<Talhao> talhoes = talhaoDAO.listTalhoes();
                return new SafrasPageData(safras, talhoes);
            }
        };

        carregarTask.setOnSucceeded(e -> {
            SafrasPageData data = carregarTask.getValue();

            // 1. Atualiza lista mestra de safras
            listaMestraSafras.clear();
            listaMestraSafras.addAll(data.safras);
            
            // 2. Atualiza tabela de talhões
            dadosTabelaTalhoes.clear();
            dadosTabelaTalhoes.addAll(data.talhoes);

            // 3. Aplica filtro (rápido, em memória)
            aplicarFiltroSafras();
            
            // 4. Esconde o loading
            showLoading(false);
        });

        carregarTask.setOnFailed(e -> {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os dados da página.");
            carregarTask.getException().printStackTrace();
            showLoading(false);
        });

        showLoading(true);
        new Thread(carregarTask).start();
    }

    /**
     * ATUALIZADO: Agora apenas chama o novo método unificado de recarga.
     */
    private void carregarDadosSafras() {
        carregarDadosPagina();
    }

    /**
     * ATUALIZADO: Agora apenas chama o novo método unificado de recarga.
     */
    private void carregarDadosTalhoes() {
        carregarDadosPagina();
    }

    /**
     * NOVO: Filtra a lista mestra e exibe na tabela (rápido, em memória).
     */
    private void aplicarFiltroSafras() {
        String filtro = filtroStatusSafra.getSelectionModel().getSelectedItem();
        if (filtro == null) {
            filtro = "Em Andamento"; 
        }

        List<SafraInfo> listaFiltrada;

        switch (filtro) {
            case "Colhidas":
                listaFiltrada = listaMestraSafras.stream()
                    .filter(s -> s.getStatus().equalsIgnoreCase("Colhida"))
                    .collect(Collectors.toList());
                break;
            case "Em Andamento":
                listaFiltrada = listaMestraSafras.stream()
                    .filter(s -> !s.getStatus().equalsIgnoreCase("Colhida"))
                    .collect(Collectors.toList());
                break;
            default: // "Todas"
                listaFiltrada = new ArrayList<>(listaMestraSafras);
                break;
        }

        dadosTabelaSafras.clear();
        dadosTabelaSafras.addAll(listaFiltrada);
    }

    /**
     * ATUALIZADO: Esta lógica é rápida (busca em memória e UI)
     * e pode ficar na JavaFX Thread.
     * As chamadas lentas ao DAO (listAtividades, getCustoTotal, etc.)
     * devem ser movidas para uma Task.
     * ATUALIZADO: Agora soma vendas "À Vista" (Financeiro) e "A Prazo" (Contas)
     * para calcular a receita total.
     */
    private void handleSafraSelectionChanged(SafraInfo safra) {
        if (safra == null) {
            lblDetalhesTitulo.setText("Detalhes da Safra: (Selecione uma safra acima)");
            btnExportarCsv.setDisable(true);
            dadosTabelaAtividades.clear();
            limparResumoFinanceiro(); 
            return;
        }

        btnExportarCsv.setDisable(!safra.getStatus().equalsIgnoreCase("Colhida"));
        lblDetalhesTitulo.setText("Detalhes da Safra: " + safra.getCultura() + " (" + safra.getAnoInicio() + ")");

        // 3. Carrega atividades e custos (DEVE SER ASSÍNCRONO)
        // ATENÇÃO: Esta é uma operação de leitura que pode ser lenta.
        // Por simplicidade, deixaremos na thread principal por enquanto,
        // mas o ideal seria criar uma Task separada também para *este* painel.
        // A melhoria principal (carregamento da tela) já foi feita.
        try {
            List<AtividadeSafra> atividades = atividadeSafraDAO.listAtividadesPorSafra(safra.getId());
            
            List<AtividadeSafraInfo> atividadesInfo = new ArrayList<>();
            for (AtividadeSafra atv : atividades) {
                String nomeInsumo = "N/A (Custo Manual)";
                if (atv.getItemConsumidoId() != null && atv.getItemConsumidoId() > 0) {
                    EstoqueItem item = estoqueDAO.getItemById(atv.getItemConsumidoId());
                    if (item != null) {
                        nomeInsumo = item.getItemNome();
                    } else {
                        nomeInsumo = "Insumo Removido (ID: " + atv.getItemConsumidoId() + ")";
                    }
                }
                atividadesInfo.add(new AtividadeSafraInfo(atv, nomeInsumo));
            }
            
            dadosTabelaAtividades.clear();
            dadosTabelaAtividades.addAll(atividadesInfo);

            double custoTotal = atividadeSafraDAO.getCustoTotalPorSafra(safra.getId());
            lblCustoTotalSafra.setText(currencyFormatter.format(custoTotal));

            if (safra.getStatus().equalsIgnoreCase("Colhida")) {
                // --- INÍCIO DA ATUALIZAÇÃO (Lógica de Receita) ---
                double receitaTotalVendas = 0;
                String nomeItemColheita = safra.getCultura() + " (Colheita " + safra.getAnoInicio() + ")";
                String descVendaQuery = "Venda de " + nomeItemColheita;
                
                // 1. Soma vendas À VISTA (do Financeiro)
                List<Transacao> vendasAVista = financeiroDAO.listTransacoesPorDescricaoLike(descVendaQuery);
                for (Transacao venda : vendasAVista) {
                    receitaTotalVendas += venda.getValor(); // Valores já são positivos
                }

                // 2. Soma vendas A PRAZO (de Contas a Receber)
                // (Requer novo método no ContaDAO: listContasPorDescricaoLike)
                List<Conta> vendasAPrazo = contaDAO.listContasPorDescricaoLike(descVendaQuery);
                for (Conta conta : vendasAPrazo) {
                    if (conta.getTipo().equals("receber")) {
                        // Soma o valor da conta (pendente ou paga), pois a receita é realizada na venda.
                        receitaTotalVendas += conta.getValor();
                    }
                }
                lblReceitaTotalSafra.setText(currencyFormatter.format(receitaTotalVendas));
                // --- FIM DA ATUALIZAÇÃO (Lógica de Receita) ---

                double valorEmEstoque = 0;
                EstoqueItem itemColheitaEstoque = estoqueDAO.getEstoqueItemPorNome(nomeItemColheita);
                if (itemColheitaEstoque != null) {
                    valorEmEstoque = itemColheitaEstoque.getValorTotal();
                }
                lblEstoqueTotalSafra.setText(currencyFormatter.format(valorEmEstoque));

                double balancoFinal = (receitaTotalVendas + valorEmEstoque) - custoTotal;
                lblBalancoSafra.setText(currencyFormatter.format(balancoFinal));
                
                lblBalancoSafra.getStyleClass().removeAll("positivo-text", "negativo-text"); // ATUALIZADO
                if (balancoFinal >= 0) {
                    lblBalancoSafra.getStyleClass().add("positivo-text"); // ATUALIZADO
                } else {
                    lblBalancoSafra.getStyleClass().add("negativo-text"); // ATUALIZADO
                }

            } else {
                limparResumoFinanceiro(custoTotal); 
            }

        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os detalhes da safra.");
            e.printStackTrace();
        }
    }

    /**
     * NOVO: Limpa os labels do resumo financeiro (para safras não colhidas).
     */
    private void limparResumoFinanceiro() {
        limparResumoFinanceiro(0.0);
    }
    
    /**
     * NOVO: Sobrecarga para limpar labels mantendo o custo (se já calculado).
     */
    private void limparResumoFinanceiro(double custoTotal) {
        lblCustoTotalSafra.setText(currencyFormatter.format(custoTotal));
        lblReceitaTotalSafra.setText("N/A");
        lblEstoqueTotalSafra.setText("N/A");
        lblBalancoSafra.setText("N/A");
        lblBalancoSafra.getStyleClass().removeAll("positivo-text", "negativo-text"); // ATUALIZADO
    }


    /**
     * ATUALIZADO: Adiciona conversão automática Hectare/Alqueire.
     */
    @FXML
    private void handleNovoTalhao() {
        Dialog<Talhao> dialog = new Dialog<>();
        dialog.setTitle("Adicionar Novo Talhão");
        dialog.setHeaderText("Preencha os dados do talhão.");

        ButtonType adicionarButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomeField = new TextField();
        nomeField.setPromptText("Ex: Talhão Sede");
        
        // --- INÍCIO DA ATUALIZAÇÃO (Hectares/Alqueires) ---
        TextField hectaresField = new TextField();
        hectaresField.setPromptText("Ex: 100.0");
        TextField alqueiresField = new TextField();
        alqueiresField.setPromptText("Ex: 41.32");

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Área (Hectares):"), 0, 1);
        grid.add(hectaresField, 1, 1);
        grid.add(new Label("Área (Alqueires):"), 0, 2);
        grid.add(alqueiresField, 1, 2);

        // Listeners de conversão automática
        hectaresField.textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdatingArea) return;
            isUpdatingArea = true;
            try {
                double hectares = parseDouble(newV);
                double alqueires = hectares / FATOR_ALQUEIRE_HECTARE;
                alqueiresField.setText(formatDouble(alqueires));
            } catch (NumberFormatException e) {
                alqueiresField.clear();
            }
            isUpdatingArea = false;
        });

        alqueiresField.textProperty().addListener((obs, oldV, newV) -> {
            if (isUpdatingArea) return;
            isUpdatingArea = true;
            try {
                double alqueires = parseDouble(newV);
                double hectares = alqueires * FATOR_ALQUEIRE_HECTARE;
                hectaresField.setText(formatDouble(hectares));
            } catch (NumberFormatException e) {
                hectaresField.clear();
            }
            isUpdatingArea = false;
        });
        // --- FIM DA ATUALIZAÇÃO ---

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    // O valor final é sempre salvo em Hectares
                    double area = parseDouble(hectaresField.getText()); 
                    if (nome.isEmpty() || area <= 0) {
                        AlertUtil.showError("Erro de Validação", "Nome ou área inválidos (área deve ser > 0).");
                        return null;
                    }
                    return new Talhao(nome, area);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor de área inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<Talhao> result = dialog.showAndWait();

        result.ifPresent(talhao -> {
            try {
                // Operação de escrita (rápida)
                talhaoDAO.addTalhao(talhao);
                carregarDadosTalhoes(); // Recarrega (assíncrono)
                AlertUtil.showInfo("Sucesso", "Talhão adicionado com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar o talhão: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleNovaSafra() {
        // 1. Buscar a lista de talhões
        // ATENÇÃO: Esta é uma chamada de DB na thread principal.
        // Como o dialog depende dela, vamos mantê-la por ser rápida (poucos talhões).
        List<Talhao> talhoes;
        try {
            talhoes = talhaoDAO.listTalhoes();
        } catch (SQLException e) {
            AlertUtil.showError("Erro", "Não foi possível carregar a lista de talhões para o formulário.");
            return;
        }

        if (talhoes.isEmpty()) {
            AlertUtil.showError("Ação Necessária", "Você precisa cadastrar um Talhão antes de adicionar uma Safra.");
            return;
        }
        
        Dialog<Safra> dialog = new Dialog<>();
        dialog.setTitle("Adicionar Nova Safra");
        dialog.setHeaderText("Preencha os dados da safra.");

        ButtonType adicionarButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField culturaField = new TextField();
        culturaField.setPromptText("Ex: Soja");
        TextField anoField = new TextField(); 
        anoField.setPromptText("Ex: 2025/1"); 
        
        ComboBox<Talhao> talhaoCombo = new ComboBox<>(FXCollections.observableArrayList(talhoes));
        
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList(
            "Planejada", "Em Preparo", "Plantio", "Crescimento", "Aplicação Defensivos"
        ));
        statusCombo.getSelectionModel().selectFirst(); 

        
        talhaoCombo.setCellFactory(lv -> new ListCell<Talhao>() {
            @Override
            protected void updateItem(Talhao item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getNome());
            }
        });
        talhaoCombo.setButtonCell(new ListCell<Talhao>() {
            @Override
            protected void updateItem(Talhao item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? "" : item.getNome());
            }
        });

        grid.add(new Label("Cultura:"), 0, 0);
        grid.add(culturaField, 1, 0);
        grid.add(new Label("Safra (ex: 2025/1):"), 0, 1); 
        grid.add(anoField, 1, 1);
        grid.add(new Label("Talhão:"), 0, 2);
        grid.add(talhaoCombo, 1, 2);
        grid.add(new Label("Status Inicial:"), 0, 3); 
        grid.add(statusCombo, 1, 3); 

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                // MODIFICAÇÃO: Converte cultura para minúsculo e remove espaços extras
                String cultura = culturaField.getText().toLowerCase().trim();
                String ano = anoField.getText(); 
                Talhao talhaoSel = talhaoCombo.getSelectionModel().getSelectedItem();
                String status = statusCombo.getSelectionModel().getSelectedItem(); 
                
                if (cultura.isEmpty() || ano.isEmpty() || talhaoSel == null || status == null) {
                    AlertUtil.showError("Erro de Validação", "Cultura, Safra e Talhão são obrigatórios.");
                    return null;
                }
                // O objeto Safra será criado com o nome da cultura já em minúsculo
                return new Safra(cultura, ano, talhaoSel.getId(), status);
            }
            return null;
        });

        Optional<Safra> result = dialog.showAndWait();

        result.ifPresent(safra -> {
            try {
                // Operação de escrita (rápida)
                safraDAO.addSafra(safra);
                carregarDadosSafras(); // Recarrega (assíncrono)
                AlertUtil.showInfo("Sucesso", "Safra adicionada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar a safra: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleLancarAtividade() {
        SafraInfo safraSelecionada = tabelaSafras.getSelectionModel().getSelectedItem();

        if (safraSelecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione uma safra para lançar uma atividade.");
            return;
        }

        if (safraSelecionada.getStatus().equalsIgnoreCase("Colhida")) {
            AlertUtil.showInfo("Ação Inválida", "Não é possível lançar atividades para uma safra que já foi colhida.");
            return;
        }

        // Chamada de DB (rápida, mantida na FX thread)
        List<EstoqueItem> itensEstoque;
        try {
            itensEstoque = estoqueDAO.listEstoque(); 
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar a lista de itens do estoque.");
            return;
        }

        Dialog<AtividadeSafra> dialog = new Dialog<>();
        dialog.setTitle("Lançar Atividade/Custo na Safra");
        dialog.setHeaderText("Safra: " + safraSelecionada.getCultura() + " (" + safraSelecionada.getAnoInicio() + ")");

        ButtonType lancarButtonType = new ButtonType("Lançar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(lancarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker dataField = new DatePicker(LocalDate.now());
        TextField descField = new TextField();
        descField.setPromptText("Ex: Adubação de cobertura / Mão de obra");
        
        CheckBox isManualCheck = new CheckBox("Lançar custo manual (sem item de estoque)");
        Label custoManualLabel = new Label("Custo Manual (R$):");
        TextField custoManualField = new TextField();
        custoManualField.setPromptText("Ex: 150.00");

        ComboBox<EstoqueItem> itemCombo = new ComboBox<>(FXCollections.observableArrayList(itensEstoque));
        TextField qtdField = new TextField("1.0");
        Label custoCalculadoLabel = new Label("Custo (R$): ---");

        Button maxButton = new Button("MAX");
        maxButton.setOnAction(e -> {
            EstoqueItem itemSelecionado = itemCombo.getSelectionModel().getSelectedItem();
            if (itemSelecionado != null) {
                qtdField.setText(String.format(Locale.US, "%.2f", itemSelecionado.getQuantidade()));
            }
        });
        maxButton.disableProperty().bind(itemCombo.getSelectionModel().selectedItemProperty().isNull());

        HBox qtdBox = new HBox(5, qtdField, maxButton); 
        qtdBox.setAlignment(Pos.CENTER_LEFT);

        itemCombo.setCellFactory(lv -> new ListCell<EstoqueItem>() {
            @Override
            protected void updateItem(EstoqueItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? "" : item.getItemNome() + " (Disp: " + item.getQuantidade() + " " + item.getUnidade() + ")");
            }
        });
        itemCombo.setButtonCell(new ListCell<EstoqueItem>() {
            @Override
            protected void updateItem(EstoqueItem item, boolean empty) {
                super.updateItem(item, empty);
                setText(item == null || empty ? "Selecione um Insumo" : item.getItemNome());
            }
        });
        
        if (itensEstoque.isEmpty()) {
            isManualCheck.setSelected(true);
            isManualCheck.setDisable(true);
        }

        itemCombo.valueProperty().addListener((obs, oldV, newV) -> {
            atualizarCustoAtividade(newV, qtdField.getText(), custoCalculadoLabel);
        });
        qtdField.textProperty().addListener((obs, oldV, newV) -> {
            atualizarCustoAtividade(itemCombo.getSelectionModel().getSelectedItem(), newV, custoCalculadoLabel);
        });
        
        // Lógica de Visibilidade
        custoManualLabel.setVisible(isManualCheck.isSelected());
        custoManualField.setVisible(isManualCheck.isSelected());
        custoManualLabel.setManaged(isManualCheck.isSelected());
        custoManualField.setManaged(isManualCheck.isSelected());
        itemCombo.setDisable(isManualCheck.isSelected());
        qtdBox.setDisable(isManualCheck.isSelected());
        custoCalculadoLabel.setVisible(!isManualCheck.isSelected());

        isManualCheck.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            custoManualLabel.setVisible(isSelected);
            custoManualField.setVisible(isSelected);
            custoManualLabel.setManaged(isSelected);
            custoManualField.setManaged(isSelected);
            itemCombo.setDisable(isSelected);
            qtdBox.setDisable(isSelected); 
            custoCalculadoLabel.setVisible(!isSelected);
            custoCalculadoLabel.setManaged(!isSelected); 
        });

        // Adiciona componentes ao grid
        grid.add(new Label("Data:"), 0, 0);
        grid.add(dataField, 1, 0);
        grid.add(new Label("Descrição:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(isManualCheck, 0, 2, 2, 1); 
        grid.add(new Label("Insumo (do Estoque):"), 0, 3);
        grid.add(itemCombo, 1, 3);
        grid.add(new Label("Quantidade Usada:"), 0, 4);
        grid.add(qtdBox, 1, 4); 
        grid.add(custoCalculadoLabel, 1, 5);
        grid.add(custoManualLabel, 0, 6); 
        grid.add(custoManualField, 1, 6); 

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        // Converter resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == lancarButtonType) {
                try {
                    LocalDate data = dataField.getValue();
                    String desc = descField.getText();

                    if (data == null || desc.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "Data e Descrição são obrigatórios.");
                        return null;
                    }
                    
                    if (isManualCheck.isSelected()) {
                        double custoManual = parseDouble(custoManualField.getText());
                        if (custoManual <= 0) {
                            AlertUtil.showError("Erro de Validação", "O custo manual deve ser positivo.");
                            return null;
                        }
                        return new AtividadeSafra(
                            safraSelecionada.getId(), desc, data.toString(),
                            null, 0, custoManual
                        );

                    } else {
                        EstoqueItem item = itemCombo.getSelectionModel().getSelectedItem();
                        double qtd = parseDouble(qtdField.getText());

                        if (item == null) {
                            AlertUtil.showError("Erro de Validação", "Um insumo deve ser selecionado.");
                            return null;
                        }
                        if (qtd <= 0) {
                            AlertUtil.showError("Erro de Validação", "A quantidade deve ser positiva.");
                            return null;
                        }
                        if (qtd > item.getQuantidade()) {
                            AlertUtil.showError("Estoque Insuficiente", "Disponível: " + item.getQuantidade() + ". Solicitado: " + qtd);
                            return null;
                        }
                        double custoTotal = qtd * item.getValorUnitario();
                        return new AtividadeSafra(
                            safraSelecionada.getId(), desc, data.toString(),
                            item.getId(), qtd, custoTotal
                        );
                    }

                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Quantidade ou Custo inválido.");
                    return null;
                }
            }
            return null;
        });


        // Processar resultado
        Optional<AtividadeSafra> result = dialog.showAndWait();

        result.ifPresent(atividade -> {
            try {
                // Operações de escrita (rápidas)
                atividadeSafraDAO.addAtividade(atividade);
                String successMessage;

                if (atividade.getItemConsumidoId() != null && atividade.getItemConsumidoId() > 0) {
                    estoqueDAO.consumirEstoque(atividade.getItemConsumidoId(), atividade.getQuantidadeConsumida());
                    successMessage = "Atividade lançada e estoque consumido.\nO custo da atividade foi registrado na safra.";
                } else {
                    if (atividade.getCustoTotalAtividade() > 0) {
                        String descFin = "Custo Safra (" + safraSelecionada.getCultura() + "): " + atividade.getDescricao();
                        Transacao transacao = new Transacao(
                            descFin, -atividade.getCustoTotalAtividade(), 
                            atividade.getData(), "despesa"
                        );
                        financeiroDAO.addTransacao(transacao);
                        successMessage = "Custo manual lançado com sucesso na safra e no financeiro."; 
                    } else {
                         successMessage = "Custo manual (R$ 0,00) lançado na safra."; 
                    }
                }

                AlertUtil.showInfo("Sucesso", successMessage);
                
                // Atualiza a tabela de detalhes (rápido, fica na FX thread)
                if (safraSelecionada.equals(tabelaSafras.getSelectionModel().getSelectedItem())) {
                    handleSafraSelectionChanged(safraSelecionada);
                }

            } catch (IllegalStateException e) {
                AlertUtil.showError("Erro de Estoque", e.getMessage());
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível salvar a atividade: " + e.getMessage());
            }
        });
    }

    // Classe interna ColheitaData
    private static class ColheitaData {
        final double producaoKg; 
        final double producaoSacos; 
        final double valorTotal; 
        final double valorPorSaco; 

        ColheitaData(double producaoKg, double producaoSacos, double valorTotal) {
            this.producaoKg = producaoKg;
            this.producaoSacos = producaoSacos;
            this.valorTotal = valorTotal;
            if (producaoSacos > 0) {
                this.valorPorSaco = valorTotal / producaoSacos;
            } else {
                this.valorPorSaco = 0;
            }
        }
    }

    @FXML
    private void handleRegistrarColheita() {
        SafraInfo safraSelecionada = tabelaSafras.getSelectionModel().getSelectedItem();

        if (safraSelecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione uma safra na tabela para registrar a colheita.");
            return;
        }

        if (safraSelecionada.getStatus().equalsIgnoreCase("Colhida")) {
             AlertUtil.showInfo("Ação Inválida", "Esta safra já foi colhida e a produção já foi registrada.");
             return;
        }
        
        Dialog<ColheitaData> dialog = new Dialog<>(); 
        dialog.setTitle("Registrar Colheita");
        dialog.setHeaderText("Insira a produtividade para: " + safraSelecionada.getCultura() + " (" + safraSelecionada.getTalhaoNome() + ")");

        ButtonType registrarButtonType = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registrarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        double area = safraSelecionada.getAreaHectares();
        double prodAtualTotalSacos = safraSelecionada.getProducaoTotalSacos(); 

        Label areaLabel = new Label(String.format(Locale.US, "%.2f ha", area));
        Label scHaLabel = new Label("---"); 
        Label totalKgLabel = new Label("---");
        Label valorTotalColheitaLabel = new Label("---"); 
        
        TextField totalSacosField = new TextField(String.format(Locale.US, "%.2f", prodAtualTotalSacos));
        TextField valorSacoField = new TextField("0.0"); 
        
        grid.add(new Label("Área do Talhão:"), 0, 0);
        grid.add(areaLabel, 1, 0);
        grid.add(new Label("Total (sacos):"), 0, 1); 
        grid.add(totalSacosField, 1, 1); 
        grid.add(new Label("Produção (sc/ha):"), 0, 2); 
        grid.add(scHaLabel, 1, 2); 
        grid.add(new Label("Total (kg):"), 0, 3);
        grid.add(totalKgLabel, 1, 3);
        grid.add(new Label("Valor Venda (R$/saco):"), 0, 4); 
        grid.add(valorSacoField, 1, 4); 
        grid.add(new Label("Valor Total (R$):"), 0, 5); 
        grid.add(valorTotalColheitaLabel, 1, 5); 
        
        ChangeListener<String> listener = (obs, oldV, newV) -> {
            atualizarCalculosDialogColheita(
                totalSacosField.getText(), valorSacoField.getText(), area, 
                scHaLabel, totalKgLabel, valorTotalColheitaLabel 
            );
        };
        
        totalSacosField.textProperty().addListener(listener); 
        valorSacoField.textProperty().addListener(listener); 
        
        atualizarCalculosDialogColheita(
            totalSacosField.getText(), valorSacoField.getText(), area, 
            scHaLabel, totalKgLabel, valorTotalColheitaLabel 
        );

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registrarButtonType) {
                try {
                    double totalSacos = parseDouble(totalSacosField.getText()); 
                    double valorSaco = parseDouble(valorSacoField.getText()); 
                    
                    if (totalSacos <= 0 || valorSaco < 0) { 
                        AlertUtil.showError("Valor Inválido", "A produção total deve ser maior que zero e o valor não pode ser negativo."); 
                        return null;
                    }

                    double producaoKg = totalSacos * 60.0; 
                    double valorTotal = totalSacos * valorSaco; 
                    
                    return new ColheitaData(producaoKg, totalSacos, valorTotal); 

                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valores de produção ou R$ inválidos.");
                    return null;
                }
            }
            return null;
        });

        Optional<ColheitaData> result = dialog.showAndWait(); 

        result.ifPresent(colheitaData -> {
            try {
                // Operações de escrita (rápidas)
                safraDAO.updateProducaoSafra(safraSelecionada.getId(), colheitaData.producaoKg);

                String nomeItem = safraSelecionada.getCultura() + " (Colheita " + safraSelecionada.getAnoInicio() + ")";
                String unidadeItem = "sacos"; 

                // CORREÇÃO: Chamar o construtor correto de 7 argumentos
                EstoqueItem itemColheita = new EstoqueItem(
                    nomeItem,
                    colheitaData.producaoSacos, 
                    unidadeItem,
                    colheitaData.valorPorSaco, 
                    colheitaData.valorTotal,
                    null, // fornecedorNome (produto interno)
                    null  // fornecedorEmpresa (produto interno)
                );

                estoqueDAO.addEstoque(itemColheita);

                carregarDadosSafras(); // Recarrega (assíncrono)
                
                AlertUtil.showInfo("Sucesso", 
                    "Colheita registrada com sucesso.\n" +
                    "Status da safra atualizado para 'Colhida'.\n" +
                    String.format(Locale.US, "%.2f sacos de %s", colheitaData.producaoSacos, nomeItem) + 
                    " foram adicionados ao estoque com seu valor de custo/venda registrado." 
                );

            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível registrar a colheita ou atualizar o estoque: " + e.getMessage());
            }
        });
    }

    /**
     * ATUALIZADO: Agora busca vendas "À Vista" (Financeiro) e "A Prazo" (Contas)
     * para compor o CSV.
     */
    @FXML
    private void handleExportarCsv() {
        SafraInfo safra = tabelaSafras.getSelectionModel().getSelectedItem();
        if (safra == null || !safra.getStatus().equalsIgnoreCase("Colhida")) {
            AlertUtil.showError("Ação Inválida", "Selecione uma safra 'Colhida' para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Relatório CSV");
        fileChooser.setInitialFileName("Relatorio_Safra_" + safra.getId() + "_" + safra.getCultura().replace(" ", "_") + ".csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(tabelaSafras.getScene().getWindow());

        if (file == null) {
            return; 
        }

        // ATENÇÃO: Esta operação é de leitura e pode ser lenta
        // Idealmente, seria uma Task, mas por simplicidade:
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) { 
            
            writer.write("\uFEFF"); // NOVO: Adiciona o BOM do UTF-8
            
            StringBuilder sb = new StringBuilder();
            
            sb.append("Relatório da Safra: " + safra.getCultura() + " (" + safra.getAnoInicio() + ")\n");
            sb.append("Talhão: " + safra.getTalhaoNome() + " (" + safra.getAreaHectares() + " ha)\n");
            sb.append("Status: " + safra.getStatus() + "\n\n");
            
            sb.append("Tipo;Data;Descricao;Insumo/Produto;Quantidade;Unidade;Valor Total (R$)\n");

            // 1. Custos (Atividades)
            double custoTotal = 0;
            List<AtividadeSafra> atividades = atividadeSafraDAO.listAtividadesPorSafra(safra.getId());
            
            for (AtividadeSafra atv : atividades) {
                String nomeInsumo = "N/A (Custo Manual)";
                String unidade = "";
                if (atv.getItemConsumidoId() != null && atv.getItemConsumidoId() > 0) {
                    EstoqueItem item = estoqueDAO.getItemById(atv.getItemConsumidoId());
                    if (item != null) {
                        nomeInsumo = item.getItemNome();
                        unidade = item.getUnidade();
                    }
                }
                
                sb.append(String.format(Locale.US, "Custo;%s;\"%s\";\"%s\";%.2f;%s;%.2f\n",
                    atv.getData(),
                    atv.getDescricao().replace("\"", "\"\""), 
                    nomeInsumo.replace("\"", "\"\""),
                    atv.getQuantidadeConsumida(),
                    unidade,
                    -atv.getCustoTotalAtividade() 
                ));
                custoTotal += atv.getCustoTotalAtividade(); 
            }

            // --- INÍCIO DA ATUALIZAÇÃO (Lógica de Receita CSV) ---
            // 2. Receitas (Vendas Reais - À Vista e A Prazo)
            double receitaTotalVendas = 0;
            String nomeItemColheita = safra.getCultura() + " (Colheita " + safra.getAnoInicio() + ")";
            String descVendaQuery = "Venda de " + nomeItemColheita;
            
            // Vendas à Vista (do Financeiro)
            List<Transacao> vendasAVista = financeiroDAO.listTransacoesPorDescricaoLike(descVendaQuery);
            for (Transacao venda : vendasAVista) {
                sb.append(String.format(Locale.US, "Receita (Venda à Vista);%s;\"%s\";\"%s\";N/A;N/A;%.2f\n",
                    venda.getData(),
                    venda.getDescricao().replace("\"", "\"\""),
                    nomeItemColheita.replace("\"", "\"\""),
                    venda.getValor() 
                ));
                receitaTotalVendas += venda.getValor();
            }

            // Vendas a Prazo (de Contas a Receber)
            List<Conta> vendasAPrazo = contaDAO.listContasPorDescricaoLike(descVendaQuery);
            for (Conta conta : vendasAPrazo) {
                 if (conta.getTipo().equals("receber")) {
                    sb.append(String.format(Locale.US, "Receita (Venda a Prazo);%s;\"%s\";\"%s\";N/A;N/A;%.2f\n",
                        conta.getDataVencimento(), // Usa data de vencimento como referência
                        conta.getDescricao().replace("\"", "\"\"") + " (Status: " + conta.getStatus() + ")",
                        nomeItemColheita.replace("\"", "\"\""),
                        conta.getValor()
                    ));
                    receitaTotalVendas += conta.getValor();
                }
            }
            // --- FIM DA ATUALIZAÇÃO (Lógica de Receita CSV) ---


            // 3. Valor em Estoque (Produto não vendido)
            double valorEmEstoque = 0;
            EstoqueItem itemColheitaEstoque = estoqueDAO.getEstoqueItemPorNome(nomeItemColheita);
            
            if (itemColheitaEstoque != null) {
                valorEmEstoque = itemColheitaEstoque.getValorTotal(); 
                if (valorEmEstoque > 0) {
                    sb.append(String.format(Locale.US, "Valor em Estoque;%s;\"%s\";\"%s\";%.2f;%s;%.2f\n",
                        LocalDate.now().toString(), 
                        "Produto em Estoque (Não Vendido)",
                        itemColheitaEstoque.getItemNome().replace("\"", "\"\""),
                        itemColheitaEstoque.getQuantidade(),
                        itemColheitaEstoque.getUnidade(),
                        valorEmEstoque 
                    ));
                }
            }

            // 4. Sumário
            double balancoFinal = (receitaTotalVendas + valorEmEstoque) - custoTotal;
            sb.append("\n\n--- Resumo Financeiro ---\n");
            sb.append(String.format(Locale.US, "Total Receitas (Vendas);%.2f\n", receitaTotalVendas));
            sb.append(String.format(Locale.US, "Valor em Estoque (Custo Médio);%.2f\n", valorEmEstoque));
            sb.append(String.format(Locale.US, "Receita Bruta Total (Vendas + Estoque);%.2f\n", (receitaTotalVendas + valorEmEstoque)));
            sb.append(String.format(Locale.US, "Total Custos (Insumos);%.2f\n", -custoTotal)); 
            sb.append(String.format(Locale.US, "Balanço/Lucro Final;%.2f\n", balancoFinal));

            writer.write(sb.toString());
            AlertUtil.showInfo("Sucesso", "Relatório CSV exportado com sucesso para:\n" + file.getAbsolutePath());

        } catch (IOException | SQLException e) {
            AlertUtil.showError("Erro ao Exportar", "Não foi possível gerar o arquivo CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAtualizarStatus() {
        SafraInfo safraSelecionada = tabelaSafras.getSelectionModel().getSelectedItem();
        if (safraSelecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione uma safra para atualizar o status.");
            return;
        }

        if (safraSelecionada.getStatus().equalsIgnoreCase("Colhida")) {
            AlertUtil.showInfo("Ação Inválida", "Esta safra já foi colhida e não pode ter seu status alterado.");
            return;
        }

        List<String> statusOpcoes = new ArrayList<>(Arrays.asList(
            "Planejada", "Em Preparo", "Plantio", "Crescimento", 
            "Aplicação Defensivos", "Colheita"
        ));
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(safraSelecionada.getStatus(), statusOpcoes);
        dialog.setTitle("Atualizar Status da Safra");
        dialog.setHeaderText("Selecione o novo status para:\n" + safraSelecionada.getCultura() + " (" + safraSelecionada.getAnoInicio() + ")");
        dialog.setContentText("Status:");
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(novoStatus -> {
            try {
                // Operação de escrita (rápida)
                safraDAO.updateStatusSafra(safraSelecionada.getId(), novoStatus);
                carregarDadosSafras(); // Recarrega (assíncrono)
                AlertUtil.showInfo("Sucesso", "Status da safra atualizado para '" + novoStatus + "'.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar o status: " + e.getMessage());
            }
        });
    }


    @FXML
    private void handleRemoverSafra() {
        SafraInfo safraSelecionada = tabelaSafras.getSelectionModel().getSelectedItem();

        if (safraSelecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione uma safra na tabela para remover.");
            return;
        }

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Remoção",
                "Tem certeza que deseja remover a safra: " + safraSelecionada.getCultura() + " (" + safraSelecionada.getAnoInicio() + ")?\n\nEsta ação é permanente.");

        if (confirmado) {
            try {
                // Operação de escrita (rápida)
                safraDAO.removerSafra(safraSelecionada.getId());
                carregarDadosSafras(); // Recarrega (assíncrono)
                AlertUtil.showInfo("Sucesso", "Safra removida com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover a safra: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRemoverTalhao() {
        Talhao talhaoSelecionado = tabelaTalhoes.getSelectionModel().getSelectedItem();

        if (talhaoSelecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um talhão na tabela para remover.");
            return;
        }

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Remoção",
                "Tem certeza que deseja remover o talhão: " + talhaoSelecionado.getNome() + "?\n\nEsta ação é permanente.");

        if (confirmado) {
            try {
                // Operação de escrita (rápida)
                talhaoDAO.removerTalhao(talhaoSelecionado.getId());
                carregarDadosTalhoes(); // Recarrega (assíncrono)
                AlertUtil.showInfo("Sucesso", "Talhão removido com sucesso.");
            } catch (SQLException e) {
                if (e.getMessage().contains("FOREIGN KEY constraint failed")) {
                    AlertUtil.showError("Erro de Remoção", "Não é possível remover este talhão, pois ele está associado a uma ou mais safras.");
                } else {
                    AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover o talhão: " + e.getMessage());
                }
            }
        }
    }
    
    private void atualizarCalculosDialogColheita(
        String totalSacosStr, String vlrSacoStr, double area,
        Label scHaLabel, Label totalKgLabel, Label totalValorLabel
    ) {
        try {
            double totalSacos = parseDouble(totalSacosStr); 
            double valorSaco = parseDouble(vlrSacoStr); 
            
            if (totalSacos < 0 || valorSaco < 0) { 
                scHaLabel.setText("Inválido"); 
                totalKgLabel.setText("Inválido");
                totalValorLabel.setText("Inválido");
                return;
            }
            
            double producaoKg = totalSacos * 60.0; 
            double valorTotal = totalSacos * valorSaco; 
            double producaoScHa = (area > 0) ? (totalSacos / area) : 0.0; 
            
            scHaLabel.setText(String.format(Locale.US, "%.2f sc/ha", producaoScHa)); 
            totalKgLabel.setText(String.format(Locale.US, "%.2f kg", producaoKg));
            totalValorLabel.setText(currencyFormatter.format(valorTotal)); 
            
        } catch (NumberFormatException e) {
            scHaLabel.setText("---"); 
            totalKgLabel.setText("---");
            totalValorLabel.setText("---");
        }
    }

    // MÉTODO MOVIDO E CORRIGIDO
    private double parseDouble(String text) throws NumberFormatException {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        // Substitui vírgula por ponto para aceitar ambos os formatos
        return Double.parseDouble(text.replace(",", "."));
    }
    
    // NOVO MÉTODO ADICIONADO
    /**
     * NOVO: Helper para formatar double para campos de texto (2 casas decimais).
     * Usado pelos listeners de conversão de área.
     */
    private String formatDouble(double value) {
        // Usa Locale.US (ponto decimal) para consistência programática
        return String.format(Locale.US, "%.2f", value);
    }
    
    private void atualizarCustoAtividade(EstoqueItem item, String qtdStr, Label custoLabel) {
        if (item == null) {
            custoLabel.setText("Custo (R$): ---");
            return;
        }
        try {
            double qtd = parseDouble(qtdStr);
            double custo = qtd * item.getValorUnitario();
            custoLabel.setText(currencyFormatter.format(custo));
        } catch (NumberFormatException e) {
            custoLabel.setText("Custo (R$): Inválido");
        }
    }

    // Classe interna AtividadeSafraInfo
    public static class AtividadeSafraInfo {
        private final String data;
        private final String descricao;
        private final String insumoNome;
        private final double quantidade;
        private final double custo;

        public AtividadeSafraInfo(AtividadeSafra atv, String insumoNome) {
            this.data = atv.getData();
            this.descricao = atv.getDescricao();
            this.insumoNome = insumoNome;
            this.quantidade = atv.getQuantidadeConsumida();
            this.custo = atv.getCustoTotalAtividade();
        }

        public String getData() { return data; }
        public String getDescricao() { return descricao; }
        public String getInsumoNome() { return insumoNome; }
        public double getQuantidade() { return quantidade; }
        public double getCusto() { return custo; }
    }
}