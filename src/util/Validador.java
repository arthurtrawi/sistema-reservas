package util;

import exception.ReservaInvalidaException;

import java.time.LocalDateTime;
import java.util.regex.Pattern;

/**
 * Métodos utilitários de validação reutilizados pelas camadas model e service.
 *
 * Convenção de erros:
 *  - Dado de campo inválido (vazio, nulo, formato errado) -> IllegalArgumentException
 *    (exceção nativa do Java, idiomática para argumentos inválidos).
 *  - Período de reserva inválido -> ReservaInvalidaException (regra de negócio).
 *
 * A classe é final e tem construtor privado: é apenas um agrupador de
 * métodos estáticos e não faz sentido instanciá-la nem estendê-la.
 */
public final class Validador {

    /** Formato simples de e-mail: algo@algo.algo, sem espaços. */
    private static final Pattern PADRAO_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private static final int TAMANHO_CPF = 11;

    private Validador() {
        // Não instanciável.
    }

    /**
     * Garante que o texto não é nulo nem vazio/só espaços.
     *
     * @return o texto sem espaços nas extremidades
     */
    public static String exigirTexto(String valor, String nomeCampo) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("O campo '" + nomeCampo + "' é obrigatório.");
        }
        return valor.trim();
    }

    /**
     * Garante que o objeto não é nulo.
     *
     * @return o próprio objeto, permitindo uso em atribuições
     */
    public static <T> T exigirNaoNulo(T valor, String nomeCampo) {
        if (valor == null) {
            throw new IllegalArgumentException("O campo '" + nomeCampo + "' é obrigatório.");
        }
        return valor;
    }

    /**
     * Valida e normaliza um CPF: aceita "123.456.789-00" ou "12345678900"
     * e devolve apenas os 11 dígitos. Normalizar evita que o mesmo CPF,
     * digitado com e sem pontuação, seja tratado como dois clientes diferentes.
     *
     * Observação: valida apenas o formato (11 dígitos), não os dígitos
     * verificadores, pois isso não faz parte dos requisitos.
     */
    public static String validarCpf(String cpf) {
        String apenasDigitos = exigirTexto(cpf, "cpf").replaceAll("\\D", "");
        if (apenasDigitos.length() != TAMANHO_CPF) {
            throw new IllegalArgumentException("CPF inválido: deve conter " + TAMANHO_CPF + " dígitos.");
        }
        return apenasDigitos;
    }

    /** Valida o formato básico do e-mail e devolve o valor sem espaços. */
    public static String validarEmail(String email) {
        String valor = exigirTexto(email, "email");
        if (!PADRAO_EMAIL.matcher(valor).matches()) {
            throw new IllegalArgumentException("E-mail inválido: " + valor);
        }
        return valor;
    }

    /**
     * Valida um período: início e fim obrigatórios e fim estritamente
     * posterior ao início.
     */
    public static void validarPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        if (inicio == null || fim == null) {
            throw new ReservaInvalidaException("Data/hora de início e de término são obrigatórias.");
        }
        if (!fim.isAfter(inicio)) {
            throw new ReservaInvalidaException(
                    "Período inválido: o término (" + FormatoDataHora.formatar(fim)
                            + ") deve ser posterior ao início (" + FormatoDataHora.formatar(inicio) + ").");
        }
    }
}
