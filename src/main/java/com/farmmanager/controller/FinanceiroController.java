package com.farmmanager.controller;

import com.farmmanager.model.Transacao;
import com.farmmanager.model.FinanceiroDAO;
import com.farmmanager.util.AlertUtil;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser; // NOVO

import java.io.File; // NOVO
import java.io.IOException; // NOVO
import java.io.PrintWriter; // NOVO
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
 */
public class FinanceiroController {

    // --- Componentes FXML ---
    @FXML
    private TableView<Transacao> tabelaFinanceiro;
    @FXML
    private TableColumn<Transacao, Integer> colFinId;
// ... (outras colunas FXML) ...
    @FXML
    private TableColumn<Transacao, String> colFinDesc;
    @FXML
    private TableColumn<Transacao, String> colFinData;
    
    // NOVO: Coluna de Data de Lançamento
    @FXML
    private TableColumn<Transacao, String> colFinDataHoraCriacao;

    // Colunas de valor atualizadas
    @FXML
    private TableColumn<Transacao, Double> colFinEntrada;
    @FXML
    private TableColumn<Transacao, Double> colFinSaida;

    // Novos filtros
    @FXML
    private DatePicker filtroDataInicio;
    @FXML
    private DatePicker filtroDataFim;
    @FXML
    private ComboBox<String> filtroTipo;
    @FXML
    private TextField filtroDescricao;

    // Novos labels de resumo
    @FXML
    private Label lblTotalReceitasPeriodo;
    @FXML
    private Label lblTotalDespesasPeriodo;
    @FXML
    private Label lblBalancoPeriodo;

    // NOVO: Botões de Editar/Remover
    @FXML
    private Button btnEditar;
    @FXML
    private Button btnRemover;

    // NOVO: Botão de Exportar
    @FXML
    private Button btnExportarCsv;

    // --- Lógica Interna ---
    private final FinanceiroDAO financeiroDAO;
// ... (resto dos campos) ...
    private final ObservableList<Transacao> dadosTabela; // O que está visível na tabela
    private List<Transacao> listaMestraTransacoes; // Lista completa do banco
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final NumberFormat currencyFormatter;

    public FinanceiroController() {
// ... (construtor) ...
        financeiroDAO = new FinanceiroDAO();
        dadosTabela = FXCollections.observableArrayList();
        listaMestraTransacoes = new ArrayList<>();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    @FXML
    public void initialize() {
// ... (inicialização das colunas) ...
        // Configura colunas
        colFinId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFinData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colFinData.setText("Data Evento"); // Renomeia o label da coluna existente
        colFinDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        
        // NOVO: Configura a nova coluna
        colFinDataHoraCriacao.setCellValueFactory(new PropertyValueFactory<>("dataHoraCriacao"));

        // --- Configuração das colunas customizadas de Entrada/Saída ---

        // Coluna de Entrada
        colFinEntrada.setCellValueFactory(cellData -> {
            Transacao t = cellData.getValue();
            // Retorna o valor apenas se for positivo (receita)
            return new SimpleObjectProperty<>(t.getValor() > 0 ? t.getValor() : null);
        });
        
        // Coluna de Saída
        colFinSaida.setCellValueFactory(cellData -> {
            Transacao t = cellData.getValue();
            // Retorna o valor (absoluto) apenas se for negativo (despesa)
            return new SimpleObjectProperty<>(t.getValor() < 0 ? -t.getValor() : null);
        });

        // Formatação de célula para Entrada (verde, alinhado à direita)
        colFinEntrada.setCellFactory(col -> new TableCell<Transacao, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                    // Limpa o estilo para não vazar para outras células
                    getStyleClass().removeAll("positivo-text", "negativo-text");
                } else {
                    setText(currencyFormatter.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                    getStyleClass().removeAll("negativo-text"); // Garante que não tenha a classe errada
                    getStyleClass().add("positivo-text"); // Adiciona classe CSS
                }
            }
        });

