package com.farmmanager.controller;

import com.farmmanager.model.Transacao;
import com.farmmanager.model.FinanceiroDAO;
import com.farmmanager.util.AlertUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task; // NOVO: Import para Task
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox; // NOVO: Import para o VBox
import javafx.stage.FileChooser; 

import java.io.File; 
import java.io.IOException; 
import java.io.PrintWriter; 
import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Controller para o FinanceiroView.fxml.
 * ATUALIZADO:
 * - Implementa filtros de data, tipo e descrição.
 * - Adiciona painel de resumo para o período filtrado.
 * - Altera a tabela para colunas "Entrada" e "Saída" com formatação.
 * - NOVO: Adiciona coluna dataHoraCriacao e ajusta ordenação.
 * - NOVO: Adiciona funcionalidade de Editar e Remover transações.
 * - NOVO: Adiciona exportação de CSV filtrado.
 * - MELHORIA CRÍTICA: Carregamento de dados (atualizarListaTransacoes)
 * movido para uma Task em background para não congelar a UI.
 */
public class FinanceiroController {

    // --- Componentes FXML ---
    @FXML
    private TableView<Transacao> tabelaFinanceiro;
    @FXML
    private TableColumn<Transacao, Integer> colFinId;
    @FXML
    private TableColumn<Transacao, String> colFinDesc;
    @FXML
    private TableColumn<Transacao, String> colFinData;
    @FXML
    private TableColumn<Transacao, String> colFinDataHoraCriacao;
    @FXML
    private TableColumn<Transacao, Double> colFinEntrada;
    @FXML
    private TableColumn<Transacao, Double> colFinSaida;

    // Filtros
    @FXML
    private DatePicker filtroDataInicio;
    @FXML
    private DatePicker filtroDataFim;
    @FXML
    private ComboBox<String> filtroTipo;
    @FXML
    private TextField filtroDescricao;

    // Resumo
    @FXML
    private Label lblTotalReceitasPeriodo;
    @FXML
    private Label lblTotalDespesasPeriodo;
    @FXML
    private Label lblBalancoPeriodo;

    // Botões
    @FXML
    private Button btnEditar;
    @FXML
    private Button btnRemover;
    @FXML
    private Button btnExportarCsv;
    
    // NOVO: Componentes para controle de carregamento
    @FXML
    private ProgressIndicator loadingIndicator; // Indicador de carregamento
    @FXML
    private VBox contentVBox; // Container principal (VBox do FXML)

    // --- Lógica Interna ---
    private final FinanceiroDAO financeiroDAO;
    private final ObservableList<Transacao> dadosTabela; // O que está visível na tabela
    private List<Transacao> listaMestraTransacoes; // Lista completa do banco
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final NumberFormat currencyFormatter;

    public FinanceiroController() {
        financeiroDAO = new FinanceiroDAO();
        dadosTabela = FXCollections.observableArrayList();
        listaMestraTransacoes = new ArrayList<>();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    @FXML
    public void initialize() {
        // Configura colunas
        colFinId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFinData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colFinData.setText("Data Evento"); 
        colFinDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colFinDataHoraCriacao.setCellValueFactory(new PropertyValueFactory<>("dataHoraCriacao"));

        // Coluna de Entrada
        colFinEntrada.setCellValueFactory(cellData -> {
            Transacao t = cellData.getValue();
            return new SimpleObjectProperty<>(t.getValor() > 0 ? t.getValor() : null);
        });
        
        // Coluna de Saída
        colFinSaida.setCellValueFactory(cellData -> {
            Transacao t = cellData.getValue();
            return new SimpleObjectProperty<>(t.getValor() < 0 ? -t.getValor() : null);
        });

        // Formatação de célula para Entrada
        colFinEntrada.setCellFactory(col -> new TableCell<Transacao, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                    getStyleClass().removeAll("positivo-text", "negativo-text");
                } else {
                    setText(currencyFormatter.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                    getStyleClass().removeAll("negativo-text"); 
                    getStyleClass().add("positivo-text"); 
                }
            }
        });

