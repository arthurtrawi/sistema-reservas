package exception;

/**
 * Lançada quando uma reserva viola alguma regra: período inválido
 * (término anterior ou igual ao início) ou transição de status proibida
 * (ex.: cancelar uma reserva já cancelada).
 */
public class ReservaInvalidaException extends SistemaReservasException {

    private static final long serialVersionUID = 1L;

    public ReservaInvalidaException(String mensagem) {
        super(mensagem);
    }
}
