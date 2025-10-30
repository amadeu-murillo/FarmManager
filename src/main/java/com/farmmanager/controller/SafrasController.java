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

// Imports adicionados para o cálculo automático
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Locale;

/**
 * Controller para o SafrasView.fxml.
 * ATUALIZAÇÃO 6: Adicionada coluna de Produção Total (sacos).
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
    @FXML
    private TableColumn<SafraInfo, Double> colSafraProdTotalSacos; // NOVO
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

    private final SafraDAO safraDAO;
    private final TalhaoDAO talhaoDAO;
    
    private final ObservableList<SafraInfo> dadosTabelaSafras;
    private final ObservableList<Talhao> dadosTabelaTalhoes;

    public SafrasController() {
        safraDAO = new SafraDAO();
        talhaoDAO = new TalhaoDAO();
        dadosTabelaSafras = FXCollections.observableArrayList();
        dadosTabelaTalhoes = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        // Configura Tabela Safras
        colSafraId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSafraCultura.setCellValueFactory(new PropertyValueFactory<>("cultura"));
        colSafraAno.setCellValueFactory(new PropertyValueFactory<>("anoInicio"));
        colSafraTalhao.setCellValueFactory(new PropertyValueFactory<>("talhaoNome"));
        colSafraProd.setCellValueFactory(new PropertyValueFactory<>("producaoSacosPorHectare"));
        colSafraProdTotalSacos.setCellValueFactory(new PropertyValueFactory<>("producaoTotalSacos")); // NOVO
        colSafraProdTotalKg.setCellValueFactory(new PropertyValueFactory<>("producaoTotalKg"));
        tabelaSafras.setItems(dadosTabelaSafras);
        
        // Configura Tabela Talhões
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

    private void carregarDadosTalhoes() {
         try {
            dadosTabelaTalhoes.clear();
            dadosTabelaTalhoes.addAll(talhaoDAO.listTalhoes());
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar os talhões.");
        }
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
                    double area = Double.parseDouble(areaField.getText().replace(",", ".")); // Aceita vírgula ou ponto
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

    /**
     * ATUALIZADO: Manipulador para "Registrar Colheita" com cálculo automático.
     * Usa um Dialog customizado para melhor UX.
     */
    @FXML
    private void handleRegistrarColheita() {
        SafraInfo safraSelecionada = tabelaSafras.getSelectionModel().getSelectedItem();

        if (safraSelecionada == null) {
            AlertUtil.showError("Nenhuma Seleção", "Por favor, selecione uma safra na tabela para registrar a colheita.");
            return;
        }
        
        // 1. Criar o diálogo customizado
        Dialog<Double> dialog = new Dialog<>();
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
        
        // Campo de entrada
        TextField scHaField = new TextField(String.format(Locale.US, "%.2f", prodAtualScHa));
        
        // Adiciona componentes ao grid
        grid.add(new Label("Área do Talhão:"), 0, 0);
        grid.add(areaLabel, 1, 0);
        
        grid.add(new Label("Produção (sc/ha):"), 0, 1);
        grid.add(scHaField, 1, 1);
        
        grid.add(new Label("Total (sacos):"), 0, 2);
        grid.add(totalSacosLabel, 1, 2);
        
        grid.add(new Label("Total (kg):"), 0, 3);
        grid.add(totalKgLabel, 1, 3);
        
        // 3. Adicionar listener para cálculo automático
        scHaField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
                // Chama o método helper
                atualizarCalculoColheita(newValue, totalSacosLabel, totalKgLabel, area);
            }
        });
        
        // CORREÇÃO: Chama o método helper diretamente com o valor inicial
        atualizarCalculoColheita(scHaField.getText(), totalSacosLabel, totalKgLabel, area);

        dialog.getDialogPane().setContent(grid);

        // 4. Converter o resultado (queremos salvar o total em KG)
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == registrarButtonType) {
                try {
                    double producaoScHa = Double.parseDouble(scHaField.getText().replace(",", "."));
                    if (producaoScHa < 0) {
                        AlertUtil.showError("Valor Inválido", "A produção não pode ser negativa.");
                        return null;
                    }
                    // Retorna o total em KG para ser salvo
                    return (producaoScHa * area) * 60.0; 
                } catch (NumberFormatException e) {
                    AlertUtil.showError("Erro de Formato", "Valor de produção inválido. Use apenas números (ex: 65.5).");
                    return null;
                }
            }
            return null;
        });

        // 5. Exibir o diálogo e processar o resultado
        Optional<Double> result = dialog.showAndWait();

        result.ifPresent(producaoKg -> {
            try {
                // Salva o total em KG no banco
                safraDAO.updateProducaoSafra(safraSelecionada.getId(), producaoKg);
                carregarDadosSafras(); // Atualiza a tabela para refletir a nova produtividade
                AlertUtil.showInfo("Sucesso", "Colheita registrada com sucesso.");

            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível atualizar a produção: " + e.getMessage());
            }
        });
    }


    /**
     * NOVO: Manipulador para o botão "- Remover Safra".
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
                carregarDadosSafras();
                AlertUtil.showInfo("Sucesso", "Safra removida com sucesso.");
            } catch (SQLException e) {
                AlertUtil.showError("Erro de Banco de Dados", "Não foi possível remover a safra: " + e.getMessage());
            }
        }
    }

    /**
     * NOVO: Manipulador para o botão "- Remover Talhão".
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
     * NOVO: Método helper para atualizar os labels de cálculo da colheita.
     */
    private void atualizarCalculoColheita(String newValue, Label totalSacosLabel, Label totalKgLabel, double area) {
        try {
            double producaoScHa = Double.parseDouble(newValue.replace(",", "."));
            if (producaoScHa < 0) {
                totalSacosLabel.setText("Inválido");
                totalKgLabel.setText("Inválido");
                return;
            }
            
            // Cálculo (Assumindo 60kg/saco, conforme SafraInfo)
            double totalSacos = producaoScHa * area;
            double producaoKg = totalSacos * 60.0; 
            
            totalSacosLabel.setText(String.format(Locale.US, "%.2f sacos", totalSacos));
            totalKgLabel.setText(String.format(Locale.US, "%.2f kg", producaoKg));
            
        } catch (NumberFormatException e) {
            totalSacosLabel.setText("---");
            totalKgLabel.setText("---");
        }
    }
}

