package com.farmmanager.controller;

import com.farmmanager.model.EstoqueItem;
import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.SQLException;

/**
 * Controller para o EstoqueView.fxml.
 */
public class EstoqueController {

    @FXML
    private TableView<EstoqueItem> tabelaEstoque;
    @FXML
    private TableColumn<EstoqueItem, Integer> colItemId;
    @FXML
    private TableColumn<EstoqueItem, String> colItemNome;
    @FXML
    private TableColumn<EstoqueItem, Double> colItemQtd;
    @FXML
    private TableColumn<EstoqueItem, String> colItemUnidade;

    private final EstoqueDAO estoqueDAO;
    private final ObservableList<EstoqueItem> dadosTabela;

    public EstoqueController() {
        estoqueDAO = new EstoqueDAO();
        dadosTabela = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        colItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colItemNome.setCellValueFactory(new PropertyValueFactory<>("itemNome"));
        colItemQtd.setCellValueFactory(new PropertyValueFactory<>("quantidade"));
        colItemUnidade.setCellValueFactory(new PropertyValueFactory<>("unidade"));

        tabelaEstoque.setItems(dadosTabela);
        carregarDadosDaTabela();
    }

    private void carregarDadosDaTabela() {
        try {
            dadosTabela.clear();
            dadosTabela.addAll(estoqueDAO.listEstoque());
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar o estoque.");
        }
    }
}
