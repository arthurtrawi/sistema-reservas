package exception;

/**
 * Lançada quando uma operação referencia um cliente que não está cadastrado.
 */
public class ClienteNaoEncontradoException extends SistemaReservasException {

    private static final long serialVersionUID = 1L;

    public ClienteNaoEncontradoException(String cpf) {
        super("Cliente não encontrado. CPF: " + cpf);
    }
}
