package com.farmmanager.controller;

import com.farmmanager.model.Funcionario;
import com.farmmanager.model.FuncionarioDAO;
import com.farmmanager.model.FinanceiroDAO; // NOVO
import com.farmmanager.model.Transacao; // NOVO
import com.farmmanager.util.AlertUtil;
import javafx.beans.property.SimpleObjectProperty; // NOVO
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos; // NOVO
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.text.NumberFormat; // NOVO
import java.time.LocalDate; // NOVO
import java.util.List;
import java.util.Locale; // NOVO
import java.util.Optional;

/**
 * Controller para o FuncionariosView.fxml.
 * ATUALIZADO:
 * - Substituído o fluxo de múltiplos TextInputDialogs por um único Dialog customizado
 * - Implementada a remoção real.
 * - Adicionada "Data de Início" ao cadastro.
 * - Adicionada integração com Financeiro para "Lançar Pagamento".
 * - ATUALIZADO: Adicionados campos cpf, telefone, endereco.
 * - ATUALIZADO: Adicionado "handleLancarOutroPagamento" para pagamentos customizados.
 * - ATUALIZADO: Adicionado painel de detalhes (SplitPane) com histórico de pagamentos.
 */
public class FuncionariosController {

    @FXML
    private TableView<Funcionario> tabelaFuncionarios;
    @FXML
    private TableColumn<Funcionario, Integer> colFuncId;
    @FXML
    private TableColumn<Funcionario, String> colFuncNome;
    @FXML
    private TableColumn<Funcionario, String> colFuncCargo;
    @FXML
    private TableColumn<Funcionario, Double> colFuncSalario;
    @FXML
    private TableColumn<Funcionario, String> colFuncDataInicio;
    @FXML
    private TableColumn<Funcionario, String> colFuncCpf; // NOVO
    @FXML
    private TableColumn<Funcionario, String> colFuncTelefone; // NOVO
    @FXML
    private TableColumn<Funcionario, String> colFuncEndereco; // NOVO

    @FXML
    private Button btnPagarSalario; // NOVO
    @FXML
    private Button btnLancarOutroPagamento; // NOVO
    
    // --- NOVO: Painel de Detalhes ---
    @FXML
    private SplitPane splitPane; // NOVO
    @FXML
    private Label lblDetalhesTitulo; // NOVO
    @FXML
    private TableView<Transacao> tabelaPagamentos; // NOVO
    @FXML
    private TableColumn<Transacao, String> colPagData; // NOVO
    @FXML
    private TableColumn<Transacao, String> colPagDesc; // NOVO
    @FXML
    private TableColumn<Transacao, Double> colPagSaida; // NOVO
    @FXML
    private Label lblTotalPagamentos; // NOVO
    // --- Fim do Painel de Detalhes ---

    private final FuncionarioDAO funcionarioDAO;
    private final FinanceiroDAO financeiroDAO; // NOVO
    private final ObservableList<Funcionario> dadosTabela;
    private final ObservableList<Transacao> dadosTabelaPagamentos; // NOVO
    private final NumberFormat currencyFormatter; // NOVO

    public FuncionariosController() {
        funcionarioDAO = new FuncionarioDAO();
        financeiroDAO = new FinanceiroDAO(); // NOVO
        dadosTabela = FXCollections.observableArrayList();
        dadosTabelaPagamentos = FXCollections.observableArrayList(); // NOVO
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")); // NOVO
    }

