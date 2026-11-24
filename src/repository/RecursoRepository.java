package repository;

import model.Recurso;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Armazenamento em memória dos recursos, incluindo a geração de ids
 * sequenciais (papel que, num banco de dados, seria do auto-incremento).
 */
public class RecursoRepository {

    private final List<Recurso> recursos = new ArrayList<>();
    private int ultimoId = 0;

    /**
     * Reserva o próximo id da sequência. Os ids nunca são reaproveitados,
     * mesmo após exclusões, para que um id antigo não passe a apontar para
     * outro recurso.
     */
    public int gerarProximoId() {
        ultimoId++;
        return ultimoId;
    }

    public void adicionar(Recurso recurso) {
        recursos.add(recurso);
    }

    public Optional<Recurso> buscarPorId(int id) {
        for (Recurso recurso : recursos) {
            if (recurso.getId() == id) {
                return Optional.of(recurso);
            }
        }
        return Optional.empty();
    }

    public boolean existePorId(int id) {
        return buscarPorId(id).isPresent();
    }

    /** Cópia somente leitura da lista interna. */
    public List<Recurso> listarTodos() {
        return List.copyOf(recursos);
    }

    /** @return true se o recurso existia e foi atualizado */
    public boolean atualizar(Recurso recurso) {
        int indice = recursos.indexOf(recurso); // usa equals() -> compara id
        if (indice < 0) {
            return false;
        }
        recursos.set(indice, recurso);
        return true;
    }

    /** @return true se havia um recurso com esse id e ele foi removido */
    public boolean remover(int id) {
        return recursos.removeIf(recurso -> recurso.getId() == id);
    }
}
