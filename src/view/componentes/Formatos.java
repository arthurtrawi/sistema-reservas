package view.componentes;

import util.FormatoDataHora;

import java.time.LocalDateTime;

/**
 * Formatação de dados para EXIBIÇÃO na interface.
 *
 * O back-end guarda os dados em formato "técnico" (CPF só com dígitos,
 * LocalDateTime); aqui eles são apresentados no formato brasileiro.
 * Nenhuma regra de negócio: apenas apresentação.
 */
public final class Formatos {

    private static final int TAMANHO_CPF = 11;

    private Formatos() {
        // Não instanciável.
    }

    /** "11144477735" -> "111.444.777-35". Valores fora do padrão são exibidos como vieram. */
    public static String cpf(String digitos) {
        if (digitos == null || digitos.length() != TAMANHO_CPF) {
            return digitos;
        }
        return digitos.substring(0, 3) + "." + digitos.substring(3, 6) + "."
                + digitos.substring(6, 9) + "-" + digitos.substring(9);
    }

    public static String dataHora(LocalDateTime valor) {
        return FormatoDataHora.formatar(valor); // mesmo padrão das mensagens do back-end
    }
}
