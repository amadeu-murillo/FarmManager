package com.farmmanager.controller;

import com.farmmanager.model.Patrimonio;
import com.farmmanager.model.PatrimonioDAO;
import com.farmmanager.model.Manutencao; // NOVO
import com.farmmanager.model.ManutencaoDAO; // NOVO
import com.farmmanager.model.FinanceiroDAO; // NOVO
import com.farmmanager.model.Transacao; // NOVO
import com.farmmanager.model.Conta; // NOVO: Import para Contas
import com.farmmanager.model.ContaDAO; // NOVO: Import para ContasDAO
import com.farmmanager.model.EstoqueItem; // NOVO: Import para Estoque
import com.farmmanager.model.EstoqueDAO; // NOVO: Import para EstoqueDAO
import com.farmmanager.model.AtividadeSafra; // NOVO: Import para Atividade
import com.farmmanager.model.AtividadeSafraDAO; // NOVO: Import para AtividadeDAO
import com.farmmanager.util.AlertUtil;
import javafx.beans.binding.Bindings; // NOVO IMPORT
import javafx.beans.value.ChangeListener; // NOVO IMPORT
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node; // NOVO IMPORT
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox; // NOVO IMPORT
import javafx.scene.layout.VBox; // NOVO: Import para VBox
import javafx.scene.text.Font; // NOVO IMPORT
import javafx.scene.text.FontWeight; // NOVO IMPORT
import javafx.util.Pair; // NOVO IMPORT
import javafx.scene.control.ScrollPane; // NOVO: Import para ScrollPane

import java.sql.SQLException;
import java.text.NumberFormat; // NOVO
import java.time.LocalDate;
import java.util.List; // NOVO
import java.util.Locale; // NOVO
import java.util.Optional;

/**
 * NOVO: Controller para o PatrimonioView.fxml.
 * ATUALIZADO:
 * - Adicionada integração total com Financeiro (Aquisição, Manutenção, Venda).
 * - Adicionado painel de detalhes com histórico de manutenção.
 * - MELHORIA USABILIDADE: 'handleAdicionar' agora permite adicionar ativo sem registro financeiro (ajuste).
 * - ATUALIZAÇÃO (handleRegistrarManutencao): Agora permite lançar manutenção "À Vista" (Financeiro)
 * ou "A Prazo" (Contas a Pagar) e coletar dados do fornecedor.
 * - ATUALIZADO (handleRegistrarManutencao): Conteúdo do diálogo agora está em um ScrollPane.
 * - ATUALIZADO (handleRegistrarManutencao): Agora permite consumir item do estoque.
 * - MELHORIA USABILIDADE (handleRegistrarManutencao): Diálogo reorganizado com TitledPane.
 * - CORREÇÃO CONTÁBIL (handleRegistrarManutencao): Lançamento financeiro agora usa
 * apenas o Custo Adicional (Mão de Obra), pois o custo do item já está no estoque.
 */
public class PatrimonioController {

    @FXML
    private TableView<Patrimonio> tabelaPatrimonio;
    @FXML
    private TableColumn<Patrimonio, Integer> colPatId;
    @FXML
    private TableColumn<Patrimonio, String> colPatNome;
    @FXML
    private TableColumn<Patrimonio, String> colPatTipo;
    @FXML
    private TableColumn<Patrimonio, String> colPatDataAquisicao;
    @FXML
    private TableColumn<Patrimonio, Double> colPatValorAquisicao;
    @FXML
    private TableColumn<Patrimonio, String> colPatStatus;

    // --- NOVO: Painel de Detalhes ---
    @FXML
    private SplitPane splitPane;
    @FXML
    private Label lblDetalhesTitulo;
    @FXML
    private TableView<Manutencao> tabelaManutencao;
    @FXML
    private TableColumn<Manutencao, String> colManData;
    @FXML
    private TableColumn<Manutencao, String> colManDesc;
    @FXML
    private TableColumn<Manutencao, Double> colManCusto;
    @FXML
    private Label lblCustoTotalManutencao;
    
    // --- NOVO: Botões da Toolbar ---
    @FXML
    private Button btnRegistrarManutencao;
    @FXML
    private Button btnAtualizarStatus;
    @FXML
    private Button btnVenderAtivo;
    @FXML
    private Button btnRemover;

    private final PatrimonioDAO patrimonioDAO;
    private final ManutencaoDAO manutencaoDAO; // NOVO
    private final FinanceiroDAO financeiroDAO; // NOVO
    private final ContaDAO contaDAO; // NOVO
    private final EstoqueDAO estoqueDAO; // NOVO
    private final AtividadeSafraDAO atividadeSafraDAO; // NOVO
    
