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
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox; 
import javafx.beans.value.ChangeListener; // <-- IMPORTAÇÃO ADICIONADA
import javafx.beans.value.ObservableValue;
import javafx.stage.FileChooser; 
import java.io.File; 
import java.io.IOException; 
import java.io.PrintWriter; 
import java.sql.SQLException;
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
 */
public class SafrasController {

    // Tabela Safras
    @FXML
    private TableView<SafraInfo> tabelaSafras;
    @FXML
    private TableColumn<SafraInfo, Integer> colSafraId;
    @FXML
    private TableColumn<SafraInfo, String> colSafraCultura;
    @FXML
    private TableColumn<SafraInfo, String> colSafraAno; // Alterado de Integer
    @FXML
    private TableColumn<SafraInfo, String> colSafraTalhao;
    @FXML
    private TableColumn<SafraInfo, String> colSafraStatus; // NOVO
    @FXML
    private TableColumn<SafraInfo, Double> colSafraProd;
    @FXML
    private TableColumn<SafraInfo, Double> colSafraProdTotalSacos;
    @FXML
    private TableColumn<SafraInfo, Double> colSafraProdTotalKg; 

    // Tabela Talhões
    @FXML
    private TableView<Talhao> tabelaTalhoes;
    @FXML
    private TableColumn<Talhao, Integer> colTalhaoId;
    @FXML
    private TableColumn<Talhao, String> colTalhaoNome;
    @FXML
    private TableColumn<Talhao, Double> colTalhaoArea;

    // NOVO: Filtro de Status
    @FXML
    private ComboBox<String> filtroStatusSafra;

    // NOVO: Painel de Detalhes
    @FXML
    private Label lblDetalhesTitulo;
    @FXML
    private TableView<AtividadeSafraInfo> tabelaAtividades; // DTO Customizado
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
    
    // ATUALIZADO: Labels de resumo (alguns novos)
    @FXML
    private Label lblCustoTotalSafra;
    @FXML
    private Label lblReceitaTotalSafra;
    @FXML
    private Label lblEstoqueTotalSafra;
    @FXML
    private Label lblBalancoSafra;
    
    @FXML
    private Button btnExportarCsv; // NOVO

    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO;
    private final EstoqueDAO estoqueDAO; // NOVO
    private final AtividadeSafraDAO atividadeSafraDAO; // NOVO
    private final FinanceiroDAO financeiroDAO; // NOVO
    
    private final ObservableList<SafraInfo> dadosTabelaSafras; // Lista visível na tabela
    private List<SafraInfo> listaMestraSafras; // Lista com todos os dados do banco
    private final ObservableList<Talhao> dadosTabelaTalhoes;
    private final ObservableList<AtividadeSafraInfo> dadosTabelaAtividades; // NOVO

    private final NumberFormat currencyFormatter; // NOVO

    public SafrasController() {
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO();
        estoqueDAO = new EstoqueDAO(); // NOVO
        atividadeSafraDAO = new AtividadeSafraDAO(); // NOVO
        financeiroDAO = new FinanceiroDAO(); // NOVO
        dadosTabelaSafras = FXCollections.observableArrayList();
        listaMestraSafras = new ArrayList<>(); // NOVO
        dadosTabelaTalhoes = FXCollections.observableArrayList();
        dadosTabelaAtividades = FXCollections.observableArrayList(); // NOVO
        
        // NOVO: Configura o formatador de moeda
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    @FXML
    public void initialize() {
        // Configura Tabela Safras
        colSafraId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSafraCultura.setCellValueFactory(new PropertyValueFactory<>("cultura"));
        colSafraAno.setCellValueFactory(new PropertyValueFactory<>("anoInicio"));
        colSafraTalhao.setCellValueFactory(new PropertyValueFactory<>("talhaoNome"));
        colSafraStatus.setCellValueFactory(new PropertyValueFactory<>("status")); // NOVO
        colSafraProd.setCellValueFactory(new PropertyValueFactory<>("producaoSacosPorHectare"));
        colSafraProdTotalSacos.setCellValueFactory(new PropertyValueFactory<>("producaoTotalSacos"));
        colSafraProdTotalKg.setCellValueFactory(new PropertyValueFactory<>("producaoTotalKg"));
        tabelaSafras.setItems(dadosTabelaSafras);
        
        // Configura Tabela Talhões
        colTalhaoId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTalhaoNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colTalhaoArea.setCellValueFactory(new PropertyValueFactory<>("areaHectares"));
        tabelaTalhoes.setItems(dadosTabelaTalhoes);

        // NOVO: Configura Tabela Atividades
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
        filtroStatusSafra.getSelectionModel().select("Em Andamento"); // Padrão
        // Adiciona listener para aplicar o filtro quando o valor mudar
        filtroStatusSafra.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldV, newV) -> aplicarFiltroSafras()
        );

