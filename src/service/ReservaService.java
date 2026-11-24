package service;

import exception.ConflitoReservaException;
import exception.ReservaInvalidaException;
import exception.ReservaNaoEncontradaException;
import model.Cliente;
import model.Recurso;
import model.Reserva;
import model.StatusReserva;
import repository.ReservaRepository;
import util.FormatoDataHora;
import util.Validador;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Regras de negócio das reservas: criação, alteração, cancelamento,
 * disponibilidade, conflitos e consultas.
 *
 * Usa ClienteService e RecursoService (e não os repositórios deles) para
 * obter clientes e recursos: assim, a regra "não operar sobre registros
 * inexistentes" e suas exceções ficam definidas em um único lugar.
 */
public class ReservaService {

    /** Valor usado quando nenhuma reserva deve ser ignorada na busca de conflitos. */
    private static final int NENHUMA_RESERVA = 0;

    private final ReservaRepository reservaRepository;
    private final ClienteService clienteService;
    private final RecursoService recursoService;

    public ReservaService(ReservaRepository reservaRepository,
                          ClienteService clienteService,
                          RecursoService recursoService) {
        this.reservaRepository = Validador.exigirNaoNulo(reservaRepository, "reservaRepository");
        this.clienteService = Validador.exigirNaoNulo(clienteService, "clienteService");
        this.recursoService = Validador.exigirNaoNulo(recursoService, "recursoService");
    }

    // ------------------------------------------------------------------
    // Operações principais
    // ------------------------------------------------------------------

    /**
     * Cria uma reserva ATIVA.
     * Ordem das validações:
     *  1. cliente existe;  2. recurso existe;  3. período válido;
     *  4. recurso disponível no período (sem conflito com reservas ativas).
     */
    public Reserva criar(String cpfCliente, int idRecurso, LocalDateTime inicio, LocalDateTime fim) {
        Cliente cliente = clienteService.buscarPorCpf(cpfCliente);
        Recurso recurso = recursoService.buscarPorId(idRecurso);
        Validador.validarPeriodo(inicio, fim);
        garantirSemConflito(recurso, inicio, fim, NENHUMA_RESERVA);

        Reserva nova = new Reserva(reservaRepository.gerarProximoId(), cliente, recurso, inicio, fim);
        reservaRepository.adicionar(nova);
        return nova;
    }

    /** Busca a reserva ou lança ReservaNaoEncontradaException. */
    public Reserva buscarPorId(int id) {
        return reservaRepository.buscarPorId(id)
                .orElseThrow(() -> new ReservaNaoEncontradaException(id));
    }

    public List<Reserva> listar() {
        return reservaRepository.listarTodas();
    }

    /**
     * Altera o período de uma reserva ativa.
     * A própria reserva é ignorada na verificação de conflitos — senão ela
     * conflitaria consigo mesma ao, por exemplo, ser estendida em 1 hora.
     */
    public Reserva alterarPeriodo(int idReserva, LocalDateTime novoInicio, LocalDateTime novoFim) {
        Reserva reserva = buscarPorId(idReserva);
        if (!reserva.isAtiva()) {
            throw new ReservaInvalidaException(
                    "Apenas reservas ativas podem ser alteradas. Status atual: "
                            + reserva.getStatus().getDescricao() + ".");
        }
        Validador.validarPeriodo(novoInicio, novoFim);
        garantirSemConflito(reserva.getRecurso(), novoInicio, novoFim, reserva.getId());

        reserva.alterarPeriodo(novoInicio, novoFim);
        reservaRepository.atualizar(reserva);
        return reserva;
    }

    /** Cancela a reserva; a partir daí ela deixa de bloquear o recurso. */
    public Reserva cancelar(int idReserva) {
        Reserva reserva = buscarPorId(idReserva);
        reserva.cancelar(); // a entidade recusa se já não estiver ATIVA
        reservaRepository.atualizar(reserva);
        return reserva;
    }