        // Formatação de célula para Saída (vermelho, alinhado à direita)
        colFinSaida.setCellFactory(col -> new TableCell<Transacao, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                    // Limpa o estilo para não vazar para outras células
                    getStyleClass().removeAll("positivo-text", "negativo-text");
                } else {
                    setText(currencyFormatter.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                    getStyleClass().removeAll("positivo-text"); // Garante que não tenha a classe errada
                    getStyleClass().add("negativo-text"); // Adiciona classe CSS
                }
            }
        });

        tabelaFinanceiro.setItems(dadosTabela);
        
        // Configura Filtros
// ... (configuração dos filtros) ...
        filtroTipo.setItems(FXCollections.observableArrayList("Todos", "Receitas", "Despesas"));
        filtroTipo.getSelectionModel().select("Todos");

        // Adiciona listeners para aplicar filtros automaticamente
        filtroDataInicio.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroDataFim.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroTipo.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroDescricao.textProperty().addListener((o, ov, nv) -> handleAplicarFiltro());

        // NOVO: Desabilita botões de editar/remover se nada estiver selecionado
        btnEditar.disableProperty().bind(tabelaFinanceiro.getSelectionModel().selectedItemProperty().isNull());
        btnRemover.disableProperty().bind(tabelaFinanceiro.getSelectionModel().selectedItemProperty().isNull());

        // Carrega os dados iniciais
        atualizarListaTransacoes();
    }

    /**
     * Busca todos os dados do banco para a lista mestra e, em seguida, aplica o filtro.
     */
// ... (atualizarListaTransacoes) ...
    private void atualizarListaTransacoes() {
        try {
            listaMestraTransacoes.clear();
            listaMestraTransacoes.addAll(financeiroDAO.listTransacoes());
            handleAplicarFiltro(); // Aplica o filtro (que atualiza a tabela e o resumo)
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar as transações.");
        }
    }

    /**
     * NOVO: Limpa os filtros e recarrega os dados.
     */
    @FXML
// ... (handleLimparFiltro) ...
    private void handleLimparFiltro() {
        // Limpa os campos. A mudança de valor nos listeners vai disparar a atualização.
        filtroDataInicio.setValue(null);
        filtroDataFim.setValue(null);
        filtroDescricao.clear();
        filtroTipo.getSelectionModel().select("Todos"); // Isso deve disparar o handleAplicarFiltro
    }

    /**
     * NOVO: Filtra a lista mestra com base nos filtros e atualiza a tabela e o resumo.
     */
    @FXML
// ... (handleAplicarFiltro) ...
    private void handleAplicarFiltro() {
        LocalDate dataInicio = filtroDataInicio.getValue();
        LocalDate dataFim = filtroDataFim.getValue();
        String tipo = filtroTipo.getSelectionModel().getSelectedItem();
        String descricao = filtroDescricao.getText().toLowerCase().trim();

        // 1. Filtra a lista
        List<Transacao> filtrada = listaMestraTransacoes.stream()
            .filter(t -> {
                // Filtro por Data (usa a data do evento, não a de criação)
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

        // 2. Atualiza a tabela
        dadosTabela.setAll(filtrada);

        // 3. Atualiza o resumo
        atualizarResumo(filtrada);
    }

    /**
     * NOVO: Calcula e exibe o resumo financeiro para a lista filtrada.
     */
// ... (atualizarResumo) ...
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


    /**
     * NOVO: Manipulador para o botão "+ Nova Receita".
     */
// ... (handleNovaReceita) ...
    @FXML
    private void handleNovaReceita() {
        abrirDialogoTransacao("receita");
    }

    /**
     * NOVO: Manipulador para o botão "- Nova Despesa".
     */
// ... (handleNovaDespesa) ...
    @FXML
    private void handleNovaDespesa() {
        abrirDialogoTransacao("despesa");
    }

    /**
     * NOVO: Manipulador para o botão "Editar Lançamento".
     */
// ... (handleEditarTransacao) ...
    @FXML
    private void handleEditarTransacao() {
        Transacao selecionada = tabelaFinanceiro.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Selecione uma transação para editar.");
            return;
        }
        
        // Chamar o novo método de diálogo
        abrirDialogoEdicao(selecionada);
    }

    /**
     * NOVO: Manipulador para o botão "Remover Lançamento".
     */
// ... (handleRemoverTransacao) ...
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
                financeiroDAO.removerTransacao(selecionada.getId());
                AlertUtil.showInfo("Sucesso", "Transação removida.");
                atualizarListaTransacoes(); // Recarrega a lista
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover a transação: " + e.getMessage());
            }
        }
    }

    /**
     * NOVO: Manipulador para exportar os dados filtrados para CSV.
     */
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
     * NOVO: Método auxiliar para abrir um diálogo de transação (Receita ou Despesa).
     * @param tipo "receita" ou "despesa"
     */
