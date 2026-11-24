package view.componentes;

import exception.CadastroDuplicadoException;
import exception.ClienteNaoEncontradoException;
import exception.ConflitoReservaException;
import exception.OperacaoNaoPermitidaException;
import exception.RecursoNaoEncontradoException;
import exception.ReservaInvalidaException;
import exception.ReservaNaoEncontradaException;

/**
 * Contrato para exibir mensagens ao usuário.
 *
 * As telas dependem desta INTERFACE, não de JOptionPane diretamente.
 * Em uso normal, a implementação é NotificadorDialogo (janelas de diálogo);
 * nos testes automáticos, uma implementação que apenas registra as
 * mensagens — sem abrir janelas que travariam o teste. Isso é polimorfismo:
 * o mesmo código da tela funciona com qualquer implementação.
 */
public interface Notificador {

    void sucesso(String mensagem);

    /** Aviso de uso da interface (ex.: campos obrigatórios vazios, nada selecionado). */
    void aviso(String mensagem);

    void erro(String titulo, String mensagem);

    /** @return true se o usuário confirmou a operação */
    boolean confirmar(String titulo, String mensagem);

    /**
     * Exibe um erro vindo do back-end com um título adequado ao tipo da
     * exceção. A mensagem é a do próprio back-end — a tela não reescreve
     * nem reinterpreta as regras de negócio.
     */
    default void erro(RuntimeException excecao) {
        erro(tituloPara(excecao), excecao.getMessage());
    }

    /**
     * Título amigável para cada tipo de exceção. A ordem importa:
     * ConflitoReservaException é subclasse de ReservaInvalidaException,
     * então precisa ser testada antes.
     */
    static String tituloPara(RuntimeException excecao) {
        if (excecao instanceof ConflitoReservaException) {
            return "Conflito de horário";
        }
        if (excecao instanceof ClienteNaoEncontradoException
                || excecao instanceof RecursoNaoEncontradoException
                || excecao instanceof ReservaNaoEncontradaException) {
            return "Registro não encontrado";
        }
        if (excecao instanceof CadastroDuplicadoException) {
            return "Cadastro duplicado";
        }
        if (excecao instanceof OperacaoNaoPermitidaException) {
            return "Operação não permitida";
        }
        if (excecao instanceof ReservaInvalidaException) {
            return "Reserva inválida";
        }
        if (excecao instanceof IllegalArgumentException) {
            return "Dados inválidos";
        }
        return "Erro";
    }
}
