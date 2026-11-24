package model;

/**
 * Estados possíveis de uma reserva.
 *
 * Usar enum em vez de Strings soltas ("ativa", "Ativa", "ATIVO"...) faz o
 * compilador garantir que só existam valores válidos.
 *
 * Transições permitidas (controladas pela classe Reserva):
 *   ATIVA -> CANCELADA
 *   ATIVA -> CONCLUIDA
 * CANCELADA e CONCLUIDA são estados finais.
 */
public enum StatusReserva {

    ATIVA("Ativa", true),
    CANCELADA("Cancelada", false),
    CONCLUIDA("Concluída", false);

    private final String descricao;
    private final boolean bloqueiaRecurso;

    StatusReserva(String descricao, boolean bloqueiaRecurso) {
        this.descricao = descricao;
        this.bloqueiaRecurso = bloqueiaRecurso;
    }

    public String getDescricao() {
        return descricao;
    }

    /**
     * Indica se uma reserva neste status ocupa o recurso no seu período.
     * Apenas reservas ATIVAS bloqueiam; canceladas e concluídas liberam.
     * Centralizar essa decisão aqui evita espalhar "status == ATIVA" pelo código.
     */
    public boolean bloqueiaRecurso() {
        return bloqueiaRecurso;
    }
}