    /** Marca a reserva como concluída (utilizada). */
    public Reserva concluir(int idReserva) {
        Reserva reserva = buscarPorId(idReserva);
        reserva.concluir();
        reservaRepository.atualizar(reserva);
        return reserva;
    }

    // ------------------------------------------------------------------
    // Consultas
    // ------------------------------------------------------------------

    /** Todas as reservas (qualquer status) de um cliente existente. */
    public List<Reserva> listarPorCliente(String cpfCliente) {
        Cliente cliente = clienteService.buscarPorCpf(cpfCliente);
        return reservaRepository.buscarPorCliente(cliente.getCpf());
    }

    /** Todas as reservas (qualquer status) de um recurso existente. */
    public List<Reserva> listarPorRecurso(int idRecurso) {
        Recurso recurso = recursoService.buscarPorId(idRecurso);
        return reservaRepository.buscarPorRecurso(recurso.getId());
    }

    public List<Reserva> listarPorStatus(StatusReserva status) {
        Validador.exigirNaoNulo(status, "status");
        return reservaRepository.buscarPorStatus(status);
    }

    /**
     * Reservas (qualquer status) cujo período se sobrepõe, total ou
     * parcialmente, ao período informado.
     */
    public List<Reserva> listarPorPeriodo(LocalDateTime inicio, LocalDateTime fim) {
        Validador.validarPeriodo(inicio, fim);
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservaRepository.listarTodas()) {
            if (reserva.sobrepoe(inicio, fim)) {
                resultado.add(reserva);
            }
        }
        return List.copyOf(resultado);
    }

    /** true se o recurso não possui nenhuma reserva ativa sobreposta ao período. */
    public boolean verificarDisponibilidade(int idRecurso, LocalDateTime inicio, LocalDateTime fim) {
        return buscarConflitos(idRecurso, inicio, fim).isEmpty();
    }

    /**
     * Identifica as reservas ATIVAS do recurso que conflitam com o período.
     * Útil para informar ao usuário QUAIS reservas impedem a operação.
     */
    public List<Reserva> buscarConflitos(int idRecurso, LocalDateTime inicio, LocalDateTime fim) {
        Recurso recurso = recursoService.buscarPorId(idRecurso);
        Validador.validarPeriodo(inicio, fim);
        return buscarConflitos(recurso, inicio, fim, NENHUMA_RESERVA);
    }

    // ------------------------------------------------------------------
    // Métodos auxiliares (privados)
    // ------------------------------------------------------------------

    /**
     * Núcleo da regra de conflito. Uma reserva conflita quando:
     *  - é do mesmo recurso;
     *  - está em status que bloqueia o recurso (ATIVA);
     *  - seu período se sobrepõe ao solicitado;
     *  - não é a própria reserva sendo alterada (idIgnorado).
     */
    private List<Reserva> buscarConflitos(Recurso recurso, LocalDateTime inicio, LocalDateTime fim, int idIgnorado) {
        List<Reserva> conflitos = new ArrayList<>();
        for (Reserva existente : reservaRepository.buscarPorRecurso(recurso.getId())) {
            boolean outraReserva = existente.getId() != idIgnorado;
            if (outraReserva && existente.isAtiva() && existente.sobrepoe(inicio, fim)) {
                conflitos.add(existente);
            }
        }
        return List.copyOf(conflitos);
    }

    /** Lança ConflitoReservaException se houver qualquer conflito no período. */
    private void garantirSemConflito(Recurso recurso, LocalDateTime inicio, LocalDateTime fim, int idIgnorado) {
        List<Reserva> conflitos = buscarConflitos(recurso, inicio, fim, idIgnorado);
        if (!conflitos.isEmpty()) {
            List<Integer> ids = new ArrayList<>();
            for (Reserva conflito : conflitos) {
                ids.add(conflito.getId());
            }
            throw new ConflitoReservaException(
                    "O recurso " + recurso.getNome() + " não está disponível entre "
                            + FormatoDataHora.formatar(inicio) + " e " + FormatoDataHora.formatar(fim)
                            + ". Conflito com a(s) reserva(s): " + ids + ".");
        }
    }
}
