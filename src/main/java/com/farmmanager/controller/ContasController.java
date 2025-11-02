package com.farmmanager.controller;

import com.farmmanager.model.Conta;
import com.farmmanager.model.ContaDAO;
import com.farmmanager.util.AlertUtil;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
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
import javafx.scene.paint.Color;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * NOVO: Controller para a ContasView.fxml (Contas a Pagar/Receber).
 * ATUALIZADO:
 * - Adicionado destaque visual para contas vencidas.
 * - Adicionada funcionalidade de editar contas pendentes.
 * - MELHORIA CRÍTICA: Carregamento de dados (carregarDados)
 * movido para uma Task em background para não congelar a UI.
 * - ATUALIZADO: Adicionados campos de fornecedor.
 */
public class ContasController {

    @FXML
    private TableView<Conta> tabelaContas;
    @FXML
    private TableColumn<Conta, Integer> colId;
    @FXML
    private TableColumn<Conta, String> colDescricao;
    @FXML
    private TableColumn<Conta, String> colFornecedorNome; // NOVO
    @FXML
    private TableColumn<Conta, String> colFornecedorEmpresa; // NOVO
    @FXML
    private TableColumn<Conta, String> colDataVencimento;
    @FXML
    private TableColumn<Conta, String> colTipo;
    @FXML
    private TableColumn<Conta, Double> colValor;
    @FXML
    private TableColumn<Conta, String> colStatus;
    @FXML
    private TableColumn<Conta, String> colDataCriacao;
    
    @FXML
    private Label lblTotalReceber;
    @FXML
    private Label lblTotalPagar;
    
    @FXML
    private ComboBox<String> filtroStatus;
    
    @FXML
    private Button btnLiquidar;
    @FXML
    private Button btnRemover;
    @FXML
    private Button btnEditar; 

    // NOVO: Componentes para controle de carregamento
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private VBox contentVBox; // Container principal (VBox do FXML)

    private final ContaDAO contaDAO;
    private final ObservableList<Conta> dadosTabela;
    private final NumberFormat currencyFormatter;

    public ContasController() {
        contaDAO = new ContaDAO();
        dadosTabela = FXCollections.observableArrayList();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    /**
     * NOVO: Classe interna para agrupar os resultados da Task
     * (pois precisamos buscar 3 informações do banco).
     */
    private static class ContasData {
        final List<Conta> contas;
        final double totalPagar;
        final double totalReceber;

        ContasData(List<Conta> contas, double totalPagar, double totalReceber) {
            this.contas = contas;
            this.totalPagar = totalPagar;
            this.totalReceber = totalReceber;
        }
    }

    @FXML
    public void initialize() {
        // Configura colunas
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colFornecedorNome.setCellValueFactory(new PropertyValueFactory<>("fornecedorNome")); // NOVO
        colFornecedorEmpresa.setCellValueFactory(new PropertyValueFactory<>("fornecedorEmpresa")); // NOVO
        colDataVencimento.setCellValueFactory(new PropertyValueFactory<>("dataVencimento"));
        colTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        colValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colDataCriacao.setCellValueFactory(new PropertyValueFactory<>("dataCriacao"));
        
        tabelaContas.setItems(dadosTabela);

        // Formatação customizada para Valor (R$)
        colValor.setCellFactory(col -> new TableCell<Conta, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(currencyFormatter.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                }
            }
        });

