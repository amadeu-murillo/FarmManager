package com.farmmanager.controller;

import com.farmmanager.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * Controller (Cérebro) para o "Shell" principal (MainView.fxml).
 * ATUALIZADO:
 * - Adicionado handler para a nova tela de Contas (Lançamentos Futuros).
 */
public class MainViewController {

    @FXML
    private StackPane contentArea;

    /**
     * Método especial do JavaFX.
     * É chamado automaticamente depois que o FXML é carregado.
     * Carrega o Dashboard como tela inicial.
     */
    @FXML
    public void initialize() {
        // Carrega o Dashboard como tela inicial
        handleShowDashboard();
    }

    // --- Handlers dos Botões (Menu) ---
    // Cada handler agora chama o método loadView()

    @FXML
    private void handleShowDashboard() {
        loadView("DashboardView.fxml");
    }

    /**
     * NOVO: Handler para a tela de Contas (Lançamentos Futuros).
     */
    @FXML
    private void handleShowContas() {
        loadView("ContasView.fxml");
    }

    @FXML
    private void handleShowFinanceiro() {
        loadView("FinanceiroView.fxml");
    }

    @FXML
    private void handleShowEstoque() {
        loadView("EstoqueView.fxml");
    }

    /**
     * NOVO: Handler para a tela de Patrimônio.
     */
    @FXML
    private void handleShowPatrimonio() {
        loadView("PatrimonioView.fxml");
    }

    @FXML
    private void handleShowFuncionarios() {
        loadView("FuncionariosView.fxml");
    }

    @FXML
    private void handleShowSafras() {
        loadView("SafrasView.fxml");
    }

    /**
     * Helper: Carrega um arquivo FXML na área de conteúdo central (StackPane).
     *
     * @param fxmlFileName O nome do arquivo FXML (ex: "DashboardView.fxml")
     */
    private void loadView(String fxmlFileName) {
        try {
            // Constrói o caminho para o FXML
            URL fxmlUrl = getClass().getResource("/com/farmmanager/" + fxmlFileName);
            if (fxmlUrl == null) {
                throw new IOException("Não foi possível encontrar o FXML: " + fxmlFileName);
            }
            
            // Carrega o FXML
            Node view = FXMLLoader.load(fxmlUrl);
            
            // Limpa o conteúdo antigo e adiciona a nova tela
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Erro ao Carregar Tela", "Não foi possível carregar o módulo: " + fxmlFileName);
        }
    }
}