        // Formatação de célula para Saída
        colFinSaida.setCellFactory(col -> new TableCell<Transacao, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                    getStyleClass().removeAll("positivo-text", "negativo-text");
                } else {
                    setText(currencyFormatter.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                    getStyleClass().removeAll("positivo-text"); 
                    getStyleClass().add("negativo-text"); 
                }
            }
        });

        tabelaFinanceiro.setItems(dadosTabela);
        
        // Configura Filtros
        filtroTipo.setItems(FXCollections.observableArrayList("Todos", "Receitas", "Despesas"));
        filtroTipo.getSelectionModel().select("Todos");

        // Adiciona listeners para aplicar filtros automaticamente
        filtroDataInicio.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroDataFim.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroTipo.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroDescricao.textProperty().addListener((o, ov, nv) -> handleAplicarFiltro());

        // Desabilita botões de editar/remover se nada estiver selecionado
        btnEditar.disableProperty().bind(tabelaFinanceiro.getSelectionModel().selectedItemProperty().isNull());
        btnRemover.disableProperty().bind(tabelaFinanceiro.getSelectionModel().selectedItemProperty().isNull());

        // Carrega os dados iniciais (agora assíncrono)
        atualizarListaTransacoes();
    }

    /**
     * NOVO: Controla a visibilidade do indicador de carregamento e
     * desabilita/habilita o conteúdo principal.
     */
    private void showLoading(boolean isLoading) {
        loadingIndicator.setVisible(isLoading);
        loadingIndicator.setManaged(isLoading);
        
        contentVBox.setDisable(isLoading);
        // Opcional: diminui a opacidade para feedback visual
        contentVBox.setOpacity(isLoading ? 0.5 : 1.0); 
    }

    /**
     * ATUALIZADO:
     * Busca todos os dados do banco em uma Task (background thread).
     * Após sucesso, atualiza a lista mestra e aplica o filtro na thread do JavaFX.
     */
    private void atualizarListaTransacoes() {
        // 1. Cria a Task para buscar dados em background
        Task<List<Transacao>> carregarTask = new Task<List<Transacao>>() {
            @Override
            protected List<Transacao> call() throws Exception {
                // Esta é a chamada de banco de dados demorada
                return financeiroDAO.listTransacoes();
            }
        };

        // 2. Define o que fazer quando a Task for bem-sucedida (na JavaFX Thread)
        carregarTask.setOnSucceeded(e -> {
            // Pega o resultado da Task
            listaMestraTransacoes.clear();
            listaMestraTransacoes.addAll(carregarTask.getValue());
            
            // Aplica os filtros (isso é rápido e mexe na UI)
            handleAplicarFiltro();
            
            // Esconde o indicador de carregamento
            showLoading(false);
        });

        // 3. Define o que fazer se a Task falhar (na JavaFX Thread)
        carregarTask.setOnFailed(e -> {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar as transações.");
            carregarTask.getException().printStackTrace();
            // Esconde o indicador de carregamento
            showLoading(false);
        });

        // 4. Mostra o indicador de carregamento ANTES de iniciar a Task
        showLoading(true);

        // 5. Inicia a Task em uma nova Thread
        new Thread(carregarTask).start();
    }


    /**
     * NOVO: Limpa os filtros e recarrega os dados.
     */
    @FXML
    private void handleLimparFiltro() {
        filtroDataInicio.setValue(null);
        filtroDataFim.setValue(null);
        filtroDescricao.clear();
        filtroTipo.getSelectionModel().select("Todos");
        
        // handleAplicarFiltro() é chamado automaticamente pelos listeners
    }

    /**
     * NOVO: Filtra a lista mestra (em memória) e atualiza a tabela e o resumo.
     * Esta operação é rápida e pode ser executada na JavaFX Thread.
     */
    @FXML
    private void handleAplicarFiltro() {
        LocalDate dataInicio = filtroDataInicio.getValue();
        LocalDate dataFim = filtroDataFim.getValue();
        String tipo = filtroTipo.getSelectionModel().getSelectedItem();
        String descricao = filtroDescricao.getText().toLowerCase().trim();

        // 1. Filtra a lista (em memória)
        List<Transacao> filtrada = listaMestraTransacoes.stream()
            .filter(t -> {
                // Filtro por Data (usa a data do evento)
                LocalDate dataTransacao = LocalDate.parse(t.getData(), dateFormatter);
                if (dataInicio != null && dataTransacao.isBefore(dataInicio)) {
                    return false;
                }
                if (dataFim != null && dataTransacao.isAfter(dataFim)) {
                    return false;
                }
                // Filtro por Tipo
                if (tipo != null && !tipo.equals("Todos")) {
                    String tipoTransacao = tipo.equals("Receitas") ? "receita" : "despesa";
                    if (!t.getTipo().equals(tipoTransacao)) {
                        return false;
                    }
                }
                // Filtro por Descrição
                if (!descricao.isEmpty()) {
                    if (!t.getDescricao().toLowerCase().contains(descricao)) {
                        return false;
                    }
                }
                return true;
            })
            .collect(Collectors.toList());

        // 2. Atualiza a tabela (visível)
        dadosTabela.setAll(filtrada);

        // 3. Atualiza o resumo
        atualizarResumo(filtrada);
    }

    /**
     * NOVO: Calcula e exibe o resumo financeiro para a lista filtrada.
     */
    private void atualizarResumo(List<Transacao> transacoesFiltradas) {
        double totalReceitas = 0.0;
        double totalDespesas = 0.0;

        for (Transacao t : transacoesFiltradas) {
            if (t.getTipo().equals("receita")) {
                totalReceitas += t.getValor();
            } else {
                totalDespesas += t.getValor(); // valor já é negativo
            }
        }

        double balanco = totalReceitas + totalDespesas;

        // Atualiza os labels
        lblTotalReceitasPeriodo.setText(currencyFormatter.format(totalReceitas));
        lblTotalDespesasPeriodo.setText(currencyFormatter.format(Math.abs(totalDespesas))); // Mostra positivo
        lblBalancoPeriodo.setText(currencyFormatter.format(balanco));

        // Atualiza estilo do balanço
        lblBalancoPeriodo.getStyleClass().removeAll("positivo-text", "negativo-text");
        if (balanco >= 0) {
            lblBalancoPeriodo.getStyleClass().add("positivo-text");
        } else {
            lblBalancoPeriodo.getStyleClass().add("negativo-text");
        }
    }


    @FXML
    private void handleNovaReceita() {
        abrirDialogoTransacao("receita");
    }

    @FXML
    private void handleNovaDespesa() {
        abrirDialogoTransacao("despesa");
    }

    @FXML
    private void handleEditarTransacao() {
        Transacao selecionada = tabelaFinanceiro.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Selecione uma transação para editar.");
            return;
        }
        abrirDialogoEdicao(selecionada);
    }

    @FXML
    private void handleRemoverTransacao() {
        Transacao selecionada = tabelaFinanceiro.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Selecione uma transação para remover.");
            return;
        }

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Remoção",
            "Tem certeza que deseja remover a transação:\n'" + selecionada.getDescricao() + "' (" + currencyFormatter.format(selecionada.getValor()) + ")?");

        if (confirmado) {
            try {
                // ATENÇÃO: Esta é uma operação de escrita no DB.
                // Para uma performance ideal, isso também deveria
                // ser uma Task. Por simplicidade (é uma operação rápida),
                // mantemos na thread principal por enquanto.
                financeiroDAO.removerTransacao(selecionada.getId());
                AlertUtil.showInfo("Sucesso", "Transação removida.");
                atualizarListaTransacoes(); // Recarrega a lista
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover a transação: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleExportarCsv() {
        if (dadosTabela.isEmpty()) {
            AlertUtil.showInfo("Nada para Exportar", "A tabela está vazia. Não há dados para exportar.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar Relatório Financeiro (Filtrado)");
        fileChooser.setInitialFileName("Relatorio_Financeiro_Filtrado.csv");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Arquivos CSV (*.csv)", "*.csv"));

        File file = fileChooser.showSaveDialog(tabelaFinanceiro.getScene().getWindow());

        if (file == null) {
            return; // Usuário cancelou
        }

        // Tenta escrever o arquivo
        try (PrintWriter writer = new PrintWriter(file, "UTF-8")) {
            StringBuilder sb = new StringBuilder();
            
            // Cabeçalho do CSV
            sb.append("ID;Data Evento;Data Lancamento;Descricao;Tipo;Entrada (R$);Saida (R$)\n");

            // Escreve os dados (usando a lista filtrada 'dadosTabela')
            for (Transacao t : dadosTabela) {
                double entrada = t.getValor() > 0 ? t.getValor() : 0.0;
                double saida = t.getValor() < 0 ? -t.getValor() : 0.0; // Valor absoluto

                sb.append(String.format(Locale.US, "%d;%s;%s;\"%s\";%s;%.2f;%.2f\n",
                    t.getId(),
                    t.getData(),
                    t.getDataHoraCriacao(),
                    t.getDescricao().replace("\"", "\"\""), // Escapa aspas
                    t.getTipo(),
                    entrada,
                    saida
                ));
            }

            // Adiciona o resumo
            sb.append("\n--- Resumo do Periodo Filtrado ---\n");
            sb.append("Total Receitas;").append(lblTotalReceitasPeriodo.getText()).append("\n");
            sb.append("Total Despesas;").append(lblTotalDespesasPeriodo.getText()).append("\n");
            sb.append("Balanco;").append(lblBalancoPeriodo.getText()).append("\n");

            writer.write(sb.toString());
            AlertUtil.showInfo("Sucesso", "Relatório CSV (Filtrado) exportado com sucesso para:\n" + file.getAbsolutePath());

        } catch (IOException e) {
            AlertUtil.showError("Erro ao Exportar", "Não foi possível gerar o arquivo CSV: " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Método auxiliar para abrir um diálogo de transação (Receita ou Despesa).
     * @param tipo "receita" ou "despesa"
     */
    private void abrirDialogoTransacao(String tipo) {
        boolean isReceita = tipo.equals("receita");
        
        Dialog<Transacao> dialog = new Dialog<>();
        dialog.setTitle(isReceita ? "Adicionar Nova Receita" : "Adicionar Nova Despesa");
        dialog.setHeaderText("Preencha os dados da transação.");

        ButtonType adicionarButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField descField = new TextField();
        descField.setPromptText(isReceita ? "Ex: Venda de Soja" : "Ex: Compra de Diesel");
        TextField valorField = new TextField();
        valorField.setPromptText("Ex: 5000.00");
        DatePicker dataPicker = new DatePicker(LocalDate.now()); 

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);
        grid.add(new Label("Data Evento:"), 0, 2); 
        grid.add(dataPicker, 1, 2); 

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String desc = descField.getText();
                    double valor = Double.parseDouble(valorField.getText().replace(",", "."));
                    LocalDate data = dataPicker.getValue(); 

                    if (desc.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "A descrição é obrigatória.");
                        return null;
                    }
                     if (data == null) { 
                        AlertUtil.showError("Erro de Validação", "A data é obrigatória.");
                        return null;
                    }
                    if (valor <= 0) {
                         AlertUtil.showError("Erro de Validação", "O valor deve ser positivo.");
                         return null;
                    }

                    double valorFinal = isReceita ? valor : -valor;
                    
                    return new Transacao(desc, valorFinal, data.format(dateFormatter), tipo); // Data formatada
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<Transacao> result = dialog.showAndWait();

        result.ifPresent(transacao -> {
            try {
                // Operação de escrita - idealmente também uma Task, mas rápida.
                financeiroDAO.addTransacao(transacao);
                atualizarListaTransacoes(); // Recarrega a lista
                AlertUtil.showInfo("Sucesso", "Transação adicionada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar a transação: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Método auxiliar para abrir um diálogo de EDIÇÃO de transação.
     */
    private void abrirDialogoEdicao(Transacao transacao) {
        Dialog<Transacao> dialog = new Dialog<>();
        dialog.setTitle("Editar Lançamento");
        dialog.setHeaderText("Modifique os dados da transação.");

        ButtonType salvarButtonType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(salvarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField descField = new TextField(transacao.getDescricao());
        TextField valorField = new TextField(String.format(Locale.US, "%.2f", Math.abs(transacao.getValor()))); 
        DatePicker dataPicker = new DatePicker(LocalDate.parse(transacao.getData(), dateFormatter));

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);
        grid.add(new Label("Data Evento:"), 0, 2);
        grid.add(dataPicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == salvarButtonType) {
                try {
                    String desc = descField.getText();
                    double valorAbsoluto = Double.parseDouble(valorField.getText().replace(",", "."));
                    LocalDate data = dataPicker.getValue();

                    if (desc.isEmpty() || data == null || valorAbsoluto <= 0) {
                         AlertUtil.showError("Erro de Validação", "Todos os campos são obrigatórios e o valor deve ser positivo.");
                         return null;
                    }

                    double valorFinal = transacao.getTipo().equals("receita") ? valorAbsoluto : -valorAbsoluto;

                    return new Transacao(
                        transacao.getId(),
                        desc,
                        valorFinal,
                        data.format(dateFormatter),
                        transacao.getTipo(), 
                        transacao.getDataHoraCriacao()
                    );
                    
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<Transacao> result = dialog.showAndWait();

        result.ifPresent(transacaoEditada -> {
            try {
                // Operação de escrita
                financeiroDAO.updateTransacao(transacaoEditada);
                atualizarListaTransacoes(); // Recarrega a lista
                AlertUtil.showInfo("Sucesso", "Transação atualizada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar a transação: " + e.getMessage());
            }
        });
    }
}
