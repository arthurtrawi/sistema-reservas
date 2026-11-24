package exception;

/**
 * Lançada quando uma operação é válida em formato, mas proibida pelo estado
 * atual do sistema (ex.: excluir cliente ou recurso que possui reservas ativas).
 */
public class OperacaoNaoPermitidaException extends SistemaReservasException {

    private static final long serialVersionUID = 1L;

    public OperacaoNaoPermitidaException(String mensagem) {
        super(mensagem);
    }
}
