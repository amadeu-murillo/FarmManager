package com.farmmanager;

import java.io.IOException;
import java.net.ServerSocket;
import javafx.application.Platform; // NOVO: Import para Platform

/**
 * Ponto de entrada principal da aplicação (Launcher).
 * Esta classe é necessária para lançar corretamente a aplicação JavaFX.
 *
 * ATUALIZADO:
 * Implementa um "lock" (usando ServerSocket) para garantir que apenas
 * uma instância da aplicação possa ser executada por vez (Padrão Singleton
 * a nível de aplicação).
 *
 * Se uma segunda instância for aberta, ela será imediatamente fechada.
 * A comunicação para focar a primeira instância é complexa e
 * não foi implementada (requer RMI ou Sockets complexos).
 */
public class Main {
    
    // NOVO: Porta para o "lock".
    // Escolha um número de porta que seja improvável de estar em uso.
    private static final int LOCK_PORT = 19876;
    private static ServerSocket lockSocket;

    public static void main(String[] args) {
        try {
            // 1. Tenta criar um ServerSocket na porta definida.
            // Se for bem-sucedido, esta é a primeira instância da aplicação.
            lockSocket = new ServerSocket(LOCK_PORT);

            // 2. Adiciona um "gancho" (Shutdown Hook) ao Runtime.
            // Isso garante que, quando a aplicação for fechada (mesmo que forçada),
            // o socket seja liberado, permitindo que ela seja aberta novamente.
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    lockSocket.close();
                    System.out.println("Lock do socket liberado.");
                } catch (IOException e) {
                    // Ignora erros ao fechar o socket
                }
            }));

            // 3. Se o lock foi bem-sucedido, continua a execução normal.
            System.out.println("Lock adquirido. Iniciando FarmManager...");
            
            // Inicializa o banco de dados ANTES de lançar a UI
            com.farmmanager.model.Database.initDb();
            
            // Lança a aplicação JavaFX
            App.main(args);

        } catch (IOException e) {
            // 4. Se um IOException ocorrer (ex: "Address already in use"),
            // a porta já está ocupada. Isso significa que outra instância
            // do FarmManager já está rodando.
            
            System.out.println("FarmManager já está em execução. A segunda instância será fechada.");
            
            // Apenas impede a segunda instância de abrir.
            System.exit(0);
        }
    }
}

