package repository;

import model.Cliente;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Armazenamento em memória dos clientes.
 *
 * Responsabilidade única: guardar e recuperar objetos. NÃO contém regras de
 * negócio (ex.: impedir CPF duplicado é decisão do ClienteService).
 * Se no futuro houver banco de dados, só esta classe muda.
 */
public class ClienteRepository {

    private final List<Cliente> clientes = new ArrayList<>();

    public void adicionar(Cliente cliente) {
        clientes.add(cliente);
    }

    /**
     * Busca pelo CPF (já normalizado, só dígitos).
     * Retorna Optional em vez de null: obriga quem chama a tratar o caso
     * "não encontrado", evitando NullPointerException.
     */
    public Optional<Cliente> buscarPorCpf(String cpf) {
        for (Cliente cliente : clientes) {
            if (cliente.getCpf().equals(cpf)) {
                return Optional.of(cliente);
            }
        }
        return Optional.empty();
    }

    public boolean existePorCpf(String cpf) {
        return buscarPorCpf(cpf).isPresent();
    }

    /**
     * Retorna uma cópia somente leitura: quem recebe a lista não consegue
     * adicionar ou remover clientes "por fora" do repositório.
     */
    public List<Cliente> listarTodos() {
        return List.copyOf(clientes);
    }

    /**
     * Substitui o registro de mesmo CPF. Em memória o objeto já é o mesmo,
     * mas manter este método deixa explícito o ponto de "gravação" — essencial
     * quando o armazenamento for trocado por um banco de dados.
     *
     * @return true se o cliente existia e foi atualizado
     */
    public boolean atualizar(Cliente cliente) {
        int indice = clientes.indexOf(cliente); // usa equals() -> compara CPF
        if (indice < 0) {
            return false;
        }
        clientes.set(indice, cliente);
        return true;
    }

    /** @return true se havia um cliente com esse CPF e ele foi removido */
    public boolean remover(String cpf) {
        return clientes.removeIf(cliente -> cliente.getCpf().equals(cpf));
    }
}
