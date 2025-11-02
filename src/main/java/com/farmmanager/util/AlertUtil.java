package com.farmmanager.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog; // NOVO IMPORT
import javafx.scene.image.Image; // NOVO IMPORT
import javafx.stage.Stage; // NOVO IMPORT
import java.util.Optional;

/**
 * Classe utilitária para exibir pop-ups (Alerts) padronizados.
 * Melhora a experiência do usuário.
 * ATUALIZADO: Adiciona o ícone da aplicação (logo) a todos os diálogos.
 */
public class AlertUtil {

    // NOVO: Campo estático para armazenar o ícone
    private static Image APP_ICON;

    /**
     * NOVO: Carrega o ícone da aplicação.
     * Deve ser chamado uma vez na inicialização (ex: App.start()).
     */
    public static void loadAppIcon() {
        try {
            APP_ICON = new Image(AlertUtil.class.getResourceAsStream("/com/farmmanager/logo.png"));
        } catch (Exception e) {
            System.err.println("Aviso: Não foi possível carregar o ícone 'logo.png' para os diálogos.");
            APP_ICON = null;
        }
    }

    /**
     * NOVO: Adiciona o ícone da aplicação a qualquer diálogo (Dialog, Alert, etc.).
     */
    public static void setDialogIcon(Dialog<?> dialog) {
        if (APP_ICON != null) {
            try {
                Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
                stage.getIcons().add(APP_ICON);
            } catch (Exception e) {
                 // Ignora se não conseguir (ex: em testes ou se a cena não estiver pronta)
            }
        }
    }


    /**
     * Exibe um pop-up de Erro.
     */
    public static void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        setDialogIcon(alert); // NOVO: Adiciona o ícone
        alert.showAndWait();
    }

    /**
     * Exibe um pop-up de Informação.
     */
    public static void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        setDialogIcon(alert); // NOVO: Adiciona o ícone
        alert.showAndWait();
    }

    /**
     * Exibe um pop-up de Confirmação (Sim/Não).
     *
     * @return true se o usuário clicou em "OK" (Sim), false caso contrário.
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        setDialogIcon(alert); // NOVO: Adiciona o ícone

        Optional<ButtonType> result = alert.showAndWait();
        return (result.isPresent() && result.get() == ButtonType.OK);
    }
}

