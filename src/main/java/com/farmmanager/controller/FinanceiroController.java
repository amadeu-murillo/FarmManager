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
        filtroTipo.setItems(FXCollections.observableArrayList("Todos", "Receitas", "Despesas"));
        filtroTipo.getSelectionModel().select("Todos");

        // Adiciona listeners para aplicar filtros automaticamente
        filtroDataInicio.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroDataFim.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroTipo.valueProperty().addListener((o, ov, nv) -> handleAplicarFiltro());
        filtroDescricao.textProperty().addListener((o, ov, nv) -> handleAplicarFiltro());

        // Carrega os dados iniciais
        atualizarListaTransacoes();
    }

    /**
     * Busca todos os dados do banco para a lista mestra e, em seguida, aplica o filtro.
     */
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
    @FXML
    private void handleNovaReceita() {
        abrirDialogoTransacao("receita");
    }

    /**
     * NOVO: Manipulador para o botão "- Nova Despesa".
     */
    @FXML
    private void handleNovaDespesa() {
        abrirDialogoTransacao("despesa");
    }

    /**
     * NOVO: Método auxiliar para abrir um diálogo de transação (Receita ou Despesa).
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

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String desc = descField.getText();
                    double valor = Double.parseDouble(valorField.getText().replace(",", "."));
                    String data = LocalDate.now().format(dateFormatter);

                    if (desc.isEmpty()) {
                        AlertUtil.showError("Erro de Validação", "A descrição é obrigatória.");
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
                    return new Transacao(desc, valorFinal, data, tipo);
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
}

