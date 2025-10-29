package com.farmmanager.controller;

import com.farmmanager.model.Safra;
import com.farmmanager.model.SafraInfo;
import com.farmmanager.model.SafraDAO;
import com.farmmanager.model.Talhao;
import com.farmmanager.model.TalhaoDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Controller para o SafrasView.fxml.
 * ATUALIZADO: Implementados handlers de adição e carregamento da tabela de Talhões.
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
    private TableColumn<SafraInfo, Integer> colSafraAno;
    @FXML
    private TableColumn<SafraInfo, String> colSafraTalhao;
    @FXML
    private TableColumn<SafraInfo, Double> colSafraProd;

    // NOVO: Tabela Talhões
    @FXML
    private TableView<Talhao> tabelaTalhoes;
    @FXML
    private TableColumn<Talhao, Integer> colTalhaoId;
    @FXML
    private TableColumn<Talhao, String> colTalhaoNome;
    @FXML
    private TableColumn<Talhao, Double> colTalhaoArea;

    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO; // NOVO
    
    private final ObservableList<SafraInfo> dadosTabelaSafras;
    private final ObservableList<Talhao> dadosTabelaTalhoes; // NOVO

    public SafrasController() {
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO(); // NOVO
        dadosTabelaSafras = FXCollections.observableArrayList();
        dadosTabelaTalhoes = FXCollections.observableArrayList(); // NOVO
    }

    @FXML
    public void initialize() {
        // Configura Tabela Safras
        colSafraId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSafraCultura.setCellValueFactory(new PropertyValueFactory<>("cultura"));
        colSafraAno.setCellValueFactory(new PropertyValueFactory<>("anoInicio"));
        colSafraTalhao.setCellValueFactory(new PropertyValueFactory<>("talhaoNome"));
        colSafraProd.setCellValueFactory(new PropertyValueFactory<>("producaoTotalKg"));
        tabelaSafras.setItems(dadosTabelaSafras);
        
        // NOVO: Configura Tabela Talhões
        colTalhaoId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colTalhaoNome.setCellValueFactory(new PropertyValueFactory<>("nome"));
        colTalhaoArea.setCellValueFactory(new PropertyValueFactory<>("areaHectares"));
        tabelaTalhoes.setItems(dadosTabelaTalhoes);

        // Carrega dados das duas tabelas
        carregarDadosSafras();
        carregarDadosTalhoes();
    }

    private void carregarDadosSafras() {
        try {
            dadosTabelaSafras.clear();
            dadosTabelaSafras.addAll(safraDAO.listSafrasComInfo());
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar as safras.");
        }
    }

    // NOVO
    private void carregarDadosTalhoes() {
         try {
            dadosTabelaTalhoes.clear();
            dadosTabelaTalhoes.addAll(talhaoDAO.listTalhoes());
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os talhões.");
        }
    }

    /**
     * NOVO: Manipulador para o botão "+ Novo Talhão".
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
                    double area = Double.parseDouble(areaField.getText());
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
     * NOVO: Manipulador para o botão "+ Nova Safra".
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
        TextField anoField = new TextField();
        anoField.setPromptText("Ex: 2024");
        ComboBox<Talhao> talhaoCombo = new ComboBox<>(FXCollections.observableArrayList(talhoes));
        
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
        grid.add(new Label("Ano Início:"), 0, 1);
        grid.add(anoField, 1, 1);
        grid.add(new Label("Talhão:"), 0, 2);
        grid.add(talhaoCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == adicionarButtonType) {
                try {
                    String cultura = culturaField.getText();
                    int ano = Integer.parseInt(anoField.getText());
                    Talhao talhaoSel = talhaoCombo.getSelectionModel().getSelectedItem();
                    
                    if (cultura.isEmpty() || talhaoSel == null) {
                        AlertUtil.showError("Erro de Validação", "Cultura e Talhão são obrigatórios.");
                        return null;
                    }
                    return new Safra(cultura, ano, talhaoSel.getId());
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor de ano inválido.");
                    return null;
                }
            }
            return null;
        });

        Optional<Safra> result = dialog.showAndWait();

        result.ifPresent(safra -> {
            try {
                safraDAO.addSafra(safra);
                carregarDadosSafras(); // Atualiza a tabela de safras
                AlertUtil.showInfo("Sucesso", "Safra adicionada com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível adicionar a safra: " + e.getMessage());
            }
        });
    }
}
