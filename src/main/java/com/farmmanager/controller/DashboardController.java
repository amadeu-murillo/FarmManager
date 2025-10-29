package com.farmmanager.controller;

import com.farmmanager.model.EstoqueDAO;
import com.farmmanager.model.FinanceiroDAO;
import com.farmmanager.model.FuncionarioDAO;
import com.farmmanager.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.sql.SQLException;
import java.text.NumberFormat;
import java.util.Locale;

/**
 * Controller para o DashboardView.fxml.
 * Este controller "comunica" dados de múltiplos DAOs.
 */
public class DashboardController {

    @FXML
    private Label lblBalanco;
    @FXML
    private Label lblFuncionarios;
    @FXML
    private Label lblEstoque;

    // DAOs necessários para o resumo
    private final FinanceiroDAO financeiroDAO;
    private final FuncionarioDAO funcionarioDAO;
    private final EstoqueDAO estoqueDAO;

    // Formatador para Reais (R$)
    private final NumberFormat currencyFormatter;

    public DashboardController() {
        // Instancia os DAOs
        financeiroDAO = new FinanceiroDAO();
        funcionarioDAO = new FuncionarioDAO();
        estoqueDAO = new EstoqueDAO();
        
        // Configura o formatador de moeda
        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
    }

    /**
     * Chamado automaticamente ao carregar o FXML.
     * Busca os dados dos DAOs e preenche os cards.
     */
    @FXML
    public void initialize() {
        carregarBalanco();
        carregarTotalFuncionarios();
        carregarTotalEstoque();
    }

    private void carregarBalanco() {
        try {
            double balanco = financeiroDAO.getBalançoFinanceiro();
            String balancoFormatado = currencyFormatter.format(balanco);
            
            lblBalanco.setText(balancoFormatado);

            // Adiciona classe de estilo (CSS) para cor
            lblBalanco.getStyleClass().removeAll("positivo", "negativo");
            if (balanco >= 0) {
                lblBalanco.getStyleClass().add("positivo");
            } else {
                lblBalanco.getStyleClass().add("negativo");
            }
        } catch (SQLException e) {
            lblBalanco.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar balanço financeiro.");
        }
    }

    private void carregarTotalFuncionarios() {
        try {
            int total = funcionarioDAO.getContagemFuncionarios();
            lblFuncionarios.setText(String.valueOf(total));
        } catch (SQLException e) {
            lblFuncionarios.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar total de funcionários.");
        }
    }

    private void carregarTotalEstoque() {
        try {
            int total = estoqueDAO.getContagemItensDistintos();
            lblEstoque.setText(String.valueOf(total));
        } catch (SQLException e) {
            lblEstoque.setText("Erro");
            AlertUtil.showError("Dashboard", "Erro ao carregar total de itens em estoque.");
        }
    }
}
