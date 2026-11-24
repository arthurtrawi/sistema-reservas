package exception;

/**
 * Lançada quando uma operação (consulta, alteração, cancelamento)
 * referencia uma reserva que não existe.
 */
public class ReservaNaoEncontradaException extends SistemaReservasException {

    private static final long serialVersionUID = 1L;

    public ReservaNaoEncontradaException(int id) {
        super("Reserva não encontrada. ID: " + id);
    }
}
