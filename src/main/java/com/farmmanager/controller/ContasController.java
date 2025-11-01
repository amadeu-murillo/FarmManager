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
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
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
 */
public class ContasController {

    @FXML
    private TableView<Conta> tabelaContas;
    @FXML
    private TableColumn<Conta, Integer> colId;
    @FXML
    private TableColumn<Conta, String> colDescricao;
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
    private Button btnEditar; // NOVO

    private final ContaDAO contaDAO;
    private final ObservableList<Conta> dadosTabela;
    private final NumberFormat currencyFormatter;

    public ContasController() {
        contaDAO = new ContaDAO();
        dadosTabela = FXCollections.observableArrayList();
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    @FXML
    public void initialize() {
        // Configura colunas
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colDescricao.setCellValueFactory(new PropertyValueFactory<>("descricao"));
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
                    setTextFill(Color.BLACK); // Reseta a cor
                } else {
                    setText(item.equalsIgnoreCase("pagar") ? "A Pagar" : "A Receber");
                    if (item.equalsIgnoreCase("pagar")) {
                        setTextFill(Color.web("#E53E3E")); // Vermelho (negativo-text)
                    } else {
                        setTextFill(Color.web("#38A169")); // Verde (positivo-text)
                    }
                }
            }
        });

        // NOVO: Destaque visual para contas vencidas
        tabelaContas.setRowFactory(tv -> new TableRow<Conta>() {
            @Override
            protected void updateItem(Conta item, boolean empty) {
                super.updateItem(item, empty);
                // Limpa estilos anteriores
                getStyleClass().remove("table-row-cell:warning");

                if (empty || item == null) {
                    setStyle("");
                } else if (item.getStatus().equalsIgnoreCase("pendente")) {
                    LocalDate dataVencimento = LocalDate.parse(item.getDataVencimento());
                    if (dataVencimento.isBefore(LocalDate.now())) {
                        // Aplica a classe CSS de aviso (amarelo)
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
        
        // NOVO: Desabilita 'Editar' e 'Liquidar' se a conta não estiver pendente
        
        ReadOnlyObjectProperty<Conta> selectedItem = tabelaContas.getSelectionModel().selectedItemProperty();

        // Este binding será 'true' (desabilitar) se:
        // 1. A seleção for nula (conta == null)
        // 2. A conta selecionada NÃO tiver o status "pendente"
        BooleanBinding disableEditAndLiquidarBinding = Bindings.createBooleanBinding(() -> {
            Conta conta = selectedItem.get();
            if (conta == null) {
                return true; // Desabilita se for nulo
            }
            return !conta.getStatus().equals("pendente"); // Desabilita se não for "pendente"
        }, selectedItem); // Depende da seleção

        btnEditar.disableProperty().bind(disableEditAndLiquidarBinding);
        btnLiquidar.disableProperty().bind(disableEditAndLiquidarBinding);


        carregarDados();
    }

    private void carregarDados() {
        try {
            String filtro = filtroStatus.getSelectionModel().getSelectedItem();
            if (filtro == null) filtro = "Pendente";
            
            List<Conta> lista = contaDAO.listContas(filtro);
            dadosTabela.clear();
            dadosTabela.addAll(lista);
            
            atualizarResumo();
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar as contas.");
            e.printStackTrace();
        }
    }

    private void atualizarResumo() {
        try {
            double totalPagar = contaDAO.getTotalPendente("pagar");
            double totalReceber = contaDAO.getTotalPendente("receber");
            
            lblTotalPagar.setText(currencyFormatter.format(totalPagar));
            lblTotalReceber.setText(currencyFormatter.format(totalReceber));
        } catch (SQLException e) {
            lblTotalPagar.setText("Erro");
            lblTotalReceber.setText("Erro");
        }
    }

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

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);
        grid.add(new Label("Data Vencimento:"), 0, 2);
        grid.add(dataVencimentoPicker, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String desc = descField.getText();
                    double valor = Double.parseDouble(valorField.getText().replace(",", "."));
                    LocalDate data = dataVencimentoPicker.getValue();

                    if (desc.isEmpty() || data == null || valor <= 0) {
                        AlertUtil.showError("Erro de Validação", "Todos os campos são obrigatórios e o valor deve ser positivo.");
                        return null;
                    }
                    
                    return new Conta(desc, valor, data.toString(), tipo, "pendente");
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
                contaDAO.addConta(conta);
                carregarDados();
                AlertUtil.showInfo("Sucesso", "Conta adicionada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar a conta: " + e.getMessage());
            }
        });
    }

    /**
     * NOVO: Manipulador para o botão "Editar Lançamento".
     */
    @FXML
    private void handleEditarConta() {
        Conta selecionada = tabelaContas.getSelectionModel().getSelectedItem();
        if (selecionada == null || !selecionada.getStatus().equals("pendente")) {
            AlertUtil.showError("Ação Inválida", "Selecione uma conta 'pendente' para editar.");
            return;
        }
        abrirDialogoEdicao(selecionada);
    }

    /**
     * NOVO: Abre o diálogo para editar uma conta existente.
     */
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

        // Preenche os campos com os dados existentes
        TextField descField = new TextField(conta.getDescricao());
        TextField valorField = new TextField(String.format(Locale.US, "%.2f", conta.getValor()));
        DatePicker dataVencimentoPicker = new DatePicker(LocalDate.parse(conta.getDataVencimento()));
        
        // Permite alterar o tipo
        ComboBox<String> tipoCombo = new ComboBox<>(FXCollections.observableArrayList("pagar", "receber"));
        tipoCombo.setValue(conta.getTipo());

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);
        grid.add(new Label("Data Vencimento:"), 0, 2);
        grid.add(dataVencimentoPicker, 1, 2);
        grid.add(new Label("Tipo:"), 0, 3);
        grid.add(tipoCombo, 1, 3);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == salvarButtonType) {
                try {
                    String desc = descField.getText();
                    double valor = Double.parseDouble(valorField.getText().replace(",", "."));
                    LocalDate data = dataVencimentoPicker.getValue();
                    String tipo = tipoCombo.getValue();

                    if (desc.isEmpty() || data == null || valor <= 0) {
                        AlertUtil.showError("Erro de Validação", "Todos os campos são obrigatórios e o valor deve ser positivo.");
                        return null;
                    }
                    
                    // Atualiza o objeto original
                    conta.setDescricao(desc);
                    conta.setValor(valor);
                    conta.setDataVencimento(data.toString());
                    conta.setTipo(tipo);
                    
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
                contaDAO.updateConta(contaEditada);
                carregarDados();
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

        // Pergunta a data da liquidação (pagamento/recebimento)
        DatePickerDialog dialog = new DatePickerDialog();
        Optional<LocalDate> result = dialog.showAndWait();

        if (result.isPresent()) {
            try {
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
                contaDAO.removerConta(selecionada.getId());
                carregarDados();
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

