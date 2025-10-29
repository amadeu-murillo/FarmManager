package com.farmmanager.controller;

import com.farmmanager.model.SafraInfo;
import com.farmmanager.model.SafraDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.SQLException;

/**
 * Controller para o SafrasView.fxml.
 */
public class SafrasController {

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

    private final SafraDAO safraDAO;
    private final ObservableList<SafraInfo> dadosTabela;

    public SafrasController() {
        safraDAO = new SafraDAO();
        dadosTabela = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        // Os nomes ("id", "cultura", etc.) vêm da classe SafraInfo.java
        colSafraId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colSafraCultura.setCellValueFactory(new PropertyValueFactory<>("cultura"));
        colSafraAno.setCellValueFactory(new PropertyValueFactory<>("anoInicio"));
        colSafraTalhao.setCellValueFactory(new PropertyValueFactory<>("talhaoNome"));
        colSafraProd.setCellValueFactory(new PropertyValueFactory<>("producaoTotalKg"));

        tabelaSafras.setItems(dadosTabela);
        carregarDadosDaTabela();
    }

    private void carregarDadosDaTabela() {
        try {
            dadosTabela.clear();
            dadosTabela.addAll(safraDAO.listSafrasComInfo());
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar as safras.");
        }
    }
}
