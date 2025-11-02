package com.farmmanager;

/**
 * Ponto de entrada principal da aplicação (Launcher).
 * Esta classe é necessária para lançar corretamente a aplicação JavaFX.
 */
public class Main {
    public static void main(String[] args) {
        // Inicializa o banco de dados ANTES de lançar a UI
        com.farmmanager.model.Database.initDb();
        
        // Lança a aplicação JavaFX
        App.main(args);
    }
}
