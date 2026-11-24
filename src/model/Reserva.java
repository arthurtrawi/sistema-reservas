package model;

import exception.ReservaInvalidaException;
import util.Validador;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Representa a reserva de um recurso por um cliente em um período.
 *
 * Associações: a reserva conhece seu Cliente e seu Recurso (referências
 * finais — uma reserva nunca troca de dono nem de recurso). A associação é
 * unidirecional: Cliente e Recurso não guardam listas de reservas.
 *
 * Encapsulamento de comportamento: não há setStatus() nem setInicio()/setFim().
 * O estado só muda por métodos com intenção clara (cancelar, concluir,
 * alterarPeriodo), que verificam se a transição é permitida.
 */
public class Reserva {

    private final int id;
    private final Cliente cliente;
    private final Recurso recurso;
    private LocalDateTime inicio;
    private LocalDateTime fim;
    private StatusReserva status;

    /** Cria uma reserva já no status ATIVA, com período validado. */
    public Reserva(int id, Cliente cliente, Recurso recurso, LocalDateTime inicio, LocalDateTime fim) {
        if (id <= 0) {
            throw new IllegalArgumentException("O id da reserva deve ser positivo.");
        }
        Validador.validarPeriodo(inicio, fim);
        this.id = id;
        this.cliente = Validador.exigirNaoNulo(cliente, "cliente");
        this.recurso = Validador.exigirNaoNulo(recurso, "recurso");
        this.inicio = inicio;
        this.fim = fim;
        this.status = StatusReserva.ATIVA;
    }

    public int getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Recurso getRecurso() {
        return recurso;
    }

    public LocalDateTime getInicio() {
        return inicio;
    }

    public LocalDateTime getFim() {
        return fim;
    }

    public StatusReserva getStatus() {
        return status;
    }

    /** Atalho legível: a reserva ocupa o recurso no seu período? */
    public boolean isAtiva() {
        return status.bloqueiaRecurso();
    }

    /**
     * Verifica se o período desta reserva se sobrepõe ao período informado.
     *
     * Dois intervalos [A_ini, A_fim) e [B_ini, B_fim) se sobrepõem quando
     *     A_ini < B_fim  E  B_ini < A_fim
     * Intervalos que apenas "encostam" (10h–12h e 12h–14h) NÃO se sobrepõem.
     *
     * Este método considera somente as datas; quem decide se o status e o
     * recurso importam é a camada de serviço.
     */
    public boolean sobrepoe(LocalDateTime outroInicio, LocalDateTime outroFim) {
        return inicio.isBefore(outroFim) && outroInicio.isBefore(fim);
    }

    /**
     * Altera o período da reserva. A verificação de conflito com outras
     * reservas é responsabilidade do serviço (a entidade não conhece as demais).
     */
    public void alterarPeriodo(LocalDateTime novoInicio, LocalDateTime novoFim) {
        exigirAtiva("alterar");
        Validador.validarPeriodo(novoInicio, novoFim);
        this.inicio = novoInicio;
        this.fim = novoFim;
    }

    /** ATIVA -> CANCELADA. A partir daqui, a reserva não bloqueia mais o recurso. */
    public void cancelar() {
        exigirAtiva("cancelar");
        this.status = StatusReserva.CANCELADA;
    }

    /** ATIVA -> CONCLUIDA. Registra que a reserva foi utilizada. */
    public void concluir() {
        exigirAtiva("concluir");
        this.status = StatusReserva.CONCLUIDA;
    }

    /** Regra comum às transições: só reservas ATIVAS podem ser modificadas. */
    private void exigirAtiva(String operacao) {
        if (status != StatusReserva.ATIVA) {
            throw new ReservaInvalidaException(
                    "Não é possível " + operacao + " a reserva " + id
                            + ": status atual é " + status.getDescricao() + ".");
        }
    }

    /** Duas reservas são a mesma reserva se tiverem o mesmo id. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Reserva)) {
            return false;
        }
        Reserva outra = (Reserva) o;
        return id == outra.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Reserva{id=" + id
                + ", cliente=" + cliente.getNome()
                + ", recurso=" + recurso.getNome()
                + ", inicio=" + inicio
                + ", fim=" + fim
                + ", status=" + status.getDescricao() + "}";
    }
}