    @FXML
    public void initialize() {
        // Configura as colunas da tabela
        colFuncId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFuncNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colFuncCargo.setCellValueFactory(new PropertyValueFactory<>("cargo"));
        colFuncSalario.setCellValueFactory(new PropertyValueFactory<>("salario"));
        colFuncDataInicio.setCellValueFactory(new PropertyValueFactory<>("dataInicio"));
        colFuncCpf.setCellValueFactory(new PropertyValueFactory<>("cpf")); // NOVO
        colFuncTelefone.setCellValueFactory(new PropertyValueFactory<>("telefone")); // NOVO
        colFuncEndereco.setCellValueFactory(new PropertyValueFactory<>("endereco")); // NOVO
        
        // Associa a lista observável à tabela
        tabelaFuncionarios.setItems(dadosTabela);

        // NOVO: Desabilita botões se nada estiver selecionado
        btnPagarSalario.disableProperty().bind(tabelaFuncionarios.getSelectionModel().selectedItemProperty().isNull());
        btnLancarOutroPagamento.disableProperty().bind(tabelaFuncionarios.getSelectionModel().selectedItemProperty().isNull()); // NOVO

        // --- NOVO: Configura painel de detalhes ---
        colPagData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colPagDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        
        // Coluna de Saída (customizada para mostrar valor absoluto e cor)
        colPagSaida.setCellValueFactory(cellData -> {
            Transacao t = cellData.getValue();
            // Retorna o valor (absoluto)
            return new SimpleObjectProperty<>(Math.abs(t.getValor()));
        });
        colPagSaida.setCellFactory(col -> new TableCell<Transacao, Double>() {
            @Override
            protected void updateItem(Double item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item == 0) {
                    setText(null);
                    getStyleClass().removeAll("positivo-text", "negativo-text");
                } else {
                    setText(currencyFormatter.format(item));
                    setAlignment(Pos.CENTER_RIGHT);
                    getStyleClass().add("negativo-text"); // Pagamentos são sempre despesas
                }
            }
        });
        
        tabelaPagamentos.setItems(dadosTabelaPagamentos);

