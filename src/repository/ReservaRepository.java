package repository;

import model.Reserva;
import model.StatusReserva;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Armazenamento em memória das reservas.
 *
 * Oferece consultas por atributo (cliente, recurso, status) — o equivalente
 * a um "WHERE" num banco de dados. Consultas com significado de negócio
 * (conflitos, disponibilidade, período) ficam no ReservaService.
 *
 * Não há método de remoção: reservas não são apagadas, são canceladas,
 * preservando o histórico.
 */
public class ReservaRepository {

    private final List<Reserva> reservas = new ArrayList<>();
    private int ultimoId = 0;

    /** Próximo id sequencial; ids nunca são reaproveitados. */
    public int gerarProximoId() {
        ultimoId++;
        return ultimoId;
    }

    public void adicionar(Reserva reserva) {
        reservas.add(reserva);
    }

    public Optional<Reserva> buscarPorId(int id) {
        for (Reserva reserva : reservas) {
            if (reserva.getId() == id) {
                return Optional.of(reserva);
            }
        }
        return Optional.empty();
    }

    /** Cópia somente leitura da lista interna. */
    public List<Reserva> listarTodas() {
        return List.copyOf(reservas);
    }

    public List<Reserva> buscarPorCliente(String cpf) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getCliente().getCpf().equals(cpf)) {
                resultado.add(reserva);
            }
        }
        return List.copyOf(resultado);
    }

    public List<Reserva> buscarPorRecurso(int idRecurso) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getRecurso().getId() == idRecurso) {
                resultado.add(reserva);
            }
        }
        return List.copyOf(resultado);
    }

    public List<Reserva> buscarPorStatus(StatusReserva status) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservas) {
            if (reserva.getStatus() == status) {
                resultado.add(reserva);
            }
        }
        return List.copyOf(resultado);
    }

    /** @return true se a reserva existia e foi atualizada */
    public boolean atualizar(Reserva reserva) {
        int indice = reservas.indexOf(reserva); // usa equals() -> compara id
        if (indice < 0) {
            return false;
        }
        reservas.set(indice, reserva);
        return true;
    }
}
