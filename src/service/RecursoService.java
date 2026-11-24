package service;

import exception.OperacaoNaoPermitidaException;
import exception.RecursoNaoEncontradoException;
import model.Recurso;
import model.Reserva;
import repository.RecursoRepository;
import repository.ReservaRepository;
import util.Validador;

import java.util.List;

/**
 * Regras de negócio dos recursos reserváveis.
 *
 * A unicidade do identificador é garantida pela geração sequencial de ids
 * no repositório, por isso não há verificação de duplicidade aqui.
 */
public class RecursoService {

    private final RecursoRepository recursoRepository;
    private final ReservaRepository reservaRepository;

    public RecursoService(RecursoRepository recursoRepository, ReservaRepository reservaRepository) {
        this.recursoRepository = Validador.exigirNaoNulo(recursoRepository, "recursoRepository");
        this.reservaRepository = Validador.exigirNaoNulo(reservaRepository, "reservaRepository");
    }

    public Recurso cadastrar(String nome, String tipo, String descricao) {
        // Valida antes de consumir um id, para não gerar "buracos" na sequência
        // quando os dados forem inválidos.
        Validador.exigirTexto(nome, "nome");
        Validador.exigirTexto(tipo, "tipo");

        Recurso novo = new Recurso(recursoRepository.gerarProximoId(), nome, tipo, descricao);
        recursoRepository.adicionar(novo);
        return novo;
    }

    /** Busca o recurso ou lança RecursoNaoEncontradoException. */
    public Recurso buscarPorId(int id) {
        return recursoRepository.buscarPorId(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(id));
    }

    public List<Recurso> listar() {
        return recursoRepository.listarTodos();
    }

    /** Atualiza os dados do recurso; valida tudo antes de alterar qualquer campo. */
    public Recurso atualizar(int id, String novoNome, String novoTipo, String novaDescricao) {
        Recurso recurso = buscarPorId(id);

        String nomeValidado = Validador.exigirTexto(novoNome, "nome");
        String tipoValidado = Validador.exigirTexto(novoTipo, "tipo");

        recurso.setNome(nomeValidado);
        recurso.setTipo(tipoValidado);
        recurso.setDescricao(novaDescricao);
        recursoRepository.atualizar(recurso);
        return recurso;
    }

    /** Exclui o recurso, desde que ele não possua reservas ATIVAS. */
    public void excluir(int id) {
        Recurso recurso = buscarPorId(id);
        if (possuiReservasAtivas(recurso)) {
            throw new OperacaoNaoPermitidaException(
                    "O recurso " + recurso.getNome() + " possui reservas ativas e não pode ser excluído. "
                            + "Cancele as reservas antes.");
        }
        recursoRepository.remover(recurso.getId());
    }

    private boolean possuiReservasAtivas(Recurso recurso) {
        for (Reserva reserva : reservaRepository.buscarPorRecurso(recurso.getId())) {
            if (reserva.isAtiva()) {
                return true;
            }
        }
        return false;
    }
}
