package exception;

/**
 * Lançada ao tentar cadastrar um registro cujo identificador já existe
 * (ex.: cliente com CPF já cadastrado).
 */
public class CadastroDuplicadoException extends SistemaReservasException {

    private static final long serialVersionUID = 1L;

    public CadastroDuplicadoException(String mensagem) {
        super(mensagem);
    }
}