        // NOVO: Listener de seleção da Tabela Safras
        tabelaSafras.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> handleSafraSelectionChanged(newSelection)
        );

        // NOVO: Desabilita botão de exportar e limpa labels de resumo
        btnExportarCsv.setDisable(true);
        limparResumoFinanceiro(); // Limpa os novos labels

        // Carrega dados das duas tabelas
        carregarDadosSafras();
        carregarDadosTalhoes();
    }

    /**
     * ATUALIZADO: Carrega dados do DAO para a lista mestra e aplica o filtro.
     */
    private void carregarDadosSafras() {
        try {
            listaMestraSafras.clear();
            listaMestraSafras.addAll(safraDAO.listSafrasComInfo());
            aplicarFiltroSafras(); // Aplica o filtro (que preenche a dadosTabelaSafras)
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar as safras.");
        }
    }

    /**
     * NOVO: Filtra a lista mestra e exibe na tabela.
     */
    private void aplicarFiltroSafras() {
        String filtro = filtroStatusSafra.getSelectionModel().getSelectedItem();
        if (filtro == null) {
            filtro = "Em Andamento"; // Padrão
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


    private void carregarDadosTalhoes() {
         try {
            dadosTabelaTalhoes.clear();
            dadosTabelaTalhoes.addAll(talhaoDAO.listTalhoes());
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os talhões.");
        }
    }

    /**
     * NOVO: Chamado quando o usuário seleciona uma safra na tabela principal.
     * Atualiza o painel de detalhes (atividades e custos).
     * ATUALIZADO: Agora calcula e exibe o resumo financeiro (lucro).
     */
    private void handleSafraSelectionChanged(SafraInfo safra) {
        if (safra == null) {
            lblDetalhesTitulo.setText("Detalhes da Safra: (Selecione uma safra acima)");
            btnExportarCsv.setDisable(true);
            dadosTabelaAtividades.clear();
            limparResumoFinanceiro(); // Limpa os labels de custo, receita, etc.
            return;
        }

        // 1. Atualiza botão de exportar
        btnExportarCsv.setDisable(!safra.getStatus().equalsIgnoreCase("Colhida"));

        // 2. Atualiza título
        lblDetalhesTitulo.setText("Detalhes da Safra: " + safra.getCultura() + " (" + safra.getAnoInicio() + ")");

        // 3. Carrega atividades e custos
        try {
            // Carrega atividades
            List<AtividadeSafra> atividades = atividadeSafraDAO.listAtividadesPorSafra(safra.getId());
            
            // Mapeia AtividadeSafra para AtividadeSafraInfo (para incluir nome do insumo)
            List<AtividadeSafraInfo> atividadesInfo = new ArrayList<>();
            for (AtividadeSafra atv : atividades) {
                String nomeInsumo = "N/A (Custo Manual)";
                if (atv.getItemConsumidoId() != null && atv.getItemConsumidoId() > 0) {
                    // USA O NOVO MÉTODO getItemById
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

            // Carrega custo total
            double custoTotal = atividadeSafraDAO.getCustoTotalPorSafra(safra.getId());
            lblCustoTotalSafra.setText(currencyFormatter.format(custoTotal));

            // --- NOVO: Lógica do Resumo Financeiro ---
            if (safra.getStatus().equalsIgnoreCase("Colhida")) {
                // 1. Calcular Receita (Vendas)
                double receitaTotalVendas = 0;
                String nomeItemColheita = safra.getCultura() + " (Colheita " + safra.getAnoInicio() + ")";
                String descVendaQuery = "Venda de " + nomeItemColheita;
                
                List<Transacao> vendas = financeiroDAO.listTransacoesPorDescricaoLike(descVendaQuery);
                for (Transacao venda : vendas) {
                    receitaTotalVendas += venda.getValor();
                }
                lblReceitaTotalSafra.setText(currencyFormatter.format(receitaTotalVendas));

                // 2. Calcular Valor em Estoque
                double valorEmEstoque = 0;
                EstoqueItem itemColheitaEstoque = estoqueDAO.getEstoqueItemPorNome(nomeItemColheita);
                if (itemColheitaEstoque != null) {
                    valorEmEstoque = itemColheitaEstoque.getValorTotal();
                }
                lblEstoqueTotalSafra.setText(currencyFormatter.format(valorEmEstoque));

                // 3. Calcular Balanço
                double balancoFinal = (receitaTotalVendas + valorEmEstoque) - custoTotal;
                lblBalancoSafra.setText(currencyFormatter.format(balancoFinal));
                
                // Aplicar estilo de cor ao balanço
                lblBalancoSafra.getStyleClass().removeAll("positivo", "negativo");
                if (balancoFinal >= 0) {
                    lblBalancoSafra.getStyleClass().add("positivo");
                } else {
                    lblBalancoSafra.getStyleClass().add("negativo");
                }

            } else {
                // Se a safra não foi colhida, limpa os campos do resumo
                limparResumoFinanceiro(custoTotal); // Limpa mantendo o custo
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
        lblBalancoSafra.getStyleClass().removeAll("positivo", "negativo");
    }


    /**
     * Manipulador para o botão "+ Novo Talhão".
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
        TextField areaField = new TextField();
        areaField.setPromptText("Ex: 120.5");

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Área (ha):"), 0, 1);
        grid.add(areaField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    double area = parseDouble(areaField.getText()); // Usando novo helper
                    if (nome.isEmpty() || area <= 0) {
                        AlertUtil.showError("Erro de Validação", "Nome ou área inválidos.");
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
                talhaoDAO.addTalhao(talhao);
                carregarDadosTalhoes(); // Atualiza a tabela de talhões
                AlertUtil.showInfo("Sucesso", "Talhão adicionado com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar o talhão: " + e.getMessage());
            }
        });
    }

    /**
     * Manipulador para o botão "+ Nova Safra".
     * ATUALIZADO:
     * - Campo "Ano" agora é "Safra" (String).
     * - Adicionado ComboBox para "Status".
     */
    @FXML
    private void handleNovaSafra() {
        // 1. Buscar a lista de talhões primeiro
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
        TextField anoField = new TextField(); // Alterado para TextField
        anoField.setPromptText("Ex: 2025/1"); // Prompt atualizado
        
        ComboBox<Talhao> talhaoCombo = new ComboBox<>(FXCollections.observableArrayList(talhoes));
        
        // NOVO: ComboBox para Status
        ComboBox<String> statusCombo = new ComboBox<>();
        statusCombo.setItems(FXCollections.observableArrayList(
            "Planejada", "Em Preparo", "Plantio", "Crescimento", "Aplicação Defensivos"
        ));
        statusCombo.getSelectionModel().selectFirst(); // Default "Planejada"

        
        // Isso faz o ComboBox mostrar o nome do talhão
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
        grid.add(new Label("Safra (ex: 2025/1):"), 0, 1); // Label atualizado
        grid.add(anoField, 1, 1);
        grid.add(new Label("Talhão:"), 0, 2);
        grid.add(talhaoCombo, 1, 2);
        grid.add(new Label("Status Inicial:"), 0, 3); // NOVO
        grid.add(statusCombo, 1, 3); // NOVO

        dialog.getDialogPane().setContent(grid);

        // Lógica de conversão atualizada
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                String cultura = culturaField.getText();
                String ano = anoField.getText(); // Alterado para String
                Talhao talhaoSel = talhaoCombo.getSelectionModel().getSelectedItem();
                String status = statusCombo.getSelectionModel().getSelectedItem(); // NOVO
                
                if (cultura.isEmpty() || ano.isEmpty() || talhaoSel == null || status == null) {
                    AlertUtil.showError("Erro de Validação", "Cultura, Safra e Talhão são obrigatórios.");
                    return null;
                }
                // Construtor atualizado
                return new Safra(cultura, ano, talhaoSel.getId(), status);
            }
            return null;
        });

        Optional<Safra> result = dialog.showAndWait();

        result.ifPresent(safra -> {
            try {
                safraDAO.addSafra(safra);
                carregarDadosSafras(); // ATUALIZADO: Recarrega dados e aplica filtro
                AlertUtil.showInfo("Sucesso", "Safra adicionada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar a safra: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Manipulador para o botão "Lançar Atividade/Custo".
     * Implementa a Etapa 2 do plano.
     * ATUALIZADO: Agora também lança despesa no financeiro.
     */
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

        // 1. Buscar itens de estoque para o ComboBox
        List<EstoqueItem> itensEstoque;
        try {
            itensEstoque = estoqueDAO.listEstoque();
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar a lista de itens do estoque.");
            return;
        }

        if (itensEstoque.isEmpty()) {
            AlertUtil.showError("Estoque Vazio", "Você precisa ter itens cadastrados no Estoque para lançar um consumo.");
            return;
        }

        // 2. Criar o Diálogo
        Dialog<AtividadeSafra> dialog = new Dialog<>();
        dialog.setTitle("Lançar Atividade/Custo na Safra");
        dialog.setHeaderText("Safra: " + safraSelecionada.getCultura() + " (" + safraSelecionada.getAnoInicio() + ")");

        ButtonType lancarButtonType = new ButtonType("Lançar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(lancarButtonType, ButtonType.CANCEL);

        // 3. Layout do Diálogo
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker dataField = new DatePicker(LocalDate.now());
        TextField descField = new TextField();
        descField.setPromptText("Ex: Adubação de cobertura");
        
        ComboBox<EstoqueItem> itemCombo = new ComboBox<>(FXCollections.observableArrayList(itensEstoque));
        TextField qtdField = new TextField("1.0");
        Label custoCalculadoLabel = new Label("Custo (R$): ---");

        // Configura ComboBox para mostrar nome do item
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

        // 4. Lógica de cálculo automático de custo (CORRIGIDO)
        
        // Listener for ComboBox<EstoqueItem>
        itemCombo.valueProperty().addListener((obs, oldV, newV) -> {
            atualizarCustoAtividade(newV, qtdField.getText(), custoCalculadoLabel);
        });

        // Listener for TextField (String)
        qtdField.textProperty().addListener((obs, oldV, newV) -> {
            atualizarCustoAtividade(itemCombo.getSelectionModel().getSelectedItem(), newV, custoCalculadoLabel);
        });

        grid.add(new Label("Data:"), 0, 0);
        grid.add(dataField, 1, 0);
        grid.add(new Label("Descrição:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Insumo (do Estoque):"), 0, 2);
        grid.add(itemCombo, 1, 2);
        grid.add(new Label("Quantidade Usada:"), 0, 3);
        grid.add(qtdField, 1, 3);
        grid.add(custoCalculadoLabel, 1, 4);

        dialog.getDialogPane().setContent(grid);

        // 5. Converter resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == lancarButtonType) {
                try {
                    LocalDate data = dataField.getValue();
                    String desc = descField.getText();
                    EstoqueItem item = itemCombo.getSelectionModel().getSelectedItem();
                    double qtd = parseDouble(qtdField.getText());

                    if (data == null || desc.isEmpty() || item == null) {
                        AlertUtil.showError("Erro de Validação", "Data, Descrição e Insumo são obrigatórios.");
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
                        safraSelecionada.getId(),
                        desc,
                        data.toString(),
                        item.getId(),
                        qtd,
                        custoTotal
                    );

                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Quantidade inválida.");
                    return null;
                }
            }
            return null;
        });

        // 6. Processar resultado
        Optional<AtividadeSafra> result = dialog.showAndWait();

        result.ifPresent(atividade -> {
            try {
                // 1. Lança a atividade no banco
                atividadeSafraDAO.addAtividade(atividade);
                
                // 2. Consome o item do estoque
                estoqueDAO.consumirEstoque(atividade.getItemConsumidoId(), atividade.getQuantidadeConsumida());

                // 3. NOVO: Lança a despesa no financeiro <-- REMOVIDO
                // O custo já foi registrado na COMPRA do insumo (EstoqueController)
                // if (atividade.getCustoTotalAtividade() > 0) {
                //     String descFin = "Custo Safra (" + safraSelecionada.getCultura() + "): " + atividade.getDescricao();
                //     double valorFin = -atividade.getCustoTotalAtividade(); // Despesa é negativa
                //     Transacao transacao = new Transacao(descFin, valorFin, atividade.getData(), "despesa");
                //     financeiroDAO.addTransacao(transacao);
                // }

                // Mensagem de sucesso atualizada
                AlertUtil.showInfo("Sucesso", "Atividade lançada e estoque consumido.\nO custo da atividade foi registrado na safra.");
                
                // 4. Atualiza a tabela de detalhes se a safra ainda estiver selecionada
                if (safraSelecionada.equals(tabelaSafras.getSelectionModel().getSelectedItem())) {
                    handleSafraSelectionChanged(safraSelecionada);
                }

            } catch (IllegalStateException e) {
                // Exceção do consumirEstoque (caso a validação do dialog falhe por concorrência)
                AlertUtil.showError("Erro de Estoque", e.getMessage());
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível salvar a atividade: " + e.getMessage());
            }
        });
    }

    /**
     * Helper class para o resultado do diálogo de colheita.
     * ATUALIZADO: Armazena produção em KG e SACOS, e valor por SACO.
     */
    private static class ColheitaData {
        final double producaoKg; // Necessário para o SafraDAO
        final double producaoSacos; // NOVO: Necessário para o EstoqueDAO
        final double valorTotal; // Para EstoqueDAO
        final double valorPorSaco; // NOVO: (valorTotal / producaoSacos)

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

    /**
     * Manipulador para "Registrar Colheita".
     * ATUALIZADO:
     * - Pergunta o Valor de Venda (R$/saco).
     * - Adiciona o item colhido (em SACOS) ao Estoque.
     * - ATUALIZADO: Não lança mais receita aqui.
     */
    @FXML
    private void handleRegistrarColheita() {
        SafraInfo safraSelecionada = tabelaSafras.getSelectionModel().getSelectedItem();

        if (safraSelecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione uma safra na tabela para registrar a colheita.");
            return;
        }

        // NOVO: Verificar se já está colhida
        if (safraSelecionada.getStatus().equalsIgnoreCase("Colhida")) {
             AlertUtil.showInfo("Ação Inválida", "Esta safra já foi colhida e a produção já foi registrada.");
             return;
        }
        
        // 1. Criar o diálogo customizado
        Dialog<ColheitaData> dialog = new Dialog<>(); // Alterado para ColheitaData
        dialog.setTitle("Registrar Colheita");
        dialog.setHeaderText("Insira a produtividade para: " + safraSelecionada.getCultura() + " (" + safraSelecionada.getTalhaoNome() + ")");

        ButtonType registrarButtonType = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(registrarButtonType, ButtonType.CANCEL);

        // 2. Criar o layout (GridPane)
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        // Dados fixos da safra
        double area = safraSelecionada.getAreaHectares();
        double prodAtualScHa = safraSelecionada.getProducaoSacosPorHectare();

        // Labels de informação
        Label areaLabel = new Label(String.format(Locale.US, "%.2f ha", area));
        Label totalSacosLabel = new Label("---");
        Label totalKgLabel = new Label("---");
        Label valorTotalColheitaLabel = new Label("---"); // NOVO
        
        // Campo de entrada
        TextField scHaField = new TextField(String.format(Locale.US, "%.2f", prodAtualScHa));
        TextField valorSacoField = new TextField("0.0"); // NOVO
        
        // Adiciona componentes ao grid
        grid.add(new Label("Área do Talhão:"), 0, 0);
        grid.add(areaLabel, 1, 0);
        
        grid.add(new Label("Produção (sc/ha):"), 0, 1);
        grid.add(scHaField, 1, 1);
        
        grid.add(new Label("Total (sacos):"), 0, 2);
        grid.add(totalSacosLabel, 1, 2);
        
        grid.add(new Label("Total (kg):"), 0, 3);
        grid.add(totalKgLabel, 1, 3);
        
        grid.add(new Label("Valor Venda (R$/saco):"), 0, 4); // NOVO
        grid.add(valorSacoField, 1, 4); // NOVO
        
        grid.add(new Label("Valor Total (R$):"), 0, 5); // NOVO
        grid.add(valorTotalColheitaLabel, 1, 5); // NOVO
        
        // 3. Adicionar listener para cálculo automático
        ChangeListener<String> listener = (obs, oldV, newV) -> {
            atualizarCalculosDialogColheita(
                scHaField.getText(), valorSacoField.getText(), area,
                totalSacosLabel, totalKgLabel, valorTotalColheitaLabel
            );
        };
        
        scHaField.textProperty().addListener(listener);
        valorSacoField.textProperty().addListener(listener); // NOVO listener
        
        // Chama o método helper diretamente com o valor inicial
        atualizarCalculosDialogColheita(
            scHaField.getText(), valorSacoField.getText(), area,
            totalSacosLabel, totalKgLabel, valorTotalColheitaLabel
        );

        dialog.getDialogPane().setContent(grid);

        // 4. Converter o resultado para o objeto ColheitaData
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registrarButtonType) {
                try {
                    double producaoScHa = parseDouble(scHaField.getText()); // Usando novo helper
                    double valorSaco = parseDouble(valorSacoField.getText()); // NOVO
                    
                    if (producaoScHa <= 0 || valorSaco < 0) { // Validando valor (produção deve ser > 0)
                        AlertUtil.showError("Valor Inválido", "A produção deve ser maior que zero e o valor não pode ser negativo.");
                        return null;
                    }

                    // Cálculos
                    double totalSacos = (producaoScHa * area);
                    double producaoKg = totalSacos * 60.0;
                    double valorTotal = totalSacos * valorSaco; 
                    
                    // Retorna o objeto atualizado
                    return new ColheitaData(producaoKg, totalSacos, valorTotal); 

                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valores de produção ou R$ inválidos.");
                    return null;
                }
            }
            return null;
        });

        // 5. Exibir o diálogo e processar o resultado
        Optional<ColheitaData> result = dialog.showAndWait(); // Tipo de Optional alterado

        result.ifPresent(colheitaData -> {
            try {
                // 1. Atualizar a safra (DAO define status como 'Colhida' e salva KG)
                safraDAO.updateProducaoSafra(safraSelecionada.getId(), colheitaData.producaoKg);

                // 2. Criar o item de estoque (ATUALIZADO PARA SACOS)
                String nomeItem = safraSelecionada.getCultura() + " (Colheita " + safraSelecionada.getAnoInicio() + ")";
                String unidadeItem = "sacos"; // ATUALIZADO

                EstoqueItem itemColheita = new EstoqueItem(
                    nomeItem,
                    colheitaData.producaoSacos, // ATUALIZADO
                    unidadeItem,
                    colheitaData.valorPorSaco, // ATUALIZADO (R$/saco)
                    colheitaData.valorTotal
                );

                // 3. NOVO: Adicionar ao estoque
                estoqueDAO.addEstoque(itemColheita);

                // 4. ATUALIZADO: Transação financeira de "Receita de Produção" foi REMOVIDA.
                // A receita agora é registrada somente na VENDA do item.
                // O valor da colheita agora é um ATIVO em estoque.

                // 5. Recarregar dados e notificar
                carregarDadosSafras(); // ATUALIZADO: Recarrega dados e aplica filtro
                
                // Mensagem de sucesso atualizada
                AlertUtil.showInfo("Sucesso", 
                    "Colheita registrada com sucesso.\n" +
                    "Status da safra atualizado para 'Colhida'.\n" +
                    String.format(Locale.US, "%.2f sacos de %s", colheitaData.producaoSacos, nomeItem) + // ATUALIZADO
                    " foram adicionados ao estoque com seu valor de custo/venda registrado." // Mensagem atualizada
                );

            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível registrar a colheita ou atualizar o estoque: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Manipulador para o botão "Exportar Relatório (CSV)".
     * ATUALIZADO: Agora busca vendas reais e calcula o lucro.
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

        // Mostra o diálogo de salvar
        File file = fileChooser.showSaveDialog(tabelaSafras.getScene().getWindow());

        if (file == null) {
            return; // Usuário cancelou
        }

        // Tenta escrever o arquivo
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) { // Especifica UTF-8 para acentuação
            
            StringBuilder sb = new StringBuilder();
            
            // --- Cabeçalho do Relatório ---
            sb.append("Relatório da Safra: " + safra.getCultura() + " (" + safra.getAnoInicio() + ")\n");
            sb.append("Talhão: " + safra.getTalhaoNome() + " (" + safra.getAreaHectares() + " ha)\n");
            sb.append("Status: " + safra.getStatus() + "\n\n");
            
            // --- Cabeçalho CSV ---
            sb.append("Tipo;Data;Descricao;Insumo/Produto;Quantidade;Unidade;Valor Total (R$)\n");

            // --- 1. Custos (Atividades) ---
            double custoTotal = 0;
            List<AtividadeSafra> atividades = atividadeSafraDAO.listAtividadesPorSafra(safra.getId());
            
            for (AtividadeSafra atv : atividades) {
                String nomeInsumo = "N/A (Custo Manual)";
                String unidade = "";
                if (atv.getItemConsumidoId() != null && atv.getItemConsumidoId() > 0) {
                    // Busca o item de estoque para pegar nome e unidade
                    EstoqueItem item = estoqueDAO.getItemById(atv.getItemConsumidoId());
                    if (item != null) {
                        nomeInsumo = item.getItemNome();
                        unidade = item.getUnidade();
                    }
                }
                
                // Formata usando Locale.US (ponto decimal) para consistência no CSV
                sb.append(String.format(Locale.US, "Custo;%s;\"%s\";\"%s\";%.2f;%s;%.2f\n",
                    atv.getData(),
                    atv.getDescricao().replace("\"", "\"\""), // Escapa aspas
                    nomeInsumo.replace("\"", "\"\""),
                    atv.getQuantidadeConsumida(),
                    unidade,
                    -atv.getCustoTotalAtividade() // Custo é negativo
                ));
                custoTotal += atv.getCustoTotalAtividade(); // Soma o custo (positivo)
            }

            // --- 2. Receitas (Vendas Reais) ---
            double receitaTotalVendas = 0;
            String nomeItemColheita = safra.getCultura() + " (Colheita " + safra.getAnoInicio() + ")";
            // Busca por transações de "Venda de..."
            String descVendaQuery = "Venda de " + nomeItemColheita;
            
            // Usa o novo método do DAO para buscar TODAS as vendas
            List<Transacao> vendas = financeiroDAO.listTransacoesPorDescricaoLike(descVendaQuery);

            for (Transacao venda : vendas) {
                sb.append(String.format(Locale.US, "Receita (Venda);%s;\"%s\";\"%s\";N/A;N/A;%.2f\n",
                    venda.getData(),
                    venda.getDescricao().replace("\"", "\"\""),
                    nomeItemColheita.replace("\"", "\"\""),
                    venda.getValor() // Valor da receita (positivo)
                ));
                receitaTotalVendas += venda.getValor();
            }

            // --- 3. Valor em Estoque (Produto não vendido) ---
            double valorEmEstoque = 0;
            EstoqueItem itemColheitaEstoque = estoqueDAO.getEstoqueItemPorNome(nomeItemColheita);
            
            if (itemColheitaEstoque != null) {
                // O valor total do item em estoque (qtd restante * custo médio)
                valorEmEstoque = itemColheitaEstoque.getValorTotal(); 
                if (valorEmEstoque > 0) {
                    sb.append(String.format(Locale.US, "Valor em Estoque;%s;\"%s\";\"%s\";%.2f;%s;%.2f\n",
                        LocalDate.now().toString(), // Data de hoje para o valor em estoque
                        "Produto em Estoque (Não Vendido)",
                        itemColheitaEstoque.getItemNome().replace("\"", "\"\""),
                        itemColheitaEstoque.getQuantidade(),
                        itemColheitaEstoque.getUnidade(),
                        valorEmEstoque // Valor do ativo restante
                    ));
                }
            }

            // --- 4. Sumário ---
            double balancoFinal = (receitaTotalVendas + valorEmEstoque) - custoTotal;
            sb.append("\n\n--- Resumo Financeiro ---\n");
            sb.append(String.format(Locale.US, "Total Receitas (Vendas);%.2f\n", receitaTotalVendas));
            sb.append(String.format(Locale.US, "Valor em Estoque (Custo Médio);%.2f\n", valorEmEstoque));
            sb.append(String.format(Locale.US, "Receita Bruta Total (Vendas + Estoque);%.2f\n", (receitaTotalVendas + valorEmEstoque)));
            sb.append(String.format(Locale.US, "Total Custos (Insumos);%.2f\n", -custoTotal)); // Mostra custo como negativo
            sb.append(String.format(Locale.US, "Balanço/Lucro Final;%.2f\n", balancoFinal));

            writer.write(sb.toString());
            AlertUtil.showInfo("Sucesso", "Relatório CSV exportado com sucesso para:\n" + file.getAbsolutePath());

        } catch (IOException | SQLException e) {
            AlertUtil.showError("Erro ao Exportar", "Não foi possível gerar o arquivo CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * NOVO: Manipulador para o botão "Atualizar Status".
     */
    @FXML
    private void handleAtualizarStatus() {
        SafraInfo safraSelecionada = tabelaSafras.getSelectionModel().getSelectedItem();
        if (safraSelecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione uma safra para atualizar o status.");
            return;
        }

        // Não permite alterar status de safra colhida
        if (safraSelecionada.getStatus().equalsIgnoreCase("Colhida")) {
            AlertUtil.showInfo("Ação Inválida", "Esta safra já foi colhida e não pode ter seu status alterado.");
            return;
        }

        // Lista de possíveis status
        List<String> statusOpcoes = new ArrayList<>(Arrays.asList(
            "Planejada", "Em Preparo", "Plantio", "Crescimento", 
            "Aplicação Defensivos", "Colheita"
            // Removido "Colhida", pois só deve ser setado via "Registrar Colheita"
        ));
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(safraSelecionada.getStatus(), statusOpcoes);
        dialog.setTitle("Atualizar Status da Safra");
        dialog.setHeaderText("Selecione o novo status para:\n" + safraSelecionada.getCultura() + " (" + safraSelecionada.getAnoInicio() + ")");
        dialog.setContentText("Status:");

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(novoStatus -> {
            try {
                // Chama o novo método do DAO
                safraDAO.updateStatusSafra(safraSelecionada.getId(), novoStatus);
                carregarDadosSafras(); // ATUALIZADO: Recarrega dados e aplica filtro
                AlertUtil.showInfo("Sucesso", "Status da safra atualizado para '" + novoStatus + "'.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar o status: " + e.getMessage());
            }
        });
    }


    /**
     * Manipulador para o botão "- Remover Safra".
     */
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
                safraDAO.removerSafra(safraSelecionada.getId());
                carregarDadosSafras(); // ATUALIZADO: Recarrega dados e aplica filtro
                AlertUtil.showInfo("Sucesso", "Safra removida com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover a safra: " + e.getMessage());
            }
        }
    }

    /**
     * Manipulador para o botão "- Remover Talhão".
     */
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
                talhaoDAO.removerTalhao(talhaoSelecionado.getId());
                carregarDadosTalhoes(); // Atualiza a tabela de talhões
                AlertUtil.showInfo("Sucesso", "Talhão removido com sucesso.");
            } catch (SQLException e) {
                // Captura erro de chave estrangeira (SQLite)
                if (e.getMessage().contains("FOREIGN KEY constraint failed")) {
                    AlertUtil.showError("Erro de Remoção", "Não é possível remover este talhão, pois ele está associado a uma ou mais safras.");
                } else {
                    AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover o talhão: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * NOVO: Método helper atualizado para calcular produção e valor.
     */
    private void atualizarCalculosDialogColheita(
        String scHaStr, String vlrSacoStr, double area,
        Label totalSacosLabel, Label totalKgLabel, Label totalValorLabel
    ) {
        try {
            double producaoScHa = parseDouble(scHaStr); // Usando novo helper
            double valorSaco = parseDouble(vlrSacoStr); // Usando novo helper
            
            if (producaoScHa < 0 || valorSaco < 0) {
                totalSacosLabel.setText("Inválido");
                totalKgLabel.setText("Inválido");
                totalValorLabel.setText("Inválido");
                return;
            }
            
            // Cálculos
            double totalSacos = producaoScHa * area;
            double producaoKg = totalSacos * 60.0;
            double valorTotal = totalSacos * valorSaco;
            
            totalSacosLabel.setText(String.format(Locale.US, "%.2f sacos", totalSacos));
            totalKgLabel.setText(String.format(Locale.US, "%.2f kg", producaoKg));
            totalValorLabel.setText(currencyFormatter.format(valorTotal)); // Formata como moeda
            
        } catch (NumberFormatException e) {
            totalSacosLabel.setText("---");
            totalKgLabel.setText("---");
            totalValorLabel.setText("---");
        }
    }

    // NOVO: Helper para parse de double (copiado do EstoqueController)
    private double parseDouble(String text) throws NumberFormatException {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        return Double.parseDouble(text.replace(",", "."));
    }
    
    /**
     * NOVO: Helper para atualizar o label de custo no diálogo de atividade.
     */
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

    /**
     * NOVO: DTO (Data Transfer Object) interno para popular a tabela de atividades.
     * Inclui o nome do insumo (String) ao invés do ID.
     */
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

