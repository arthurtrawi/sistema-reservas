package exception;

/**
 * Lançada quando uma operação referencia um recurso que não está cadastrado.
 */
public class RecursoNaoEncontradoException extends SistemaReservasException {

    private static final long serialVersionUID = 1L;

    public RecursoNaoEncontradoException(int id) {
        super("Recurso não encontrado. ID: " + id);
    }
}