    private final ObservableList<Patrimonio> dadosTabela;
    private final ObservableList<Manutencao> dadosTabelaManutencao; // NOVO
    private final NumberFormat currencyFormatter; // NOVO
    
    // NOVO: Variável para validação de diálogo (Evita loops)
    private boolean validadorEstoque = false;


    /**
     * NOVO: Classe interna para encapsular o resultado complexo
     * do diálogo de registro de manutenção.
     * ATUALIZADO: Inclui dados de consumo de estoque.
     */
    private static class ManutencaoDialogInfo {
        final String descricao;
        final LocalDate data;
        final EstoqueItem itemConsumido; // NULO se não consumir
        final double quantidadeConsumida;
        final double custoAdicional; // Mão de obra, etc.
        final String tipoPagamento; // "À Vista" ou "A Prazo"
        final LocalDate dataVencimento; // Nulo se for "À Vista"
        final String fornecedorNome;
        final String fornecedorEmpresa;

        ManutencaoDialogInfo(String descricao, LocalDate data, EstoqueItem itemConsumido, double quantidadeConsumida, double custoAdicional, String tipoPagamento, LocalDate dataVencimento, String fornecedorNome, String fornecedorEmpresa) {
            this.descricao = descricao;
            this.data = data;
            this.itemConsumido = itemConsumido;
            this.quantidadeConsumida = quantidadeConsumida;
            this.custoAdicional = custoAdicional;
            this.tipoPagamento = tipoPagamento;
            this.dataVencimento = dataVencimento;
            this.fornecedorNome = fornecedorNome;
            this.fornecedorEmpresa = fornecedorEmpresa;
        }
    }


    public PatrimonioController() {
        patrimonioDAO = new PatrimonioDAO();
        manutencaoDAO = new ManutencaoDAO(); // NOVO
        financeiroDAO = new FinanceiroDAO(); // NOVO
        contaDAO = new ContaDAO(); // NOVO
        estoqueDAO = new EstoqueDAO(); // NOVO
        atividadeSafraDAO = new AtividadeSafraDAO(); // NOVO
        
        dadosTabela = FXCollections.observableArrayList();
        dadosTabelaManutencao = FXCollections.observableArrayList(); // NOVO
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")); // NOVO
    }

