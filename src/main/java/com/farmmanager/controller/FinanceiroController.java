package com.farmmanager.controller;

import com.farmmanager.model.Transacao;
import com.farmmanager.model.FinanceiroDAO;
import com.farmmanager.util.AlertUtil;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import java.sql.SQLException;

/**
 * Controller para o FinanceiroView.fxml.
 */
public class FinanceiroController {

    @FXML
    private TableView<Transacao> tabelaFinanceiro;
    @FXML
    private TableColumn<Transacao, Integer> colFinId;
    @FXML
    private TableColumn<Transacao, String> colFinDesc;
    @FXML
    private TableColumn<Transacao, Double> colFinValor;
    @FXML
    private TableColumn<Transacao, String> colFinData;
    @FXML
    private TableColumn<Transacao, String> colFinTipo;

    private final FinanceiroDAO financeiroDAO;
    private final ObservableList<Transacao> dadosTabela;

    public FinanceiroController() {
        financeiroDAO = new FinanceiroDAO();
        dadosTabela = FXCollections.observableArrayList();
    }

    @FXML
    public void initialize() {
        colFinId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colFinDesc.setCellValueFactory(new PropertyValueFactory<>("descricao"));
        colFinValor.setCellValueFactory(new PropertyValueFactory<>("valor"));
        colFinData.setCellValueFactory(new PropertyValueFactory<>("data"));
        colFinTipo.setCellValueFactory(new PropertyValueFactory<>("tipo"));
        
        tabelaFinanceiro.setItems(dadosTabela);
        carregarDadosDaTabela();
    }

    private void carregarDadosDaTabela() {
        try {
            dadosTabela.clear();
            dadosTabela.addAll(financeiroDAO.listTransacoes());
        } catch (SQLException e) {
            AlertUtil.showError("Erro de Banco de Dados", "Não foi possível carregar as transações.");
        }
    }
}
