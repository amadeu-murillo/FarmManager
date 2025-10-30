package com.farmmanager.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * NOVO: Classe utilitária para lidar com formatação de data e hora.
 */
public class DateTimeUtil {

    // Formatador para o padrão de timestamp (Data e Hora)
    private static final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Retorna o timestamp atual formatado como "yyyy-MM-dd HH:mm:ss".
     */
    public static String getCurrentTimestamp() {
        return LocalDateTime.now().format(dtf);
    }
}