    @FXML
    public void initialize() {
        // Configura as colunas da tabela principal
        colPatId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colPatNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colPatTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colPatDataAquisicao.setCellValueFactory(new PropertyValueFactory<>("dataAquisicao"));
        colPatValorAquisicao.setCellValueFactory(new PropertyValueFactory<>("valorAquisicao"));
        colPatStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        tabelaPatrimonio.setItems(dadosTabela);
        
        // Formatação da coluna de valor
        colPatValorAquisicao.setCellFactory(tc -> new TableCell<Patrimonio, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(value));
                }
            }
        });


        // NOVO: Configura colunas da tabela de manutenção
        colManData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colManDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colManCusto.setCellValueFactory(new PropertyValueFactory<>("custo"));
        
        // Formatação da coluna de custo de manutenção
         colManCusto.setCellFactory(tc -> new TableCell<Manutencao, Double>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(value));
                }
            }
        });
        
        tabelaManutencao.setItems(dadosTabelaManutencao);

        // NOVO: Listener de seleção para atualizar painel de detalhes
        tabelaPatrimonio.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> handlePatrimonioSelectionChanged(newSelection)
        );

        // NOVO: Desabilitar botões de ação se nada estiver selecionado
        btnRegistrarManutencao.disableProperty().bind(tabelaPatrimonio.getSelectionModel().selectedItemProperty().isNull());
        btnAtualizarStatus.disableProperty().bind(tabelaPatrimonio.getSelectionModel().selectedItemProperty().isNull());
        btnVenderAtivo.disableProperty().bind(tabelaPatrimonio.getSelectionModel().selectedItemProperty().isNull());
        btnRemover.disableProperty().bind(tabelaPatrimonio.getSelectionModel().selectedItemProperty().isNull());

        limparDetalhes();
        carregarDadosDaTabela();
    }

    private void carregarDadosDaTabela() {
        try {
            dadosTabela.clear();
            dadosTabela.addAll(patrimonioDAO.listPatrimonio());
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar o patrimônio.");
        }
    }

    /**
     * NOVO: Limpa o painel de detalhes quando nada está selecionado.
     */
    private void limparDetalhes() {
        lblDetalhesTitulo.setText("Detalhes: (Selecione um ativo)");
        dadosTabelaManutencao.clear();
        lblCustoTotalManutencao.setText("Custo Total de Manutenção: R$ 0,00");
    }

    /**
     * NOVO: Carrega os detalhes do ativo selecionado no painel da direita.
     */
    private void handlePatrimonioSelectionChanged(Patrimonio selecionado) {
        if (selecionado == null) {
            limparDetalhes();
            return;
        }

        lblDetalhesTitulo.setText("Detalhes: " + selecionado.getNome());

        try {
            // Carrega histórico de manutenção
            dadosTabelaManutencao.clear();
            dadosTabelaManutencao.addAll(manutencaoDAO.listManutencaoPorPatrimonio(selecionado.getId()));

            // Carrega custo total
            double custoTotal = manutencaoDAO.getCustoTotalManutencao(selecionado.getId());
            lblCustoTotalManutencao.setText("Custo Total de Manutenção: " + currencyFormatter.format(custoTotal));

        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar o histórico de manutenção: " + e.getMessage());
        }
    }


    /**
     * ATUALIZADO: Agora também lança uma despesa no financeiro.
     * ATUALIZADO: Adiciona CheckBox para registro financeiro.
     * ATUALIZADO: Adiciona validação em tempo real (listeners) e usa Pair no resultado.
     */
    @FXML
    private void handleAdicionar() {
        // 1. Criar o diálogo
        Dialog<Pair<Patrimonio, Boolean>> dialog = new Dialog<>(); // ATUALIZADO
        dialog.setTitle("Adicionar Novo Ativo");
        dialog.setHeaderText("Preencha os dados do ativo (máquina, implemento, etc.)");

        ButtonType adicionarButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomeField = new TextField();
        nomeField.setPromptText("Ex: Trator John Deere 6110J");
        TextField tipoField = new TextField();
        tipoField.setPromptText("Ex: Trator, Colheitadeira");
        DatePicker dataAquisicaoPicker = new DatePicker(LocalDate.now());
        TextField valorAquisicaoField = new TextField();
        valorAquisicaoField.setPromptText("Ex: 500000.00");
        ComboBox<String> statusCombo = new ComboBox<>(
                FXCollections.observableArrayList("Operacional", "Em Manutenção", "Inativo")
        );
        statusCombo.getSelectionModel().selectFirst();
        
        // NOVO: Checkbox para controle financeiro
        CheckBox registrarFinanceiroCheck = new CheckBox("Registrar aquisição no financeiro?");
        registrarFinanceiroCheck.setSelected(true);


        grid.add(new Label("Nome/Descrição:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Tipo:"), 0, 1);
        grid.add(tipoField, 1, 1);
        grid.add(new Label("Data Aquisição:"), 0, 2);
        grid.add(dataAquisicaoPicker, 1, 2);
        grid.add(new Label("Valor (R$):"), 0, 3);
        grid.add(valorAquisicaoField, 1, 3);
        grid.add(new Label("Status:"), 0, 4);
        grid.add(statusCombo, 1, 4);
        grid.add(registrarFinanceiroCheck, 0, 5, 2, 1);
        
        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        // 4. Habilitar/Desabilitar botão Adicionar (Validação)
        Node adicionarButtonNode = dialog.getDialogPane().lookupButton(adicionarButtonType);
        adicionarButtonNode.setDisable(true); // Começa desabilitado

        // Listener para validar
        Runnable validador = () -> {
            boolean nomeOk = !nomeField.getText().trim().isEmpty();
            boolean tipoOk = !tipoField.getText().trim().isEmpty();
            boolean valorOk = false;
            boolean registrar = registrarFinanceiroCheck.isSelected();
            
            try {
                // Remove o R$ e formatação regional, aceita , e .
                String valorLimpo = valorAquisicaoField.getText().replace("R$", "").trim().replace(".", "").replace(",", ".");
                double valor = Double.parseDouble(valorLimpo);
                
                // Se for registrar, valor > 0. Se for ajuste, valor >= 0 (permite 0).
                valorOk = registrar ? valor > 0 : valor >= 0;
            } catch (NumberFormatException e) {
                valorOk = false;
            }
            
            adicionarButtonNode.setDisable(!nomeOk || !tipoOk || !valorOk);
        };
        

        // Adiciona listeners
        nomeField.textProperty().addListener((obs, o, n) -> validador.run());
        tipoField.textProperty().addListener((obs, o, n) -> validador.run());
        valorAquisicaoField.textProperty().addListener((obs, o, n) -> validador.run());
        registrarFinanceiroCheck.selectedProperty().addListener((obs, o, n) -> validador.run());
        validador.run(); // Validação inicial


        // 5. Converter o resultado
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    String tipo = tipoField.getText();
                    LocalDate data = dataAquisicaoPicker.getValue();
                    // Garante que a conversão use o mesmo método da validação
                    String valorLimpo = valorAquisicaoField.getText().replace("R$", "").trim().replace(".", "").replace(",", ".");
                    double valor = Double.parseDouble(valorLimpo);
                    String status = statusCombo.getSelectionModel().getSelectedItem();
                    boolean deveRegistrar = registrarFinanceiroCheck.isSelected();

                    // Validações (já cobertas pelos listeners, mas mantidas por segurança)
                    if (nome.isEmpty() || tipo.isEmpty() || data == null || status.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "Todos os campos são obrigatórios.");
                        return null;
                    }
                    if (deveRegistrar && valor <= 0) {
                        AlertUtil.showError("Erro de Validação", "O valor deve ser positivo para registrar no financeiro.");
                        return null;
                    }
                     if (valor < 0) { // Valor 0 é permitido para ajuste
                        AlertUtil.showError("Erro de Validação", "O valor não pode ser negativo.");
                        return null;
                    }
                    
                    Patrimonio p = new Patrimonio(nome, tipo, data.toString(), valor, status);
                    return new Pair<>(p, deveRegistrar); // ATUALIZADO: Retorna o Pair
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor de aquisição inválido.");
                    return null;
                }
            }
            return null;
        });

        // 6. Exibir o diálogo e processar o resultado
        Optional<Pair<Patrimonio, Boolean>> result = dialog.showAndWait(); // ATUALIZADO

        result.ifPresent(pair -> { // ATUALIZADO
            try {
                Patrimonio patrimonio = pair.getKey();
                boolean deveRegistrar = pair.getValue();

                // 1. Adiciona ao patrimônio (SEMPRE)
                patrimonioDAO.addPatrimonio(patrimonio);
                
                // 2. Lança a despesa no financeiro (CONDICIONAL)
                if (deveRegistrar) {
                    String desc = "Aquisição Ativo: " + patrimonio.getNome();
                    double valor = -patrimonio.getValorAquisicao(); // Despesa é negativa
                    Transacao transacao = new Transacao(desc, valor, patrimonio.getDataAquisicao(), "despesa");
                    financeiroDAO.addTransacao(transacao);
                    AlertUtil.showInfo("Sucesso", "Ativo adicionado e despesa registrada no financeiro.");
                } else {
                     AlertUtil.showInfo("Sucesso", "Ativo adicionado (ajuste manual, sem lançamento financeiro).");
                }

                carregarDadosDaTabela(); // Atualiza a tabela
                
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar o ativo: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Registra um custo de manutenção para o ativo selecionado.
     * ATUALIZADO: Permite registro "À Vista" ou "A Prazo".
     * ATUALIZADO: Conteúdo do diálogo agora está em um ScrollPane.
     * ATUALIZADO: Agora permite consumir item do estoque.
     * ATUALIZADO (USABILIDADE): Reorganizado com TitledPanes.
     */
    @FXML
    private void handleRegistrarManutencao() {
        Patrimonio selecionado = tabelaPatrimonio.getSelectionModel().getSelectedItem();
        if (selecionado == null) return; // Botão deve estar desabilitado, mas é uma segurança

        // NOVO: Carrega itens do estoque
        List<EstoqueItem> itensEstoque;
        try {
            itensEstoque = estoqueDAO.listEstoque();
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os itens do estoque.");
            return;
        }

        Dialog<ManutencaoDialogInfo> dialog = new Dialog<>(); // ATUALIZADO
        dialog.setTitle("Registrar Manutenção");
        dialog.setHeaderText("Ativo: " + selecionado.getNome());
        dialog.setResizable(true); // Mantém redimensionável

        ButtonType adicionarButtonType = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        // --- INÍCIO DA ATUALIZAÇÃO DE USABILIDADE ---

        // VBox principal para os TitledPanes
        VBox mainLayout = new VBox(10);
        mainLayout.setPadding(new Insets(10));

        // --- Seção 1: Detalhes do Serviço ---
        GridPane gridDetalhes = new GridPane();
        gridDetalhes.setHgap(10);
        gridDetalhes.setVgap(10);
        gridDetalhes.setPadding(new Insets(10));
        
        DatePicker dataPicker = new DatePicker(LocalDate.now());
        TextField descField = new TextField();
        descField.setPromptText("Ex: Troca de óleo e filtros");
        
        gridDetalhes.add(new Label("Data:"), 0, 0);
        gridDetalhes.add(dataPicker, 1, 0);
        gridDetalhes.add(new Label("Descrição:"), 0, 1);
        gridDetalhes.add(descField, 1, 1);

        TitledPane detalhesPane = new TitledPane("1. Detalhes do Serviço", gridDetalhes);
        detalhesPane.setCollapsible(true);
        detalhesPane.setExpanded(true);

        // --- Seção 2: Custos (Itens e Mão de Obra) ---
        GridPane gridCustos = new GridPane();
        gridCustos.setHgap(10);
        gridCustos.setVgap(10);
        gridCustos.setPadding(new Insets(10));

        CheckBox consumirItemCheck = new CheckBox("Consumir item do estoque?");
        consumirItemCheck.setSelected(false);

        Label itemLabel = new Label("Item do Estoque:");
        ComboBox<EstoqueItem> itemCombo = new ComboBox<>(FXCollections.observableArrayList(itensEstoque));
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

        Label qtdLabel = new Label("Quantidade:");
        TextField qtdField = new TextField("1.0");

        Label custoItemLabel = new Label(currencyFormatter.format(0.0));
        
        TextField custoAdicionalField = new TextField("0.0");
        custoAdicionalField.setPromptText("Ex: 150.00 (Mão de obra)");
        
        Label custoTotalLabel = new Label("Custo Total: R$ 0,00");
        custoTotalLabel.setFont(Font.font("System", FontWeight.BOLD, 14));
        
        gridCustos.add(consumirItemCheck, 0, 0, 2, 1);
        gridCustos.add(itemLabel, 0, 1);
        gridCustos.add(itemCombo, 1, 1);
        gridCustos.add(qtdLabel, 0, 2);
        gridCustos.add(qtdField, 1, 2);
        gridCustos.add(new Label("Custo do Item:"), 0, 3);
        gridCustos.add(custoItemLabel, 1, 3);
        gridCustos.add(new Label("Custo Adicional (Mão de obra):"), 0, 4);
        gridCustos.add(custoAdicionalField, 1, 4);
        gridCustos.add(new Separator(), 0, 5, 2, 1);
        gridCustos.add(custoTotalLabel, 0, 6, 2, 1);
        
        TitledPane custosPane = new TitledPane("2. Custos da Manutenção", gridCustos);
        custosPane.setCollapsible(true);
        custosPane.setExpanded(true);

        // --- Seção 3: Pagamento ---
        GridPane gridPagamento = new GridPane();
        gridPagamento.setHgap(10);
        gridPagamento.setVgap(10);
        gridPagamento.setPadding(new Insets(10));
        
        TextField fornecedorNomeField = new TextField();
        fornecedorNomeField.setPromptText("Ex: Oficina do Zé (Opcional)");
        TextField fornecedorEmpresaField = new TextField();
        fornecedorEmpresaField.setPromptText("Ex: Zé Peças LTDA (Opcional)");
        
        ComboBox<String> tipoPagCombo = new ComboBox<>(
                FXCollections.observableArrayList("À Vista", "A Prazo")
        );
        tipoPagCombo.getSelectionModel().select("À Vista");

        Label vencimentoLabel = new Label("Data Vencimento:");
        DatePicker vencimentoPicker = new DatePicker(LocalDate.now().plusDays(30));

        // NOVO: Label de aviso sobre o pagamento
        Label infoPagamentoLabel = new Label("O pagamento refere-se APENAS ao Custo Adicional (Mão de Obra).");
        infoPagamentoLabel.setStyle("-fx-font-style: italic; -fx-text-fill: #555;");

        gridPagamento.add(infoPagamentoLabel, 0, 0, 2, 1); // Adicionado no topo
        gridPagamento.add(new Label("Fornecedor (Nome):"), 0, 1);
        gridPagamento.add(fornecedorNomeField, 1, 1);
        gridPagamento.add(new Label("Fornecedor (Empresa):"), 0, 2);
        gridPagamento.add(fornecedorEmpresaField, 1, 2);
        gridPagamento.add(new Label("Tipo de Pagamento:"), 0, 3);
        gridPagamento.add(tipoPagCombo, 1, 3);
        gridPagamento.add(vencimentoLabel, 0, 4);
        gridPagamento.add(vencimentoPicker, 1, 4);

        TitledPane pagamentosPane = new TitledPane("3. Informações de Pagamento (Mão de Obra)", gridPagamento);
        pagamentosPane.setCollapsible(true);
        pagamentosPane.setExpanded(true);

        // --- Montagem Final ---
        mainLayout.getChildren().addAll(detalhesPane, custosPane, pagamentosPane);
        
        // --- FIM DA ATUALIZAÇÃO DE USABILIDADE ---


        // --- Lógica de Visibilidade (Como antes) ---
        Node[] consumoNodes = {itemLabel, itemCombo, qtdLabel, qtdField, custoItemLabel};
        for(Node n : consumoNodes) {
            n.setVisible(false);
            n.setManaged(false);
        }
        vencimentoLabel.setVisible(false);
        vencimentoPicker.setVisible(false);
        vencimentoLabel.setManaged(false);
        vencimentoPicker.setManaged(false);

        consumirItemCheck.selectedProperty().addListener((obs, oldVal, newVal) -> {
            for(Node n : consumoNodes) {
                n.setVisible(newVal);
                n.setManaged(newVal);
            }
        });
        
        tipoPagCombo.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean aPrazo = newVal.equals("A Prazo");
            vencimentoLabel.setVisible(aPrazo);
            vencimentoPicker.setVisible(aPrazo);
            vencimentoLabel.setManaged(aPrazo);
            vencimentoPicker.setManaged(aPrazo);
        });
        
        // --- Validação e Cálculo de Custo Total (Como antes) ---
        Node adicionarButtonNode = dialog.getDialogPane().lookupButton(adicionarButtonType);
        adicionarButtonNode.setDisable(true); // Começa desabilitado

        Runnable validadorManutencao = () -> {
            boolean descOk = !descField.getText().trim().isEmpty();
            boolean custoAdicionalOk = false;
            boolean dataVencOk = true; // Assume OK
            boolean itemConsumoOk = true; // Assume OK
            
            double custoItem = 0.0;
            double custoAdicional = 0.0;

            try {
                custoAdicional = parseDouble(custoAdicionalField.getText());
                custoAdicionalOk = custoAdicional >= 0;
            } catch (NumberFormatException e) {
                custoAdicionalOk = false;
            }

            if (consumirItemCheck.isSelected()) {
                EstoqueItem item = itemCombo.getSelectionModel().getSelectedItem();
                double qtd = 0.0;
                boolean qtdOk = false;

                try {
                    qtd = parseDouble(qtdField.getText());
                    qtdOk = qtd > 0;
                } catch (NumberFormatException e) {
                    qtdOk = false;
                }
                
                if (item == null || !qtdOk) {
                    itemConsumoOk = false;
                    custoItemLabel.setText(currencyFormatter.format(0.0));
                } else {
                    itemConsumoOk = qtd <= item.getQuantidade(); // Valida estoque
                    custoItem = item.getValorUnitario() * qtd;
                    custoItemLabel.setText(currencyFormatter.format(custoItem) + (itemConsumoOk ? "" : " (ESTOQUE INSUFICIENTE)"));
                }
            } else {
                custoItemLabel.setText(currencyFormatter.format(0.0));
            }

            if (tipoPagCombo.getSelectionModel().getSelectedItem().equals("A Prazo")) {
                dataVencOk = vencimentoPicker.getValue() != null;
            }
            
            // Custo total deve ser positivo
            boolean custoTotalOk = (custoItem + custoAdicional) > 0;
            
            // ATUALIZADO: Desabilita pagamento se custoAdicional for zero
            pagamentosPane.setDisable(custoAdicional <= 0);
            
            adicionarButtonNode.setDisable(!descOk || !custoAdicionalOk || !dataVencOk || !itemConsumoOk || !custoTotalOk);
            custoTotalLabel.setText("Custo Total: " + currencyFormatter.format(custoItem + custoAdicional));
        };
        
        // Adiciona listeners
        descField.textProperty().addListener((obs, o, n) -> validadorManutencao.run());
        custoAdicionalField.textProperty().addListener((obs, o, n) -> validadorManutencao.run());
        consumirItemCheck.selectedProperty().addListener((obs, o, n) -> validadorManutencao.run());
        itemCombo.valueProperty().addListener((obs, o, n) -> validadorManutencao.run());
        qtdField.textProperty().addListener((obs, o, n) -> validadorManutencao.run());
        tipoPagCombo.valueProperty().addListener((obs, o, n) -> validadorManutencao.run());
        vencimentoPicker.valueProperty().addListener((obs, o, n) -> validadorManutencao.run());
        validadorManutencao.run(); // Run inicial
        
        // --- FIM VALIDAÇÃO ---

        // NOVO: Coloca o VBox principal dentro de um ScrollPane
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setContent(mainLayout); // ATUALIZADO
        scrollPane.setFitToWidth(true);
        // Remove fundo e borda do scrollpane para integrar com o diálogo
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background-insets: 0;");
        // Define uma altura preferencial para o scrollpane, para evitar que o diálogo fique gigante
        scrollPane.setPrefHeight(550); // Aumentado para caber tudo

        dialog.getDialogPane().setContent(scrollPane); // ATUALIZADO: Define o ScrollPane como conteúdo
        AlertUtil.setDialogIcon(dialog);

        // ATUALIZADO: Converte para ManutencaoDialogInfo
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String desc = descField.getText();
                    LocalDate data = dataPicker.getValue();
                    double custoAdicional = parseDouble(custoAdicionalField.getText());
                    
                    EstoqueItem item = consumirItemCheck.isSelected() ? itemCombo.getSelectionModel().getSelectedItem() : null;
                    double qtd = (item != null) ? parseDouble(qtdField.getText()) : 0.0;
                    
                    String fornNome = fornecedorNomeField.getText();
                    String fornEmpresa = fornecedorEmpresaField.getText();
                    String tipoPag = tipoPagCombo.getValue();
                    LocalDate dataVenc = vencimentoPicker.getValue();

                    // Validações (já cobertas, mas por segurança)
                    if (desc.isEmpty() || data == null) {
                        return null; // Erro já tratado pelo listener
                    }
                    if (tipoPag.equals("A Prazo") && dataVenc == null) {
                         return null; // Erro já tratado
                    }
                    
                    return new ManutencaoDialogInfo(desc, data, item, qtd, custoAdicional, tipoPag, dataVenc, fornNome, fornEmpresa);

                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Custo ou quantidade inválido.");
                    return null;
                }
            }
            return null;
        });

        // ATUALIZADO: Processa ManutencaoDialogInfo
        Optional<ManutencaoDialogInfo> result = dialog.showAndWait();

        result.ifPresent(info -> {
            try {
                // 1. Calcular Custos
                double custoItem = (info.itemConsumido != null) ? (info.itemConsumido.getValorUnitario() * info.quantidadeConsumida) : 0.0;
                // ATUALIZADO: Custo financeiro é APENAS o custo adicional
                double custoAdicional = info.custoAdicional; 
                // Custo total da manutenção (Item + Mão de Obra)
                double custoTotalManutencao = custoItem + custoAdicional; 
                String dataStr = info.data.toString();

                // 2. Adiciona ao histórico de manutenção (Custo Total)
                Manutencao m = new Manutencao(selecionado.getId(), dataStr, info.descricao, custoTotalManutencao);
                manutencaoDAO.addManutencao(m);

                // 3. Consome item (se houver)
                if (info.itemConsumido != null) {
                    // 3a. Consome do estoque
                    estoqueDAO.consumirEstoque(info.itemConsumido.getId(), info.quantidadeConsumida);
                    
                    // 3b. Registra no histórico de atividades (custo do item)
                    String descAtividade = "Manutenção (" + selecionado.getNome() + "): " + info.descricao;
                    AtividadeSafra atv = new AtividadeSafra(
                        null, // safraId (nulo)
                        descAtividade, 
                        dataStr, 
                        info.itemConsumido.getId(), 
                        info.quantidadeConsumida, 
                        custoItem // Custo apenas do item
                    );
                    atividadeSafraDAO.addAtividade(atv);
                }

                // 4. Lança a despesa (APENAS CUSTO ADICIONAL)
                String msgSucesso = "Manutenção registrada com sucesso.";
                if (info.itemConsumido != null) {
                    msgSucesso += "\nItem consumido do estoque.";
                }

                if (custoAdicional > 0) {
                    String descFin = "Manutenção (Mão de Obra): " + selecionado.getNome() + " (" + info.descricao + ")";
                    if (info.fornecedorNome != null && !info.fornecedorNome.isEmpty()) {
                        descFin += " (Fornec: " + info.fornecedorNome + ")";
                    }
                    if (info.fornecedorEmpresa != null && !info.fornecedorEmpresa.isEmpty()) {
                        descFin += " (Empresa: " + info.fornecedorEmpresa + ")";
                    }

                    if ("À Vista".equals(info.tipoPagamento)) {
                        // Lança no Financeiro (Caixa)
                        Transacao transacao = new Transacao(
                            descFin, 
                            -custoAdicional, // ATUALIZADO: Apenas custo adicional
                            dataStr, 
                            "despesa"
                        );
                        financeiroDAO.addTransacao(transacao);
                        msgSucesso += "\nLançamento (à vista) da mão de obra efetuado.";
                        
                    } else {
                        // Lança em Contas a Pagar
                        Conta conta = new Conta(
                            descFin,
                            custoAdicional, // ATUALIZADO: Apenas custo adicional
                            info.dataVencimento.toString(),
                            "pagar",
                            "pendente",
                            info.fornecedorNome,
                            info.fornecedorEmpresa
                        );
                        contaDAO.addConta(conta);
                        msgSucesso += "\nConta a Pagar (mão de obra) registrada.";
                    }
                }

                // 5. Atualiza o status do ativo para "Em Manutenção"
                patrimonioDAO.updateStatus(selecionado.getId(), "Em Manutenção");
                
                AlertUtil.showInfo("Sucesso", msgSucesso);
                
                carregarDadosDaTabela(); // Atualiza a tabela principal (para o status)
                // Re-seleciona o item para atualizar o painel de detalhes
                tabelaPatrimonio.getSelectionModel().select(selecionado);
                handlePatrimonioSelectionChanged(selecionado); 

            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível registrar a manutenção: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Atualiza o status do ativo selecionado.
     */
    @FXML
    private void handleAtualizarStatus() {
        Patrimonio selecionado = tabelaPatrimonio.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;

        List<String> statusOpcoes = List.of("Operacional", "Em Manutenção", "Inativo");
        
        ChoiceDialog<String> dialog = new ChoiceDialog<>(selecionado.getStatus(), statusOpcoes);
        dialog.setTitle("Atualizar Status");
        dialog.setHeaderText("Ativo: " + selecionado.getNome());
        dialog.setContentText("Novo Status:");
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(novoStatus -> {
            try {
                patrimonioDAO.updateStatus(selecionado.getId(), novoStatus);
                AlertUtil.showInfo("Sucesso", "Status atualizado para '" + novoStatus + "'.");
                carregarDadosDaTabela(); // Atualiza a tabela
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar o status: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Vende um ativo, lançando receita e removendo-o.
     */
    @FXML
    private void handleVenderAtivo() {
        Patrimonio selecionado = tabelaPatrimonio.getSelectionModel().getSelectedItem();
        if (selecionado == null) return;

        TextInputDialog dialog = new TextInputDialog("0.00");
        dialog.setTitle("Vender Ativo");
        dialog.setHeaderText("Venda de: " + selecionado.getNome());
        dialog.setContentText("Valor da Venda (R$):");
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(valorStr -> {
            try {
                String valorLimpo = valorStr.replace("R$", "").trim().replace(".", "").replace(",", ".");
                double valorVenda = Double.parseDouble(valorLimpo);
                if (valorVenda < 0) {
                    AlertUtil.showError("Erro de Validação", "O valor da venda não pode ser negativo.");
                    return;
                }

                // 1. Lança a receita no financeiro
                String desc = "Venda Ativo: " + selecionado.getNome();
                Transacao transacao = new Transacao(desc, valorVenda, LocalDate.now().toString(), "receita");
                financeiroDAO.addTransacao(transacao);

                // 2. Remove o ativo do patrimônio
                patrimonioDAO.removerPatrimonio(selecionado.getId());

                AlertUtil.showInfo("Sucesso", "Ativo vendido, receita registrada e item removido do patrimônio.");
                carregarDadosDaTabela(); // Atualiza a tabela
                limparDetalhes(); // Limpa o painel de detalhes

            } catch (NumberFormatException e) {
                AlertUtil.showError("Erro de Formato", "Valor de venda inválido.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível processar a venda: " + e.getMessage());
            }
        });
    }


    /**
     * ATUALIZADO: Agora é apenas para ajuste de dados.
     */
    @FXML
    private void handleRemover() {
        Patrimonio selecionado = tabelaPatrimonio.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um ativo na tabela para remover.");
            return;
        }

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Remoção (Ajuste)", 
            "Tem certeza que deseja remover o ativo '" + selecionado.getNome() + "'?\n\nEsta ação é um AJUSTE e não lançará receitas ou despesas.");

        if (confirmado) {
            try {
                if (patrimonioDAO.removerPatrimonio(selecionado.getId())) {
                    AlertUtil.showInfo("Removido", "Ativo removido com sucesso.");
                    carregarDadosDaTabela();
                    limparDetalhes();
                } else {
                    AlertUtil.showError("Erro ao Remover", "O ativo não pôde ser removido.");
                }
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover o ativo: " + e.getMessage());
            }
        }
    }
    
    // Helper para converter texto em double (aceita , e .)
    private double parseDouble(String text) throws NumberFormatException {
        if (text == null || text.isEmpty()) {
            return 0.0;
        }
        // Substitui vírgula por ponto para aceitar ambos os formatos
        return Double.parseDouble(text.replace(",", "."));
    }
}