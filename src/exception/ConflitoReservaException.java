package exception;

/**
 * Lançada quando o período solicitado se sobrepõe a uma reserva ATIVA
 * do mesmo recurso.
 *
 * Herda de ReservaInvalidaException porque um conflito de horário É UM tipo
 * de reserva inválida (relação "é-um" da herança). Assim, quem captura
 * ReservaInvalidaException também trata conflitos, e quem precisa de
 * tratamento específico pode capturar apenas esta classe.
 */
public class ConflitoReservaException extends ReservaInvalidaException {

    private static final long serialVersionUID = 1L;

    public ConflitoReservaException(String mensagem) {
        super(mensagem);
    }
}
