package view.reserva;

import view.componentes.Formatos;

import java.time.LocalDateTime;

/**
 * Valor exibido na coluna "Período" da tabela de reservas.
 *
 * Junta início e término em uma única coluna (economizando espaço) sem
 * perder a ordenação correta: a tabela ordena pelo compareTo (início e,
 * em empate, término), e não pelo texto exibido.
 *
 * Objeto imutável (atributos final, sem setters).
 */
public final class Periodo implements Comparable<Periodo> {

    private final LocalDateTime inicio;
    private final LocalDateTime fim;

    public Periodo(LocalDateTime inicio, LocalDateTime fim) {
        this.inicio = inicio;
        this.fim = fim;
    }

    @Override
    public int compareTo(Periodo outro) {
        int comparacao = inicio.compareTo(outro.inicio);
        return comparacao != 0 ? comparacao : fim.compareTo(outro.fim);
    }

    /** "01/12/2026 10:00 – 12:00" (mesmo dia) ou "01/12/2026 22:00 – 02/12/2026 02:00". */
    @Override
    public String toString() {
        String termino = inicio.toLocalDate().equals(fim.toLocalDate())
                ? fim.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                : Formatos.dataHora(fim);
        return Formatos.dataHora(inicio) + " – " + termino;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Periodo)) {
            return false;
        }
        Periodo outro = (Periodo) o;
        return inicio.equals(outro.inicio) && fim.equals(outro.fim);
    }

    @Override
    public int hashCode() {
        return 31 * inicio.hashCode() + fim.hashCode();
    }
}
