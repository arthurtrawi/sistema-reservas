package exception;

/**
 * Exceção base de todas as violações de regra de negócio do sistema.
 *
 * É "unchecked" (herda de RuntimeException): não obriga declarar "throws"
 * em toda a cadeia de chamadas, mantendo o código limpo. Uma futura camada
 * de Front-end pode capturar qualquer erro de negócio com um único
 * catch (SistemaReservasException e).
 */
public class SistemaReservasException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public SistemaReservasException(String mensagem) {
        super(mensagem);
    }
}