// ... (abrirDialogoTransacao) ...
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
        DatePicker dataPicker = new DatePicker(LocalDate.now()); // NOVO: Data selecionável

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);
        grid.add(new Label("Data Evento:"), 0, 2); // NOVO
        grid.add(dataPicker, 1, 2); // NOVO

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String desc = descField.getText();
                    double valor = Double.parseDouble(valorField.getText().replace(",", "."));
                    LocalDate data = dataPicker.getValue(); // NOVO

                    if (desc.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "A descrição é obrigatória.");
                        return null;
                    }
                     if (data == null) { // NOVO
                        AlertUtil.showError("Erro de Validação", "A data é obrigatória.");
                        return null;
                    }
                    if (valor <= 0) {
                         AlertUtil.showError("Erro de Validação", "O valor deve ser positivo.");
                         return null;
                    }

                    // Se for despesa, inverte o valor para negativo
                    double valorFinal = isReceita ? valor : -valor;
                    
                    // USA O CONSTRUTOR SEM dataHoraCriacao (será nulo no objeto)
                    // O DAO irá preencher no banco, e o atualizarListaTransacoes() irá reler
                    // o objeto completo, incluindo o dataHoraCriacao.
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
                financeiroDAO.addTransacao(transacao);
                atualizarListaTransacoes(); // ATUALIZADO: Recarrega a lista mestra e aplica o filtro
                AlertUtil.showInfo("Sucesso", "Transação adicionada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar a transação: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Método auxiliar para abrir um diálogo de EDIÇÃO de transação.
     */
// ... (abrirDialogoEdicao) ...
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

        // Preenche os campos com os dados existentes
        TextField descField = new TextField(transacao.getDescricao());
        
        // O valor no DB é negativo para despesa, mas o usuário edita o valor absoluto (positivo)
        TextField valorField = new TextField(String.format(Locale.US, "%.2f", Math.abs(transacao.getValor()))); 
        
        DatePicker dataPicker = new DatePicker(LocalDate.parse(transacao.getData(), dateFormatter));

        // O tipo (receita/despesa) não será editável aqui.
        // Se o usuário errou o tipo, ele deve remover e adicionar novamente.
        // Isso simplifica a lógica de edição do valor.

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);
        grid.add(new Label("Data Evento:"), 0, 2);
        grid.add(dataPicker, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == salvarButtonType) {
                try {
                    String desc = descField.getText();
                    double valorAbsoluto = Double.parseDouble(valorField.getText().replace(",", "."));
                    LocalDate data = dataPicker.getValue();

                    if (desc.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "A descrição é obrigatória.");
                        return null;
                    }
                     if (data == null) {
                        AlertUtil.showError("Erro de Validação", "A data é obrigatória.");
                        return null;
                    }
                    if (valorAbsoluto <= 0) {
                         AlertUtil.showError("Erro de Validação", "O valor deve ser positivo.");
                         return null;
                    }

                    // Mantém o sinal original (receita > 0, despesa < 0)
                    double valorFinal = transacao.getTipo().equals("receita") ? valorAbsoluto : -valorAbsoluto;

                    // Retorna uma *nova* instância de Transacao com o ID original
                    return new Transacao(
                        transacao.getId(),
                        desc,
                        valorFinal,
                        data.format(dateFormatter),
                        transacao.getTipo(), // Mantém o tipo original
                        transacao.getDataHoraCriacao() // Passa o valor antigo (não será usado pelo DAO de update)
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
                financeiroDAO.updateTransacao(transacaoEditada);
                atualizarListaTransacoes(); // Recarrega a lista
                AlertUtil.showInfo("Sucesso", "Transação atualizada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar a transação: " + e.getMessage());
            }
        });
    }
}