        // Formatação customizada para Tipo (Pagar/Receber)
        colTipo.setCellFactory(col -> new TableCell<Conta, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setTextFill(Color.BLACK); 
                } else {
                    setText(item.equalsIgnoreCase("pagar") ? "A Pagar" : "A Receber");
                    if (item.equalsIgnoreCase("pagar")) {
                        setTextFill(Color.web("#E53E3E")); // Vermelho
                    } else {
                        setTextFill(Color.web("#38A169")); // Verde
                    }
                }
            }
        });

        // Destaque visual para contas vencidas
        tabelaContas.setRowFactory(tv -> new TableRow<Conta>() {
            @Override
            protected void updateItem(Conta item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("table-row-cell:warning");

                if (empty || item == null) {
                    setStyle("");
                } else if (item.getStatus().equalsIgnoreCase("pendente")) {
                    LocalDate dataVencimento = LocalDate.parse(item.getDataVencimento());
                    if (dataVencimento.isBefore(LocalDate.now())) {
                        if (!getStyleClass().contains("table-row-cell:warning")) {
                            getStyleClass().add("table-row-cell:warning");
                        }
                    }
                }
            }
        });

        // Configura Filtro de Status
        filtroStatus.setItems(FXCollections.observableArrayList(
            "Pendente", "Pago", "Todos"
        ));
        filtroStatus.getSelectionModel().select("Pendente");
        filtroStatus.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldV, newV) -> carregarDados());

        // Desabilita botões se nada estiver selecionado
        btnRemover.disableProperty().bind(tabelaContas.getSelectionModel().selectedItemProperty().isNull());
        
        ReadOnlyObjectProperty<Conta> selectedItem = tabelaContas.getSelectionModel().selectedItemProperty();

        BooleanBinding disableEditAndLiquidarBinding = Bindings.createBooleanBinding(() -> {
            Conta conta = selectedItem.get();
            if (conta == null) {
                return true; 
            }
            return !conta.getStatus().equals("pendente"); // Desabilita se não for "pendente"
        }, selectedItem); 

        btnEditar.disableProperty().bind(disableEditAndLiquidarBinding);
        btnLiquidar.disableProperty().bind(disableEditAndLiquidarBinding);

        // Carrega os dados (agora assíncrono)
        carregarDados();
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
     * ATUALIZADO:
     * Executa a busca de dados (lista e resumos) em uma Task (background thread).
     * Atualiza a UI na JavaFX Thread quando a busca termina.
     */
    private void carregarDados() {
        // Pega o filtro ANTES de iniciar a thread
        String filtro = filtroStatus.getSelectionModel().getSelectedItem();
        if (filtro == null) filtro = "Pendente";
        final String filtroFinal = filtro; // 'final' para ser usado na Task

        // 1. Cria a Task
        Task<ContasData> carregarTask = new Task<ContasData>() {
            @Override
            protected ContasData call() throws Exception {
                // Chamadas de banco de dados (demoradas)
                List<Conta> lista = contaDAO.listContas(filtroFinal);
                double totalPagar = contaDAO.getTotalPendente("pagar");
                double totalReceber = contaDAO.getTotalPendente("receber");

                // Retorna o objeto com todos os dados
                return new ContasData(lista, totalPagar, totalReceber);
            }
        };

        // 2. Define o que fazer em caso de SUCESSO (na JavaFX Thread)
        carregarTask.setOnSucceeded(e -> {
            ContasData data = carregarTask.getValue();

            // Atualiza a tabela
            dadosTabela.clear();
            dadosTabela.addAll(data.contas);
            
            // Atualiza os resumos
            lblTotalPagar.setText(currencyFormatter.format(data.totalPagar));
            lblTotalReceber.setText(currencyFormatter.format(data.totalReceber));

            // Esconde o loading
            showLoading(false);
        });

        // 3. Define o que fazer em caso de FALHA (na JavaFX Thread)
        carregarTask.setOnFailed(e -> {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar as contas.");
            carregarTask.getException().printStackTrace();
            
            // Limpa os dados
            dadosTabela.clear();
            lblTotalPagar.setText("Erro");
            lblTotalReceber.setText("Erro");
            
            // Esconde o loading
            showLoading(false);
        });

        // 4. Mostra o loading ANTES de iniciar a Task
        showLoading(true);

        // 5. Inicia a Task em uma nova Thread
        new Thread(carregarTask).start();
    }

    // O método antigo atualizarResumo() não é mais necessário,
    // pois sua lógica foi movida para dentro da Task carregarDados().

    @FXML
    private void handleAdicionarPagar() {
        abrirDialogoCadastro("pagar");
    }

    @FXML
    private void handleAdicionarReceber() {
        abrirDialogoCadastro("receber");
    }

    private void abrirDialogoCadastro(String tipo) {
        Dialog<Conta> dialog = new Dialog<>();
        dialog.setTitle(tipo.equals("pagar") ? "Nova Conta a Pagar" : "Nova Conta a Receber");
        dialog.setHeaderText("Preencha os dados do lançamento futuro.");

        ButtonType adicionarButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField descField = new TextField();
        descField.setPromptText("Ex: Compra de fertilizante / Venda futura");
        TextField valorField = new TextField();
        valorField.setPromptText("Ex: 1500.00");
        DatePicker dataVencimentoPicker = new DatePicker(LocalDate.now().plusDays(30));
        
        TextField fornecedorNomeField = new TextField(); // NOVO
        fornecedorNomeField.setPromptText("Ex: João da Silva (Opcional)"); // NOVO
        TextField fornecedorEmpresaField = new TextField(); // NOVO
        fornecedorEmpresaField.setPromptText("Ex: Agropecuária XYZ (Opcional)"); // NOVO

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);
        grid.add(new Label("Data Vencimento:"), 0, 2);
        grid.add(dataVencimentoPicker, 1, 2);
        grid.add(new Label("Fornecedor (Nome):"), 0, 3); // NOVO
        grid.add(fornecedorNomeField, 1, 3); // NOVO
        grid.add(new Label("Fornecedor (Empresa):"), 0, 4); // NOVO
        grid.add(fornecedorEmpresaField, 1, 4); // NOVO

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String desc = descField.getText();
                    double valor = Double.parseDouble(valorField.getText().replace(",", "."));
                    LocalDate data = dataVencimentoPicker.getValue();
                    String fornNome = fornecedorNomeField.getText(); // NOVO
                    String fornEmpresa = fornecedorEmpresaField.getText(); // NOVO

                    if (desc.isEmpty() || data == null || valor <= 0) {
                        AlertUtil.showError("Erro de Validação", "Descrição, Data e Valor (> 0) são obrigatórios.");
                        return null;
                    }
                    
                    return new Conta(desc, valor, data.toString(), tipo, "pendente", fornNome, fornEmpresa);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<Conta> result = dialog.showAndWait();

        result.ifPresent(conta -> {
            try {
                // Operação de escrita (rápida, mantida na FX thread por simplicidade)
                contaDAO.addConta(conta);
                carregarDados(); // Recarrega os dados (agora assíncrono)
                AlertUtil.showInfo("Sucesso", "Conta adicionada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar a conta: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleEditarConta() {
        Conta selecionada = tabelaContas.getSelectionModel().getSelectedItem();
        if (selecionada == null || !selecionada.getStatus().equals("pendente")) {
            AlertUtil.showError("Ação Inválida", "Selecione uma conta 'pendente' para editar.");
            return;
        }
        abrirDialogoEdicao(selecionada);
    }

    private void abrirDialogoEdicao(Conta conta) {
        Dialog<Conta> dialog = new Dialog<>();
        dialog.setTitle("Editar Lançamento");
        dialog.setHeaderText("Modifique os dados da conta pendente.");

        ButtonType salvarButtonType = new ButtonType("Salvar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(salvarButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField descField = new TextField(conta.getDescricao());
        TextField valorField = new TextField(String.format(Locale.US, "%.2f", conta.getValor()));
        DatePicker dataVencimentoPicker = new DatePicker(LocalDate.parse(conta.getDataVencimento()));
        ComboBox<String> tipoCombo = new ComboBox<>(FXCollections.observableArrayList("pagar", "receber"));
        tipoCombo.setValue(conta.getTipo());
        
        TextField fornecedorNomeField = new TextField(conta.getFornecedorNome()); // NOVO
        TextField fornecedorEmpresaField = new TextField(conta.getFornecedorEmpresa()); // NOVO

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);
        grid.add(new Label("Data Vencimento:"), 0, 2);
        grid.add(dataVencimentoPicker, 1, 2);
        grid.add(new Label("Tipo:"), 0, 3);
        grid.add(tipoCombo, 1, 3);
        grid.add(new Label("Fornecedor (Nome):"), 0, 4); // NOVO
        grid.add(fornecedorNomeField, 1, 4); // NOVO
        grid.add(new Label("Fornecedor (Empresa):"), 0, 5); // NOVO
        grid.add(fornecedorEmpresaField, 1, 5); // NOVO

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == salvarButtonType) {
                try {
                    String desc = descField.getText();
                    double valor = Double.parseDouble(valorField.getText().replace(",", "."));
                    LocalDate data = dataVencimentoPicker.getValue();
                    String tipo = tipoCombo.getValue();
                    String fornNome = fornecedorNomeField.getText(); // NOVO
                    String fornEmpresa = fornecedorEmpresaField.getText(); // NOVO

                    if (desc.isEmpty() || data == null || valor <= 0) {
                        AlertUtil.showError("Erro de Validação", "Todos os campos são obrigatórios e o valor deve ser positivo.");
                        return null;
                    }
                    
                    conta.setDescricao(desc);
                    conta.setValor(valor);
                    conta.setDataVencimento(data.toString());
                    conta.setTipo(tipo);
                    conta.setFornecedorNome(fornNome); // NOVO
                    conta.setFornecedorEmpresa(fornEmpresa); // NOVO
                    
                    return conta;
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<Conta> result = dialog.showAndWait();

        result.ifPresent(contaEditada -> {
            try {
                // Operação de escrita (rápida)
                contaDAO.updateConta(contaEditada);
                carregarDados(); // Recarrega os dados (agora assíncrono)
                AlertUtil.showInfo("Sucesso", "Conta atualizada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar a conta: " + e.getMessage());
            }
        });
    }

    @FXML
    private void handleLiquidar() {
        Conta selecionada = tabelaContas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Selecione uma conta para liquidar.");
            return;
        }

        if (selecionada.getStatus().equalsIgnoreCase("pago")) {
            AlertUtil.showInfo("Ação Inválida", "Esta conta já foi paga/recebida.");
            return;
        }

        DatePickerDialog dialog = new DatePickerDialog();
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone
        Optional<LocalDate> result = dialog.showAndWait();

        if (result.isPresent()) {
            try {
                // Operação de escrita (transação de DB, pode demorar um pouco)
                // Idealmente, também seria uma Task, mas por simplicidade:
                String dataPagamento = result.get().toString();
                contaDAO.liquidarConta(selecionada.getId(), dataPagamento);
                
                AlertUtil.showInfo("Sucesso", "Conta liquidada com sucesso.\nUm lançamento foi gerado no Financeiro.");
                carregarDados(); // Recarrega a lista
                
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível liquidar a conta: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleRemover() {
        Conta selecionada = tabelaContas.getSelectionModel().getSelectedItem();
        if (selecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Selecione uma conta para remover.");
            return;
        }

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Remoção (Ajuste)",
            "Tem certeza que deseja remover este lançamento?\n\nEsta ação é um AJUSTE e não afetará o caixa.");

        if (confirmado) {
            try {
                // Operação de escrita (rápida)
                contaDAO.removerConta(selecionada.getId());
                carregarDados(); // Recarrega
                AlertUtil.showInfo("Sucesso", "Lançamento removido.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover: " + e.getMessage());
            }
        }
    }

    /**
     * Classe interna simples para um diálogo de DatePicker.
     */
    private static class DatePickerDialog extends Dialog<LocalDate> {
        public DatePickerDialog() {
            setTitle("Data da Liquidação");
            setHeaderText("Quando esta conta foi paga/recebida?");

            ButtonType liquidarButtonType = new ButtonType("Liquidar", ButtonBar.ButtonData.OK_DONE);
            getDialogPane().getButtonTypes().addAll(liquidarButtonType, ButtonType.CANCEL);

            DatePicker datePicker = new DatePicker(LocalDate.now());
            getDialogPane().setContent(datePicker);

            setResultConverter(dialogButton -> {
                if (dialogButton == liquidarButtonType) {
                    return datePicker.getValue();
                }
                return null;
            });
        }
    }
}
