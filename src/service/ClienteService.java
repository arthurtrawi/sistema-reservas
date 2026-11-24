package service;

import exception.CadastroDuplicadoException;
import exception.ClienteNaoEncontradoException;
import exception.OperacaoNaoPermitidaException;
import model.Cliente;
import model.Reserva;
import repository.ClienteRepository;
import repository.ReservaRepository;
import util.Validador;

import java.util.List;

/**
 * Regras de negócio dos clientes.
 *
 * Depende do ReservaRepository apenas para consultar se um cliente possui
 * reservas ativas antes de excluí-lo (integridade referencial).
 * As dependências são recebidas pelo construtor (injeção de dependência
 * manual), o que permite montar o sistema no Main sem frameworks.
 */
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final ReservaRepository reservaRepository;

    public ClienteService(ClienteRepository clienteRepository, ReservaRepository reservaRepository) {
        this.clienteRepository = Validador.exigirNaoNulo(clienteRepository, "clienteRepository");
        this.reservaRepository = Validador.exigirNaoNulo(reservaRepository, "reservaRepository");
    }

    /**
     * Cadastra um novo cliente.
     * O construtor de Cliente valida os campos; aqui validamos a regra de
     * negócio que depende dos demais registros: CPF não pode se repetir.
     */
    public Cliente cadastrar(String cpf, String nome, String email, String telefone) {
        Cliente novo = new Cliente(cpf, nome, email, telefone);
        if (clienteRepository.existePorCpf(novo.getCpf())) {
            throw new CadastroDuplicadoException("Já existe cliente cadastrado com o CPF " + novo.getCpf() + ".");
        }
        clienteRepository.adicionar(novo);
        return novo;
    }

    /** Busca o cliente ou lança ClienteNaoEncontradoException. */
    public Cliente buscarPorCpf(String cpf) {
        String cpfNormalizado = Validador.validarCpf(cpf);
        return clienteRepository.buscarPorCpf(cpfNormalizado)
                .orElseThrow(() -> new ClienteNaoEncontradoException(cpfNormalizado));
    }

    public List<Cliente> listar() {
        return clienteRepository.listarTodos();
    }

    /**
     * Atualiza os dados cadastrais (o CPF, identificador, não muda).
     * Todos os novos valores são validados ANTES de qualquer alteração,
     * para não deixar o cliente parcialmente atualizado se um campo for inválido.
     */
    public Cliente atualizar(String cpf, String novoNome, String novoEmail, String novoTelefone) {
        Cliente cliente = buscarPorCpf(cpf);

        String nomeValidado = Validador.exigirTexto(novoNome, "nome");
        String emailValidado = Validador.validarEmail(novoEmail);
        String telefoneValidado = Validador.exigirTexto(novoTelefone, "telefone");

        cliente.setNome(nomeValidado);
        cliente.setEmail(emailValidado);
        cliente.setTelefone(telefoneValidado);
        clienteRepository.atualizar(cliente);
        return cliente;
    }

    /**
     * Exclui o cliente, desde que ele não possua reservas ATIVAS.
     * Reservas canceladas/concluídas não impedem a exclusão: elas guardam a
     * referência ao objeto Cliente e continuam válidas como histórico.
     */
    public void excluir(String cpf) {
        Cliente cliente = buscarPorCpf(cpf);
        if (possuiReservasAtivas(cliente)) {
            throw new OperacaoNaoPermitidaException(
                    "O cliente " + cliente.getNome() + " possui reservas ativas e não pode ser excluído. "
                            + "Cancele as reservas antes.");
        }
        clienteRepository.remover(cliente.getCpf());
    }

    private boolean possuiReservasAtivas(Cliente cliente) {
        for (Reserva reserva : reservaRepository.buscarPorCliente(cliente.getCpf())) {
            if (reserva.isAtiva()) {
                return true;
            }
        }
        return false;
    }
}
