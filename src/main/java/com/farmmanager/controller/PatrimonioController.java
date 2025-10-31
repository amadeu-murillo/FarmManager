package com.farmmanager.controller;

import com.farmmanager.model.Patrimonio;
import com.farmmanager.model.PatrimonioDAO;
import com.farmmanager.model.Manutencao; // NOVO
import com.farmmanager.model.ManutencaoDAO; // NOVO
import com.farmmanager.model.FinanceiroDAO; // NOVO
import com.farmmanager.model.Transacao; // NOVO
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

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
    private final ObservableList<Patrimonio> dadosTabela;
    private final ObservableList<Manutencao> dadosTabelaManutencao; // NOVO
    private final NumberFormat currencyFormatter; // NOVO

    public PatrimonioController() {
        patrimonioDAO = new PatrimonioDAO();
        manutencaoDAO = new ManutencaoDAO(); // NOVO
        financeiroDAO = new FinanceiroDAO(); // NOVO
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

        // NOVO: Configura colunas da tabela de manutenção
        colManData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colManDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colManCusto.setCellValueFactory(new PropertyValueFactory<>("custo"));
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
     */
    @FXML
    private void handleAdicionar() {
        Dialog<Patrimonio> dialog = new Dialog<>();
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

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    String tipo = tipoField.getText();
                    LocalDate data = dataAquisicaoPicker.getValue();
                    double valor = Double.parseDouble(valorAquisicaoField.getText().replace(",", ".")); // Helper
                    String status = statusCombo.getSelectionModel().getSelectedItem();

                    if (nome.isEmpty() || tipo.isEmpty() || data == null || status.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "Todos os campos são obrigatórios.");
                        return null;
                    }
                    if (valor <= 0) {
                        AlertUtil.showError("Erro de Validação", "O valor deve ser positivo.");
                        return null;
                    }
                    
                    return new Patrimonio(nome, tipo, data.toString(), valor, status);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor de aquisição inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<Patrimonio> result = dialog.showAndWait();

        result.ifPresent(patrimonio -> {
            try {
                // 1. Adiciona ao patrimônio
                patrimonioDAO.addPatrimonio(patrimonio);
                
                // 2. NOVO: Lança a despesa no financeiro
                String desc = "Aquisição Ativo: " + patrimonio.getNome();
                double valor = -patrimonio.getValorAquisicao(); // Despesa é negativa
                Transacao transacao = new Transacao(desc, valor, patrimonio.getDataAquisicao(), "despesa");
                financeiroDAO.addTransacao(transacao);

                carregarDadosDaTabela(); // Atualiza a tabela
                AlertUtil.showInfo("Sucesso", "Ativo adicionado e despesa registrada no financeiro.");
                
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar o ativo: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Registra um custo de manutenção para o ativo selecionado.
     */
    @FXML
    private void handleRegistrarManutencao() {
        Patrimonio selecionado = tabelaPatrimonio.getSelectionModel().getSelectedItem();
        if (selecionado == null) return; // Botão deve estar desabilitado, mas é uma segurança

        Dialog<Manutencao> dialog = new Dialog<>();
        dialog.setTitle("Registrar Manutenção");
        dialog.setHeaderText("Ativo: " + selecionado.getNome());

        ButtonType adicionarButtonType = new ButtonType("Registrar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        DatePicker dataPicker = new DatePicker(LocalDate.now());
        TextField descField = new TextField();
        descField.setPromptText("Ex: Troca de óleo e filtros");
        TextField custoField = new TextField();
        custoField.setPromptText("Ex: 1500.00");

        grid.add(new Label("Data:"), 0, 0);
        grid.add(dataPicker, 1, 0);
        grid.add(new Label("Descrição:"), 0, 1);
        grid.add(descField, 1, 1);
        grid.add(new Label("Custo (R$):"), 0, 2);
        grid.add(custoField, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String desc = descField.getText();
                    LocalDate data = dataPicker.getValue();
                    double custo = Double.parseDouble(custoField.getText().replace(",", "."));

                    if (desc.isEmpty() || data == null) {
                        AlertUtil.showError("Erro de Validação", "Data e Descrição são obrigatórios.");
                        return null;
                    }
                    if (custo <= 0) {
                        AlertUtil.showError("Erro de Validação", "O custo deve ser positivo.");
                        return null;
                    }
                    
                    return new Manutencao(selecionado.getId(), data.toString(), desc, custo);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Custo inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<Manutencao> result = dialog.showAndWait();

        result.ifPresent(manutencao -> {
            try {
                // 1. Adiciona ao histórico de manutenção
                manutencaoDAO.addManutencao(manutencao);

                // 2. Lança a despesa no financeiro
                String descFin = "Manutenção: " + selecionado.getNome() + " (" + manutencao.getDescricao() + ")";
                Transacao transacao = new Transacao(descFin, -manutencao.getCusto(), manutencao.getData(), "despesa");
                financeiroDAO.addTransacao(transacao);

                // 3. Atualiza o status do ativo para "Em Manutenção"
                patrimonioDAO.updateStatus(selecionado.getId(), "Em Manutenção");

                AlertUtil.showInfo("Sucesso", "Manutenção registrada, despesa lançada e status do ativo atualizado.");
                
                carregarDadosDaTabela(); // Atualiza a tabela principal (para o status)
                handlePatrimonioSelectionChanged(selecionado); // Atualiza o painel de detalhes

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

        Optional<String> result = dialog.showAndWait();

        result.ifPresent(valorStr -> {
            try {
                double valorVenda = Double.parseDouble(valorStr.replace(",", "."));
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
}