        // Listener de seleção para atualizar painel de detalhes
        tabelaFuncionarios.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> handleFuncionarioSelectionChanged(newSelection)
        );
        // --- Fim da configuração de detalhes ---

        // Carrega os dados
        limparDetalhes(); // NOVO
        carregarDadosDaTabela();
    }

    /**
     * Busca os dados do DAO e atualiza a ObservableList.
     */
    private void carregarDadosDaTabela() {
        try {
            List<Funcionario> lista = funcionarioDAO.listFuncionarios();
            dadosTabela.clear();
            dadosTabela.addAll(lista);
            limparDetalhes(); // Limpa o painel direito ao recarregar a tabela
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os funcionários.");
        }
    }

    /**
     * NOVO: Limpa o painel de detalhes quando nada está selecionado.
     */
    private void limparDetalhes() {
        lblDetalhesTitulo.setText("Detalhes: (Selecione um funcionário)");
        dadosTabelaPagamentos.clear();
        lblTotalPagamentos.setText("Total Pago: R$ 0,00");
    }

    /**
     * NOVO: Carrega os detalhes do funcionário selecionado no painel da direita.
     */
    private void handleFuncionarioSelectionChanged(Funcionario selecionado) {
        if (selecionado == null) {
            limparDetalhes();
            return;
        }

        lblDetalhesTitulo.setText("Histórico de Pagamentos: " + selecionado.getNome());

        try {
            // Busca no financeiro por transações que contenham o nome do funcionário
            List<Transacao> pagamentos = financeiroDAO.listTransacoesPorDescricaoContendo(selecionado.getNome());
            
            dadosTabelaPagamentos.clear();
            dadosTabelaPagamentos.addAll(pagamentos);

            // Calcula o custo total
            double totalPago = 0.0;
            for (Transacao t : pagamentos) {
                totalPago += t.getValor(); // Valores já são negativos
            }
            
            // Exibe o valor absoluto formatado
            lblTotalPagamentos.setText("Total Pago: " + currencyFormatter.format(Math.abs(totalPago)));

        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar o histórico de pagamentos: " + e.getMessage());
            limparDetalhes(); // Limpa em caso de erro
        }
    }

    /**
     * ATUALIZADO: Adicionado DatePicker para data de início e novos campos.
     */
    @FXML
    private void handleAdicionar() {
        // 1. Criar o diálogo customizado
        Dialog<Funcionario> dialog = new Dialog<>();
        dialog.setTitle("Adicionar Novo Funcionário");
        dialog.setHeaderText("Preencha os dados do novo funcionário.");

        // 2. Definir os botões
        ButtonType adicionarButtonType = new ButtonType("Adicionar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(adicionarButtonType, ButtonType.CANCEL);

        // 3. Criar o layout (GridPane)
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField nomeField = new TextField();
        nomeField.setPromptText("Nome");
        TextField cargoField = new TextField();
        cargoField.setPromptText("Cargo");
        TextField salarioField = new TextField();
        salarioField.setPromptText("Salário (ex: 2500.50)");
        DatePicker dataInicioPicker = new DatePicker(LocalDate.now());
        TextField cpfField = new TextField(); // NOVO
        cpfField.setPromptText("CPF (somente números)"); // NOVO
        TextField telefoneField = new TextField(); // NOVO
        telefoneField.setPromptText("Telefone (ex: (45) 99999-8888)"); // NOVO
        TextField enderecoField = new TextField(); // NOVO
        enderecoField.setPromptText("Endereço (ex: Rua Principal, 123)"); // NOVO

        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nomeField, 1, 0);
        grid.add(new Label("Cargo:"), 0, 1);
        grid.add(cargoField, 1, 1);
        grid.add(new Label("Salário (R$):"), 0, 2);
        grid.add(salarioField, 1, 2);
        grid.add(new Label("Data de Início:"), 0, 3);
        grid.add(dataInicioPicker, 1, 3);
        grid.add(new Label("CPF:"), 0, 4); // NOVO
        grid.add(cpfField, 1, 4); // NOVO
        grid.add(new Label("Telefone:"), 0, 5); // NOVO
        grid.add(telefoneField, 1, 5); // NOVO
        grid.add(new Label("Endereço:"), 0, 6); // NOVO
        grid.add(enderecoField, 1, 6); // NOVO

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        // 4. Converter o resultado para um objeto Funcionario
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String nome = nomeField.getText();
                    String cargo = cargoField.getText();
                    double salario = Double.parseDouble(salarioField.getText().replace(",", "."));
                    LocalDate dataInicio = dataInicioPicker.getValue();
                    String cpf = cpfField.getText(); // NOVO
                    String telefone = telefoneField.getText(); // NOVO
                    String endereco = enderecoField.getText(); // NOVO
                    
                    if (nome.isEmpty() || cargo.isEmpty() || cpf.isEmpty()) { // NOVO: CPF obrigatório
                        AlertUtil.showError("Erro de Validação", "Nome, Cargo e CPF são obrigatórios.");
                        return null; // Retorna nulo para não fechar o diálogo
                    }
                    if (dataInicio == null) {
                        AlertUtil.showError("Erro de Validação", "Data de início é obrigatória.");
                        return null;
                    }
                    
                    // ATUALIZADO: Novo construtor
                    return new Funcionario(nome, cargo, salario, dataInicio.toString(), cpf, telefone, endereco);
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor do salário inválido.");
                    return null; // Retorna nulo para não fechar o diálogo
                }
            }
            return null;
        });

        // 5. Exibir o diálogo e processar o resultado
        Optional<Funcionario> result = dialog.showAndWait();

        result.ifPresent(funcionario -> {
            try {
                funcionarioDAO.addFuncionario(funcionario);
                carregarDadosDaTabela(); // Atualiza a tabela
                AlertUtil.showInfo("Sucesso", "Funcionário adicionado com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar o funcionário: " + e.getMessage());
            }
        });
    }


    @FXML
    private void handleRemover() {
        Funcionario selecionado = tabelaFuncionarios.getSelectionModel().getSelectedItem();
        
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um funcionário na tabela para remover.");
            return;
        }

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Remoção", 
            "Tem certeza que deseja remover o funcionário '" + selecionado.getNome() + "'?");

        if (confirmado) {
            try {
                // ATUALIZADO: Chama o DAO real para remover
                if (funcionarioDAO.removerFuncionario(selecionado.getId())) {
                    AlertUtil.showInfo("Removido", "Funcionário removido com sucesso.");
                    carregarDadosDaTabela(); // Recarrega os dados do banco (e limpa os detalhes)
                } else {
                    AlertUtil.showError("Erro ao Remover", "O funcionário não pôde ser removido.");
                }
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover o funcionário: " + e.getMessage());
            } catch (Exception e) {
                AlertUtil.showError("Erro ao Remover", "Não foi possível remover o funcionário.");
            }
        }
    }

    /**
     * NOVO: Lança o pagamento do salário Padrão do funcionário selecionado no financeiro.
     */
    @FXML
    private void handlePagarSalarioPadrao() { // RENOMEADO
        Funcionario selecionado = tabelaFuncionarios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            // Este alerta é redundante se o botão estiver desabilitado, mas é uma boa prática.
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um funcionário para lançar o pagamento.");
            return;
        }

        String dataHoje = LocalDate.now().toString();
        String desc = "Pagamento de salário: " + selecionado.getNome();
        double valor = -selecionado.getSalario(); // Despesa é negativa

        boolean confirmado = AlertUtil.showConfirmation("Confirmar Pagamento", 
            "Deseja lançar o pagamento de " + currencyFormatter.format(selecionado.getSalario()) + 
            " para " + selecionado.getNome() + " com data de hoje (" + dataHoje + ")?");

        if (confirmado) {
            try {
                Transacao transacao = new Transacao(desc, valor, dataHoje, "despesa");
                financeiroDAO.addTransacao(transacao);
                AlertUtil.showInfo("Sucesso", "Pagamento lançado no financeiro com sucesso.");
                // Atualiza o painel de detalhes se o funcionário ainda estiver selecionado
                if(selecionado.equals(tabelaFuncionarios.getSelectionModel().getSelectedItem())) {
                    handleFuncionarioSelectionChanged(selecionado);
                }
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível lançar o pagamento: " + e.getMessage());
            }
        }
    }

    /**
     * NOVO: Lança um pagamento customizado (adiantamento, bônus, etc.) para o funcionário.
     */
    @FXML
    private void handleLancarOutroPagamento() {
        Funcionario selecionado = tabelaFuncionarios.getSelectionModel().getSelectedItem();
        if (selecionado == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione um funcionário para lançar um pagamento.");
            return;
        }

        // 1. Criar o diálogo customizado
        Dialog<Transacao> dialog = new Dialog<>();
        dialog.setTitle("Lançar Outro Pagamento");
        dialog.setHeaderText("Lançar despesa para: " + selecionado.getNome());

        // 2. Definir os botões
        ButtonType lancarButtonType = new ButtonType("Lançar", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(lancarButtonType, ButtonType.CANCEL);

        // 3. Criar o layout (GridPane)
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField descField = new TextField();
        descField.setPromptText("Ex: Adiantamento de salário");
        TextField valorField = new TextField();
        valorField.setPromptText("Ex: 500.00");
        DatePicker dataPicker = new DatePicker(LocalDate.now());

        grid.add(new Label("Descrição:"), 0, 0);
        grid.add(descField, 1, 0);
        grid.add(new Label("Valor (R$):"), 0, 1);
        grid.add(valorField, 1, 1);
        grid.add(new Label("Data do Pagamento:"), 0, 2);
        grid.add(dataPicker, 1, 2);

        dialog.getDialogPane().setContent(grid);
        AlertUtil.setDialogIcon(dialog); // NOVO: Adiciona o ícone

        // 4. Converter o resultado para um objeto Transacao
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == lancarButtonType) {
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

                    // Prefixa a descrição para clareza e armazena como despesa (negativo)
                    String descFinal = "Pgto. Funcionário (" + selecionado.getNome() + "): " + desc;
                    double valorFinal = -valor; // Despesa é negativa

                    return new Transacao(descFinal, valorFinal, data.toString(), "despesa");
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor inválido.");
                    return null;
                }
            }
            return null;
        });

        // 5. Exibir o diálogo e processar o resultado
        Optional<Transacao> result = dialog.showAndWait();

        result.ifPresent(transacao -> {
            try {
                financeiroDAO.addTransacao(transacao);
                AlertUtil.showInfo("Sucesso", "Pagamento lançado no financeiro com sucesso.");
                // Atualiza o painel de detalhes se o funcionário ainda estiver selecionado
                if(selecionado.equals(tabelaFuncionarios.getSelectionModel().getSelectedItem())) {
                    handleFuncionarioSelectionChanged(selecionado);
                }
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível lançar o pagamento: " + e.getMessage());
            }
        });
    }
}

