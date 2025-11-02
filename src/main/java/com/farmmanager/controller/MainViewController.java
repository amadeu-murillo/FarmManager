package com.farmmanager.controller;

import com.farmmanager.util.AlertUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.Button; // NOVO: Import para Button
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;

/**
 * Controller (Cérebro) para o "Shell" principal (MainView.fxml).
 * ATUALIZADO:
 * - Adicionado handler para a nova tela de Contas (Lançamentos Futuros).
 * - ADICIONADO HANDLER para a nova tela de Histórico de Safras.
 * - ATUALIZADO: loadView agora injeta referência do MainViewController no DashboardController.
 * - ATUALIZADO: Adicionada lógica para gerenciar o estado "ativo" dos botões da barra lateral.
 */
public class MainViewController {

    @FXML
    private StackPane contentArea;

    // --- NOVO: Referências aos botões da barra lateral ---
    @FXML
    private Button btnDashboard;
    @FXML
    private Button btnContas;
    @FXML
    private Button btnFinanceiro;
    @FXML
    private Button btnSafras;
    @FXML
    private Button btnEstoque;
    @FXML
    private Button btnPatrimonio;
    @FXML
    private Button btnFuncionarios;
    @FXML
    private Button btnHistorico;

    // NOVO: Rastreia o botão atualmente ativo
    private Button botaoAtivoAtual;
    private static final String CLASSE_BOTAO_ATIVO = "sidebar-button-selected";

    /**
     * Método especial do JavaFX.
     * É chamado automaticamente depois que o FXML é carregado.
     * Carrega o Dashboard como tela inicial.
     */
    @FXML
    public void initialize() {
        // Carrega o Dashboard como tela inicial e define o botão como ativo
        // Define o btnDashboard como ativo inicial (sem chamar loadView novamente)
        setBotaoAtivo(btnDashboard);
        loadView("DashboardView.fxml");
    }
    
    // --- NOVO: Método para gerenciar o estado visual dos botões ---
    /**
     * Remove a classe de estilo "ativo" do botão anterior e a aplica ao novo.
     * Isso garante que apenas um botão pareça selecionado por vez.
     *
     * @param novoBotaoAtivo O botão que acabou de ser clicado.
     */
    private void setBotaoAtivo(Button novoBotaoAtivo) {
        // 1. Remove o estilo do botão antigo (se houver um)
        if (botaoAtivoAtual != null) {
            botaoAtivoAtual.getStyleClass().remove(CLASSE_BOTAO_ATIVO);
        }

        // 2. Adiciona o estilo ao novo botão
        if (novoBotaoAtivo != null) {
            novoBotaoAtivo.getStyleClass().add(CLASSE_BOTAO_ATIVO);
        }

        // 3. Atualiza a referência
        botaoAtivoAtual = novoBotaoAtivo;
    }


    // --- Handlers dos Botões (Menu) ATUALIZADOS ---
    // Cada handler agora chama setBotaoAtivo() e loadView()

    @FXML
    public void handleShowDashboard() {
        setBotaoAtivo(btnDashboard);
        loadView("DashboardView.fxml");
    }

    /**
     * NOVO: Handler para a tela de Contas (Lançamentos Futuros).
     */
    @FXML
    public void handleShowContas() {
        setBotaoAtivo(btnContas);
        loadView("ContasView.fxml");
    }

    @FXML
    public void handleShowFinanceiro() {
        setBotaoAtivo(btnFinanceiro);
        loadView("FinanceiroView.fxml");
    }

    @FXML
    public void handleShowEstoque() {
        setBotaoAtivo(btnEstoque);
        loadView("EstoqueView.fxml");
    }

    /**
     * NOVO: Handler para a tela de Patrimônio.
     */
    @FXML
    public void handleShowPatrimonio() {
        setBotaoAtivo(btnPatrimonio);
        loadView("PatrimonioView.fxml");
    }

    @FXML
    public void handleShowFuncionarios() {
        setBotaoAtivo(btnFuncionarios);
        loadView("FuncionariosView.fxml");
    }

    @FXML
    public void handleShowSafras() {
        setBotaoAtivo(btnSafras);
        loadView("SafrasView.fxml");
    }

    /**
     * NOVO: Handler para a tela de Histórico de Safras.
     */
    @FXML
    public void handleShowHistoricoSafras() {
        setBotaoAtivo(btnHistorico);
        loadView("HistoricoSafrasView.fxml");
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
            
            // --- ATUALIZAÇÃO: Injeção de Dependência Manual ---
            // Carrega o FXML manualmente para obter o controller
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Node view = loader.load();
            
            // Tenta injetar este MainViewController no controller carregado
            // (Isso é usado para permitir que o Dashboard navegue para outras telas)
            Object controller = loader.getController();
            if (controller instanceof DashboardController) {
                ((DashboardController) controller).setMainViewController(this);
            }
            // --- FIM DA ATUALIZAÇÃO ---
            
            // Limpa o conteúdo antigo e adiciona a nova tela
            contentArea.getChildren().clear();
            contentArea.getChildren().add(view);
            
        } catch (IOException e) {
            e.printStackTrace();
            AlertUtil.showError("Erro ao Carregar Tela", "Não foi possível carregar o módulo: " + fxmlFileName);
        }
    }
}
