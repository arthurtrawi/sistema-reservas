package util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Formato padrão de data e hora do sistema (dd/MM/yyyy HH:mm).
 *
 * Usado nas mensagens do back-end (ex.: conflito de horário, período
 * inválido) e reaproveitado pela interface, garantindo que as datas
 * apareçam sempre do mesmo jeito para o usuário.
 */
public final class FormatoDataHora {

    public static final DateTimeFormatter PADRAO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private FormatoDataHora() {
        // Não instanciável.
    }

    /** "2026-11-25T10:00" -> "25/11/2026 10:00"; null vira texto vazio. */
    public static String formatar(LocalDateTime valor) {
        return valor == null ? "" : valor.format(PADRAO);
    }
}
