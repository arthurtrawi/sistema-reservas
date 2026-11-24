package teste;

import exception.CadastroDuplicadoException;
import exception.ClienteNaoEncontradoException;
import exception.ConflitoReservaException;
import exception.OperacaoNaoPermitidaException;
import exception.RecursoNaoEncontradoException;
import exception.ReservaInvalidaException;
import exception.ReservaNaoEncontradaException;
import model.Reserva;
import model.StatusReserva;
import repository.ClienteRepository;
import repository.RecursoRepository;
import repository.ReservaRepository;
import service.ClienteService;
import service.RecursoService;
import service.ReservaService;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Cenários de teste manuais do back-end (antes em Main.java).
 *
 * 1. Monta o sistema: cria repositórios e serviços e "liga" as dependências
 *    (injeção de dependência manual — o papel que um framework faria).
 * 2. Executa cenários de teste manuais, válidos e inválidos, exibindo o
 *    resultado esperado e o obtido, e um resumo final.
 *
 * Execução: java -cp out teste.CenariosBackend
 * (a interface gráfica é iniciada por Main).
 */
public class CenariosBackend {

    // Data/hora base dos cenários: 01/12/2026 às 10h.
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 12, 1, 10, 0);

    private static final String CPF_ANA = "111.444.777-35";
    private static final String CPF_BRUNO = "222.555.888-46";
    private static final String CPF_INEXISTENTE = "999.999.999-99";
    private static final int ID_RECURSO_INEXISTENTE = 999;

    private static ClienteService clienteService;
    private static RecursoService recursoService;
    private static ReservaService reservaService;

    private static int aprovados = 0;
    private static int reprovados = 0;

    /** Ação de teste que pode lançar exceção (interface funcional, usada com lambdas). */
    @FunctionalInterface
    private interface Acao {
        void executar();
    }

    public static void main(String[] args) {
        montarSistema();

        testarClientes();
        testarRecursos();
        testarReservas();
        testarCancelamento();
        testarConsultas();
        testarAlteracaoEStatus();
        testarIntegridadeNaExclusao();

        exibirResumo();
        if (reprovados > 0) {
            System.exit(1);
        }
    }

    /** Cria as camadas e injeta as dependências pelo construtor. */
    private static void montarSistema() {
        ClienteRepository clienteRepository = new ClienteRepository();
        RecursoRepository recursoRepository = new RecursoRepository();
        ReservaRepository reservaRepository = new ReservaRepository();

        clienteService = new ClienteService(clienteRepository, reservaRepository);
        recursoService = new RecursoService(recursoRepository, reservaRepository);
        reservaService = new ReservaService(reservaRepository, clienteService, recursoService);
    }

    // ==================================================================
    // Cenários
    // ==================================================================

    private static void testarClientes() {
        titulo("CLIENTES");

        esperarSucesso("01. Cadastro de cliente válido (Ana)",
                () -> clienteService.cadastrar(CPF_ANA, "Ana Souza", "ana@email.com", "(11) 91111-1111"));
        esperarSucesso("01b. Cadastro de cliente válido (Bruno)",
                () -> clienteService.cadastrar(CPF_BRUNO, "Bruno Lima", "bruno@email.com", "(11) 92222-2222"));

        esperarErro("02. Cadastro de cliente duplicado (mesmo CPF sem pontuação)",
                CadastroDuplicadoException.class,
                () -> clienteService.cadastrar("11144477735", "Outra Ana", "outra@email.com", "(11) 90000-0000"));

        esperarErro("Extra. Cadastro de cliente com nome vazio",
                IllegalArgumentException.class,
                () -> clienteService.cadastrar("333.666.999-57", "   ", "c@email.com", "(11) 93333-3333"));
        esperarErro("Extra. Cadastro de cliente com e-mail inválido",
                IllegalArgumentException.class,
                () -> clienteService.cadastrar("333.666.999-57", "Carla", "email-invalido", "(11) 93333-3333"));

        esperarSucesso("Extra. Atualização de cliente válida",
                () -> clienteService.atualizar(CPF_ANA, "Ana Souza Silva", "ana.silva@email.com", "(11) 91111-0000"));
        verificar("Extra. Dados atualizados foram gravados",
                clienteService.buscarPorCpf(CPF_ANA).getNome().equals("Ana Souza Silva"));

        verificar("Extra. Listagem retorna os 2 clientes cadastrados",
                clienteService.listar().size() == 2);
    }

    private static void testarRecursos() {
        titulo("RECURSOS");

        esperarSucesso("03. Cadastro de recurso válido (Sala 101 -> id 1)",
                () -> recursoService.cadastrar("Sala 101", "Sala de reunião", "Capacidade para 8 pessoas"));
        esperarSucesso("03b. Cadastro de recurso válido (Projetor -> id 2)",
                () -> recursoService.cadastrar("Projetor Epson", "Equipamento", null));

        esperarErro("Extra. Cadastro de recurso sem nome",
                IllegalArgumentException.class,
                () -> recursoService.cadastrar("", "Sala", "sem nome"));

        verificar("Extra. Ids gerados em sequência (1 e 2)",
                recursoService.buscarPorId(1).getNome().equals("Sala 101")
                        && recursoService.buscarPorId(2).getNome().equals("Projetor Epson"));
    }

    private static void testarReservas() {
        titulo("RESERVAS");

        esperarSucesso("04. Reserva válida: Ana, Sala 101, 10h-12h (reserva 1)",
                () -> reservaService.criar(CPF_ANA, 1, BASE, BASE.plusHours(2)));

        esperarErro("05. Bruno tenta reservar a Sala 101 no mesmo período (10h-12h)",
                ConflitoReservaException.class,
                () -> reservaService.criar(CPF_BRUNO, 1, BASE, BASE.plusHours(2)));
        esperarErro("05b. Bruno tenta reservar com sobreposição parcial (11h-13h)",
                ConflitoReservaException.class,
                () -> reservaService.criar(CPF_BRUNO, 1, BASE.plusHours(1), BASE.plusHours(3)));

        esperarSucesso("06. Período diferente no mesmo recurso: Bruno, Sala 101, 14h-16h (reserva 2)",
                () -> reservaService.criar(CPF_BRUNO, 1, BASE.plusHours(4), BASE.plusHours(6)));
        esperarSucesso("06b. Período adjacente (12h-14h) não conflita: Bruno (reserva 3)",
                () -> reservaService.criar(CPF_BRUNO, 1, BASE.plusHours(2), BASE.plusHours(4)));
        esperarSucesso("06c. Outro recurso no mesmo horário: Bruno, Projetor, 10h-12h (reserva 4)",
                () -> reservaService.criar(CPF_BRUNO, 2, BASE, BASE.plusHours(2)));

        esperarErro("07. Reserva com cliente inexistente",
                ClienteNaoEncontradoException.class,
                () -> reservaService.criar(CPF_INEXISTENTE, 1, BASE.plusDays(1), BASE.plusDays(1).plusHours(1)));

        esperarErro("08. Reserva com recurso inexistente",
                RecursoNaoEncontradoException.class,
                () -> reservaService.criar(CPF_ANA, ID_RECURSO_INEXISTENTE, BASE.plusDays(1), BASE.plusDays(1).plusHours(1)));

        esperarErro("Extra. Reserva com término igual ao início",
                ReservaInvalidaException.class,
                () -> reservaService.criar(CPF_ANA, 2, BASE.plusDays(1), BASE.plusDays(1)));
        esperarErro("Extra. Reserva com término anterior ao início",
                ReservaInvalidaException.class,
                () -> reservaService.criar(CPF_ANA, 2, BASE.plusDays(1), BASE.plusDays(1).minusHours(1)));
        esperarErro("Extra. Reserva sem data de início",
                ReservaInvalidaException.class,
                () -> reservaService.criar(CPF_ANA, 2, null, BASE.plusDays(1)));
    }

    private static void testarCancelamento() {
        titulo("CANCELAMENTO");

        verificar("Extra. Antes do cancelamento, Sala 101 indisponível às 10h-12h",
                !reservaService.verificarDisponibilidade(1, BASE, BASE.plusHours(2)));

        esperarSucesso("09. Cancelamento da reserva 1 (Ana, Sala 101, 10h-12h)",
                () -> reservaService.cancelar(1));
        verificar("09b. Reserva 1 está com status CANCELADA",
                reservaService.buscarPorId(1).getStatus() == StatusReserva.CANCELADA);
        verificar("09c. Após o cancelamento, Sala 101 disponível às 10h-12h",
                reservaService.verificarDisponibilidade(1, BASE, BASE.plusHours(2)));

        esperarSucesso("10. Bruno reserva 10h-12h, período liberado pelo cancelamento (reserva 5)",
                () -> reservaService.criar(CPF_BRUNO, 1, BASE, BASE.plusHours(2)));

        esperarErro("Extra. Cancelar reserva já cancelada",
                ReservaInvalidaException.class,
                () -> reservaService.cancelar(1));
        esperarErro("Extra. Cancelar reserva inexistente",
                ReservaNaoEncontradaException.class,
                () -> reservaService.cancelar(999));
    }

    private static void testarConsultas() {
        titulo("CONSULTAS");

        // Estado neste ponto:
        //  1: Ana   - Sala 101 - 10h-12h - CANCELADA
        //  2: Bruno - Sala 101 - 14h-16h - ATIVA
        //  3: Bruno - Sala 101 - 12h-14h - ATIVA
        //  4: Bruno - Projetor - 10h-12h - ATIVA
        //  5: Bruno - Sala 101 - 10h-12h - ATIVA

        List<Reserva> daAna = reservaService.listarPorCliente(CPF_ANA);
        List<Reserva> doBruno = reservaService.listarPorCliente(CPF_BRUNO);
        exibirLista("Reservas da Ana", daAna);
        exibirLista("Reservas do Bruno", doBruno);
        verificar("11. Consulta por cliente: Ana tem 1 reserva e Bruno tem 4",
                daAna.size() == 1 && doBruno.size() == 4);

        List<Reserva> daSala = reservaService.listarPorRecurso(1);
        List<Reserva> doProjetor = reservaService.listarPorRecurso(2);
        exibirLista("Reservas da Sala 101", daSala);
        verificar("12. Consulta por recurso: Sala 101 tem 4 reservas e Projetor tem 1",
                daSala.size() == 4 && doProjetor.size() == 1);

        verificar("Extra. Consulta por status: 4 ativas e 1 cancelada",
                reservaService.listarPorStatus(StatusReserva.ATIVA).size() == 4
                        && reservaService.listarPorStatus(StatusReserva.CANCELADA).size() == 1);

        verificar("Extra. Consulta por período 11h-13h: reservas 1, 3, 4 e 5",
                idsDe(reservaService.listarPorPeriodo(BASE.plusHours(1), BASE.plusHours(3)))
                        .equals(List.of(1, 3, 4, 5)));

        verificar("Extra. Conflitos na Sala 101 de 11h-15h: reservas ativas 2, 3 e 5",
                idsDe(reservaService.buscarConflitos(1, BASE.plusHours(1), BASE.plusHours(5)))
                        .equals(List.of(2, 3, 5)));

        verificar("Extra. Sala 101 disponível no dia seguinte",
                reservaService.verificarDisponibilidade(1, BASE.plusDays(1), BASE.plusDays(1).plusHours(2)));

        esperarErro("Extra. Consulta de reservas de cliente inexistente",
                ClienteNaoEncontradoException.class,
                () -> reservaService.listarPorCliente(CPF_INEXISTENTE));
    }

    private static void testarAlteracaoEStatus() {
        titulo("ALTERAÇÃO DE PERÍODO E STATUS");

        esperarErro("Extra. Estender reserva 2 (14h-16h) para 13h-16h conflita com a reserva 3",
                ConflitoReservaException.class,
                () -> reservaService.alterarPeriodo(2, BASE.plusHours(3), BASE.plusHours(6)));

        esperarSucesso("Extra. Estender reserva 2 para 14h-17h (só se sobrepõe a ela mesma)",
                () -> reservaService.alterarPeriodo(2, BASE.plusHours(4), BASE.plusHours(7)));
        verificar("Extra. Novo término da reserva 2 é 17h",
                reservaService.buscarPorId(2).getFim().equals(BASE.plusHours(7)));

        esperarErro("Extra. Alterar período de reserva cancelada",
                ReservaInvalidaException.class,
                () -> reservaService.alterarPeriodo(1, BASE.plusDays(2), BASE.plusDays(2).plusHours(1)));

        esperarSucesso("Extra. Concluir reserva 4 (Projetor)",
                () -> reservaService.concluir(4));
        verificar("Extra. Reserva concluída libera o Projetor",
                reservaService.verificarDisponibilidade(2, BASE, BASE.plusHours(2)));
        esperarErro("Extra. Cancelar reserva já concluída",
                ReservaInvalidaException.class,
                () -> reservaService.cancelar(4));
    }

    private static void testarIntegridadeNaExclusao() {
        titulo("INTEGRIDADE NA EXCLUSÃO");

        esperarErro("Extra. Excluir cliente com reservas ativas (Bruno)",
                OperacaoNaoPermitidaException.class,
                () -> clienteService.excluir(CPF_BRUNO));
        esperarErro("Extra. Excluir recurso com reservas ativas (Sala 101)",
                OperacaoNaoPermitidaException.class,
                () -> recursoService.excluir(1));

        esperarSucesso("Extra. Excluir recurso sem reservas ativas (Projetor: só reserva concluída)",
                () -> recursoService.excluir(2));
        esperarSucesso("Extra. Excluir cliente sem reservas ativas (Ana: só reserva cancelada)",
                () -> clienteService.excluir(CPF_ANA));

        esperarErro("Extra. Cliente excluído não é mais encontrado",
                ClienteNaoEncontradoException.class,
                () -> clienteService.buscarPorCpf(CPF_ANA));
        verificar("Extra. Histórico preservado: reserva 1 ainda existe e referencia a Ana",
                reservaService.buscarPorId(1).getCliente().getNome().equals("Ana Souza Silva"));
    }

    // ==================================================================
    // Infraestrutura de teste
    // ==================================================================

    /** O cenário deve executar sem lançar exceção. */
    private static void esperarSucesso(String descricao, Acao acao) {
        try {
            acao.executar();
            registrar(true, descricao, "esperado: sucesso | obtido: sucesso");
        } catch (RuntimeException e) {
            registrar(false, descricao, "esperado: sucesso | obtido: "
                    + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /** O cenário deve lançar exatamente o tipo de exceção indicado (ou uma subclasse dele). */
    private static void esperarErro(String descricao, Class<? extends RuntimeException> tipoEsperado, Acao acao) {
        try {
            acao.executar();
            registrar(false, descricao, "esperado: " + tipoEsperado.getSimpleName() + " | obtido: sucesso");
        } catch (RuntimeException e) {
            boolean tipoCorreto = tipoEsperado.isInstance(e);
            registrar(tipoCorreto, descricao, "esperado: " + tipoEsperado.getSimpleName()
                    + " | obtido: " + e.getClass().getSimpleName() + " - " + e.getMessage());
        }
    }

    /** Verifica uma condição sobre o estado do sistema. */
    private static void verificar(String descricao, boolean condicao) {
        registrar(condicao, descricao, condicao ? "condição verdadeira" : "condição FALSA");
    }

    private static void registrar(boolean passou, String descricao, String detalhe) {
        if (passou) {
            aprovados++;
        } else {
            reprovados++;
        }
        System.out.println((passou ? "[OK]     " : "[FALHOU] ") + descricao);
        System.out.println("         " + detalhe);
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }

    private static void exibirLista(String rotulo, List<Reserva> reservas) {
        System.out.println("  " + rotulo + ":");
        for (Reserva reserva : reservas) {
            System.out.println("    " + reserva);
        }
    }

    private static List<Integer> idsDe(List<Reserva> reservas) {
        return reservas.stream().map(Reserva::getId).sorted().toList();
    }

    private static void exibirResumo() {
        System.out.println();
        System.out.println("=== RESUMO ===");
        System.out.println("Aprovados: " + aprovados);
        System.out.println("Reprovados: " + reprovados);
        System.out.println(reprovados == 0 ? "Todos os cenários passaram." : "Há cenários com falha!");
    }
}
