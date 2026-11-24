package teste;

import model.Cliente;
import model.Recurso;
import model.Reserva;
import model.StatusReserva;
import repository.ClienteRepository;
import repository.RecursoRepository;
import repository.ReservaRepository;
import service.ClienteService;
import service.RecursoService;
import service.ReservaService;
import view.JanelaPrincipal;
import view.Modulo;
import view.componentes.CampoDataHora;
import view.componentes.CartaoIndicador;
import view.componentes.Notificador;
import view.componentes.Tema;

import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.time.LocalDateTime;

/**
 * Testes automáticos da interface gráfica integrada ao back-end.
 *
 * Abre a JanelaPrincipal real e simula o uso: preenche campos, clica em
 * botões (doClick) e seleciona linhas, conferindo o que a tela exibe e as
 * mensagens emitidas. As mensagens vão para um Notificador de teste (que
 * apenas registra), em vez de caixas de diálogo que travariam a execução.
 *
 * Requer ambiente gráfico (não funciona em modo headless).
 * Execução: java -cp out teste.CenariosInterface
 */
public class CenariosInterface {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 12, 1, 10, 0);

    private static ClienteService clienteService;
    private static RecursoService recursoService;
    private static ReservaService reservaService;
    private static NotificadorDeTeste notificador;
    private static JanelaPrincipal janela;

    private static int aprovados = 0;
    private static int reprovados = 0;

    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        try {
            SwingUtilities.invokeAndWait(() -> {
                montarSistema();
                testarNavegacao();
                testarClientes();
                testarRecursos();
                testarReservas();
                testarInicio();
            });
        } catch (InvocationTargetException e) {
            // Exceção inesperada na interface: conta como falha em vez de travar o teste.
            reprovados++;
            System.out.println("[FALHOU] Exceção inesperada durante os testes:");
            e.getCause().printStackTrace(System.out);
        } finally {
            exibirResumo();
            System.exit(reprovados > 0 ? 1 : 0); // encerra mesmo com a janela aberta
        }
    }

    private static void montarSistema() {
        ReservaRepository reservaRepository = new ReservaRepository();
        clienteService = new ClienteService(new ClienteRepository(), reservaRepository);
        recursoService = new RecursoService(new RecursoRepository(), reservaRepository);
        reservaService = new ReservaService(reservaRepository, clienteService, recursoService);
        notificador = new NotificadorDeTeste();

        Tema.aplicar();
        if (janela != null) {
            janela.dispose();
        }
        janela = new JanelaPrincipal(clienteService, recursoService, reservaService, notificador);
        janela.setVisible(true);
    }

    // ==================================================================
    // Cenários
    // ==================================================================

    private static void testarNavegacao() {
        titulo("INICIALIZAÇÃO E NAVEGAÇÃO");
        verificar("Aplicação inicia no módulo Início", janela.getModuloAtual() == Modulo.INICIO);
        for (Modulo modulo : Modulo.values()) {
            clicar("menu." + modulo.name());
            verificar("Menu leva ao módulo " + modulo.getTitulo(), janela.getModuloAtual() == modulo);
        }

        // Sair: a confirmação é recusada para não encerrar o processo de teste.
        notificador.limpar();
        notificador.respostaConfirmacao = false;
        clicar("menu.SAIR");
        verificar("Botão Sair pede confirmação; ao recusar, a janela continua aberta",
                notificador.confirmacoesPedidas == 1 && janela.isDisplayable());
        janela.dispatchEvent(new WindowEvent(janela, WindowEvent.WINDOW_CLOSING));
        verificar("Fechar no 'X' passa pela mesma confirmação",
                notificador.confirmacoesPedidas == 2 && janela.isDisplayable());
        notificador.respostaConfirmacao = true;
    }

    private static void testarClientes() {
        titulo("MÓDULO DE CLIENTES");
        clicar("menu.CLIENTES");

        verificar("Tabela vazia exibe mensagem orientando o cadastro",
                mensagemVaziaVisivel("cliente") && texto("cliente.vazio").startsWith("Nenhum cliente cadastrado"));

        // Formulário vazio: bloqueado pela própria tela, sem chamar o serviço.
        notificador.limpar();
        clicar("cliente.cadastrar");
        verificar("Formulário vazio gera aviso de campos obrigatórios",
                "aviso".equals(notificador.tipo) && clienteService.listar().isEmpty());

        preencherCliente("Ana Souza", "111.444.777-35", "ana@email.com", "(11) 91111-1111");
        clicar("cliente.cadastrar");
        verificar("Cadastro válido: mensagem de sucesso", "sucesso".equals(notificador.tipo));
        verificar("Cadastro válido: cliente aparece na tabela com CPF formatado",
                tabela("cliente").getRowCount() == 1
                        && "111.444.777-35".equals(tabela("cliente").getValueAt(0, 1)));
        verificar("Após cadastrar, formulário é limpo", campo("cliente.nome").getText().isEmpty());

        preencherCliente("Outra Ana", "11144477735", "outra@email.com", "(11) 90000-0000");
        clicar("cliente.cadastrar");
        verificar("CPF duplicado: erro do back-end com título 'Cadastro duplicado'",
                "erro".equals(notificador.tipo) && "Cadastro duplicado".equals(notificador.titulo));

        preencherCliente("Carla", "333.666.999-57", "email-invalido", "(11) 93333-3333");
        clicar("cliente.cadastrar");
        verificar("E-mail inválido: erro 'Dados inválidos' com a mensagem do back-end",
                "Dados inválidos".equals(notificador.titulo) && notificador.mensagem.contains("E-mail inválido"));

        preencherCliente("Bruno Lima", "222.555.888-46", "bruno@email.com", "(11) 92222-2222");
        clicar("cliente.cadastrar");
        verificar("Listagem mostra os 2 clientes cadastrados", tabela("cliente").getRowCount() == 2);

        // Pesquisa (filtro visual)
        campo("cliente.pesquisa").setText("bruno");
        verificar("Pesquisa 'bruno' mostra 1 linha e total '1 de 2 clientes'",
                tabela("cliente").getRowCount() == 1 && "1 de 2 clientes".equals(texto("cliente.total")));
        campo("cliente.pesquisa").setText("xyz");
        verificar("Pesquisa sem resultados exibe mensagem",
                mensagemVaziaVisivel("cliente") && texto("cliente.vazio").startsWith("Nenhum resultado"));
        campo("cliente.pesquisa").setText("");
        verificar("Limpar pesquisa volta a mostrar todos", tabela("cliente").getRowCount() == 2);

        // Seleção -> modo edição
        selecionarLinhaComTexto("cliente", "Ana Souza");
        verificar("Selecionar linha preenche o formulário",
                "Ana Souza".equals(campo("cliente.nome").getText())
                        && "ana@email.com".equals(campo("cliente.email").getText()));
        verificar("Em edição: CPF bloqueado, 'Atualizar' ativo e 'Cadastrar' inativo",
                !campo("cliente.cpf").isEditable() && botao("cliente.atualizar").isEnabled()
                        && !botao("cliente.cadastrar").isEnabled());

        campo("cliente.nome").setText("Ana Souza Silva");
        clicar("cliente.atualizar");
        verificar("Atualização: sucesso e tabela exibe o novo nome",
                "sucesso".equals(notificador.tipo) && linhaComTexto("cliente", "Ana Souza Silva") >= 0);
        verificar("Após atualizar, formulário volta ao modo cadastro",
                campo("cliente.cpf").isEditable() && !botao("cliente.atualizar").isEnabled());

        selecionarLinhaComTexto("cliente", "Bruno Lima");
        clicar("cliente.limpar");
        verificar("Botão Limpar cancela a edição",
                campo("cliente.nome").getText().isEmpty() && tabela("cliente").getSelectionModel().isSelectionEmpty());

        // Exclusão
        selecionarLinhaComTexto("cliente", "Bruno Lima");
        notificador.limpar();
        notificador.respostaConfirmacao = false;
        clicar("cliente.excluir");
        verificar("Exclusão pede confirmação; ao recusar, nada é excluído",
                notificador.confirmacoesPedidas == 1 && clienteService.listar().size() == 2);

        // Cliente com reserva ativa não pode ser excluído (regra do back-end).
        recursoService.cadastrar("Sala Teste", "Sala", null);
        Reserva reserva = reservaService.criar("22255588846", 1, BASE, BASE.plusHours(1));
        notificador.respostaConfirmacao = true;
        selecionarLinhaComTexto("cliente", "Bruno Lima");
        clicar("cliente.excluir");
        verificar("Excluir cliente com reserva ativa: 'Operação não permitida'",
                "Operação não permitida".equals(notificador.titulo) && clienteService.listar().size() == 2);

        reservaService.cancelar(reserva.getId());
        selecionarLinhaComTexto("cliente", "Bruno Lima");
        clicar("cliente.excluir");
        verificar("Exclusão confirmada: sucesso e cliente sai da tabela",
                "sucesso".equals(notificador.tipo) && tabela("cliente").getRowCount() == 1
                        && linhaComTexto("cliente", "Bruno Lima") < 0);
        verificar("Sem seleção, botões Editar/Excluir ficam inativos",
                !botao("cliente.excluir").isEnabled() && !botao("cliente.editar").isEnabled());
    }

    private static void testarRecursos() {
        titulo("MÓDULO DE RECURSOS");
        clicar("menu.RECURSOS");

        // "Sala Teste" (ID 1) foi criada pelo cenário de clientes, direto no serviço.
        verificar("Ao abrir o módulo, a tabela é carregada do back-end (Sala Teste, ID 1)",
                tabela("recurso").getRowCount() == 1 && Integer.valueOf(1).equals(tabela("recurso").getValueAt(0, 0)));

        notificador.limpar();
        preencherRecurso("", "Sala", "sem nome");
        clicar("recurso.cadastrar");
        verificar("Recurso sem nome: aviso de campo obrigatório e nada cadastrado",
                "aviso".equals(notificador.tipo) && recursoService.listar().size() == 1);

        preencherRecurso("Sala 101", "Sala de reunião", "Capacidade para 8 pessoas");
        clicar("recurso.cadastrar");
        verificar("Cadastro válido: sucesso informando o ID gerado (2)",
                "sucesso".equals(notificador.tipo) && notificador.mensagem.contains("ID 2"));

        preencherRecurso("Projetor Epson", "Equipamento", "");
        clicar("recurso.cadastrar");
        verificar("Descrição é opcional: recurso cadastrado sem descrição",
                "sucesso".equals(notificador.tipo) && tabela("recurso").getRowCount() == 3);

        for (int i = 4; i <= 10; i++) {
            recursoService.cadastrar("Recurso " + i, "Teste", null);
        }
        clicar("menu.RECURSOS");
        tabela("recurso").getRowSorter().toggleSortOrder(0); // clique no cabeçalho "ID"
        tabela("recurso").getRowSorter().toggleSortOrder(0); // ordem decrescente
        verificar("Ordenação por ID é numérica (10 vem antes de 9)",
                Integer.valueOf(10).equals(tabela("recurso").getValueAt(0, 0))
                        && Integer.valueOf(9).equals(tabela("recurso").getValueAt(1, 0)));
        tabela("recurso").getRowSorter().setSortKeys(null);

        selecionarLinhaComTexto("recurso", "Sala 101");
        verificar("Selecionar linha preenche o formulário e indica o ID em edição",
                "Sala 101".equals(campo("recurso.nome").getText())
                        && texto("recurso.modo").contains("ID 2"));

        campo("recurso.tipo").setText("Sala de treinamento");
        notificador.limpar();
        clicar("recurso.atualizar");
        verificar("Atualização: sucesso e tabela exibe o novo tipo",
                "sucesso".equals(notificador.tipo) && linhaComTexto("recurso", "Sala de treinamento") >= 0);

        campo("recurso.pesquisa").setText("epson");
        verificar("Pesquisa por texto filtra os recursos",
                tabela("recurso").getRowCount() == 1 && texto("recurso.total").startsWith("1 de 10"));
        campo("recurso.pesquisa").setText("");

        reservaService.criar("11144477735", 2, BASE, BASE.plusHours(2));
        selecionarLinhaComTexto("recurso", "Sala 101");
        notificador.limpar();
        notificador.respostaConfirmacao = true;
        clicar("recurso.excluir");
        verificar("Excluir recurso com reserva ativa: 'Operação não permitida'",
                "Operação não permitida".equals(notificador.titulo) && recursoService.listar().size() == 10);

        selecionarLinhaComTexto("recurso", "Projetor Epson");
        notificador.limpar();
        notificador.respostaConfirmacao = false;
        clicar("recurso.excluir");
        verificar("Excluir recurso: ao recusar a confirmação, nada muda",
                notificador.confirmacoesPedidas == 1 && recursoService.listar().size() == 10);

        notificador.respostaConfirmacao = true;
        clicar("recurso.excluir");
        verificar("Exclusão confirmada: recurso sai da tabela",
                "sucesso".equals(notificador.tipo) && linhaComTexto("recurso", "Projetor Epson") < 0);
    }

    private static void testarReservas() {
        titulo("MÓDULO DE RESERVAS");
        // Estado vindo dos cenários anteriores:
        //  clientes: Ana Souza Silva (111.444.777-35)
        //  recursos: 1 Sala Teste, 2 Sala 101, 4..10 Recurso N
        //  reservas: #1 Bruno/Sala Teste (cancelada), #2 Ana/Sala 101 10h-12h (ativa)
        clienteService.cadastrar("222.555.888-46", "Bruno Lima", "bruno@email.com", "(11) 92222-2222");
        clicar("menu.RESERVAS");

        verificar("Combos carregam clientes e recursos do back-end (+ opção 'Selecione')",
                combo("reserva.cliente").getItemCount() == clienteService.listar().size() + 1
                        && combo("reserva.recurso").getItemCount() == recursoService.listar().size() + 1);
        verificar("Tabela lista as reservas existentes com o status",
                tabela("reserva").getRowCount() == 2 && linhaComTexto("reserva", "CANCELADA") >= 0);

        notificador.limpar();
        clicar("reserva.confirmar");
        verificar("Confirmar sem cliente/recurso: aviso e nenhuma reserva criada",
                "aviso".equals(notificador.tipo) && reservaService.listar().size() == 2);

        Cliente bruno = clienteService.buscarPorCpf("22255588846");
        Recurso sala101 = recursoService.buscarPorId(2);

        // Verificar disponibilidade de um horário ocupado (11h-13h conflita com #2).
        preencherReserva(bruno, sala101, BASE.plusHours(1), BASE.plusHours(3));
        clicar("reserva.verificar");
        verificar("Verificar disponibilidade (ocupado): indica conflito com a reserva [2]",
                texto("reserva.disponibilidade").contains("Indisponível")
                        && texto("reserva.disponibilidade").contains("[2]"));

        clicar("reserva.confirmar");
        verificar("Confirmar com conflito: erro 'Conflito de horário' do back-end",
                "Conflito de horário".equals(notificador.titulo) && reservaService.listar().size() == 2);

        // Período livre (12h-14h encosta em #2, mas não conflita).
        preencherReserva(bruno, sala101, BASE.plusHours(2), BASE.plusHours(4));
        clicar("reserva.verificar");
        verificar("Verificar disponibilidade (livre): indica disponível",
                texto("reserva.disponibilidade").contains("disponível"));
        campoData("reserva.fim").setValor(BASE.plusHours(5));
        verificar("Alterar o período apaga o resultado da verificação anterior",
                texto("reserva.disponibilidade").isEmpty());

        clicar("reserva.confirmar");
        verificar("Reserva válida: sucesso e nova linha na tabela (#3)",
                "sucesso".equals(notificador.tipo) && notificador.mensagem.contains("#3")
                        && tabela("reserva").getRowCount() == 3);
        verificar("Após confirmar, formulário é limpo (combos em 'Selecione')",
                combo("reserva.cliente").getSelectedItem() == null);

        preencherReserva(bruno, sala101, BASE.plusDays(1), BASE.plusDays(1));
        clicar("reserva.confirmar");
        verificar("Término igual ao início: erro 'Reserva inválida' do back-end",
                "Reserva inválida".equals(notificador.titulo));

        // Filtros (cada um usa a consulta correspondente do serviço).
        combo("reserva.filtroCliente").setSelectedItem(bruno);
        verificar("Filtro por cliente (Bruno): 2 reservas",
                tabela("reserva").getRowCount() == 2 && texto("reserva.total").equals("Exibindo 2 de 3 reservas"));
        combo("reserva.filtroStatus").setSelectedItem(StatusReserva.ATIVA);
        verificar("Filtros combinados (Bruno + Ativa): 1 reserva",
                tabela("reserva").getRowCount() == 1);
        combo("reserva.filtroCliente").setSelectedItem(null);
        combo("reserva.filtroStatus").setSelectedItem(null);
        combo("reserva.filtroRecurso").setSelectedItem(sala101);
        verificar("Filtro por recurso (Sala 101): 2 reservas", tabela("reserva").getRowCount() == 2);
        combo("reserva.filtroStatus").setSelectedItem(StatusReserva.CONCLUIDA);
        verificar("Filtro sem resultados exibe mensagem própria",
                mensagemVaziaVisivel("reserva") && texto("reserva.vazio").contains("filtros"));
        clicar("reserva.limparFiltros");
        verificar("Limpar filtros volta a exibir todas", tabela("reserva").getRowCount() == 3);

        // Cancelamento
        selecionarLinhaComValor("reserva", 1);
        verificar("Reserva cancelada selecionada: botões Cancelar/Concluir inativos",
                !botao("reserva.cancelar").isEnabled() && !botao("reserva.concluir").isEnabled());

        selecionarLinhaComValor("reserva", 2);
        notificador.limpar();
        notificador.respostaConfirmacao = false;
        clicar("reserva.cancelar");
        verificar("Cancelar pede confirmação; ao recusar, a reserva continua ativa",
                notificador.confirmacoesPedidas == 1 && reservaService.buscarPorId(2).isAtiva());

        notificador.respostaConfirmacao = true;
        clicar("reserva.cancelar");
        verificar("Cancelamento confirmado: sucesso e status 'Cancelada' na tabela",
                "sucesso".equals(notificador.tipo)
                        && reservaService.buscarPorId(2).getStatus() == StatusReserva.CANCELADA
                        && StatusReserva.CANCELADA.equals(valorDaLinha("reserva", 2, 4)));

        preencherReserva(bruno, sala101, BASE, BASE.plusHours(2));
        clicar("reserva.confirmar");
        verificar("Nova reserva no período liberado pelo cancelamento: sucesso",
                "sucesso".equals(notificador.tipo) && tabela("reserva").getRowCount() == 4);

        selecionarLinhaComValor("reserva", 3);
        clicar("reserva.concluir");
        verificar("Concluir reserva: status 'Concluída'",
                reservaService.buscarPorId(3).getStatus() == StatusReserva.CONCLUIDA);

        // Sincronização: novo cliente aparece no combo ao voltar para o módulo.
        clienteService.cadastrar("333.666.999-57", "Carla Mendes", "carla@email.com", "(21) 93333-3333");
        clicar("menu.RESERVAS");
        verificar("Ao reabrir o módulo, combos incluem clientes cadastrados depois",
                combo("reserva.cliente").getItemCount() == clienteService.listar().size() + 1);
    }

    private static void testarInicio() {
        titulo("INÍCIO (DASHBOARD) E INTEGRAÇÃO");

        clicar("menu.INICIO");
        int ativas = reservaService.listarPorStatus(StatusReserva.ATIVA).size();
        verificar("Indicadores refletem os dados reais do back-end",
                indicador("inicio.clientes").equals(String.valueOf(clienteService.listar().size()))
                        && indicador("inicio.recursos").equals(String.valueOf(recursoService.listar().size()))
                        && indicador("inicio.reservas").equals(String.valueOf(ativas)));
        verificar("Com o sistema já populado, 'Carregar dados de exemplo' fica inativo",
                !botao("inicio.dadosExemplo").isEnabled());

        // Criar uma reserva no módulo Reservas e voltar ao Início: o indicador acompanha.
        Cliente carla = clienteService.buscarPorCpf("33366699957");
        preencherReservaNaTela(carla, recursoService.buscarPorId(4), BASE.plusDays(5), BASE.plusDays(5).plusHours(1));
        clicar("reserva.confirmar");
        clicar("menu.INICIO");
        verificar("Após criar reserva em outro módulo, indicador de ativas é atualizado (+1)",
                indicador("inicio.reservas").equals(String.valueOf(ativas + 1)));

        // Atalhos de acesso rápido
        clicar("inicio.irClientes");
        verificar("Acesso rápido 'Gerenciar clientes' abre o módulo Clientes", janela.getModuloAtual() == Modulo.CLIENTES);
        clicar("menu.INICIO");
        clicar("inicio.irRecursos");
        verificar("Acesso rápido 'Gerenciar recursos' abre o módulo Recursos", janela.getModuloAtual() == Modulo.RECURSOS);
        clicar("menu.INICIO");
        clicar("inicio.irReservas");
        verificar("Acesso rápido 'Nova reserva' abre o módulo Reservas", janela.getModuloAtual() == Modulo.RESERVAS);

        // Sistema novo e vazio para testar os dados de exemplo.
        montarSistema();
        verificar("Sistema vazio: indicadores zerados e botão de exemplo ativo",
                indicador("inicio.clientes").equals("0") && indicador("inicio.reservas").equals("0")
                        && botao("inicio.dadosExemplo").isEnabled());

        notificador.limpar();
        notificador.respostaConfirmacao = false;
        clicar("inicio.dadosExemplo");
        verificar("Dados de exemplo pedem confirmação; ao recusar, nada é carregado",
                notificador.confirmacoesPedidas == 1 && clienteService.listar().isEmpty());

        notificador.respostaConfirmacao = true;
        clicar("inicio.dadosExemplo");
        verificar("Dados de exemplo carregados pelos serviços: 5 clientes, 6 recursos, 8 reservas",
                "sucesso".equals(notificador.tipo) && clienteService.listar().size() == 5
                        && recursoService.listar().size() == 6 && reservaService.listar().size() == 8);
        verificar("Exemplos incluem os três status (6 ativas, 1 cancelada, 1 concluída)",
                reservaService.listarPorStatus(StatusReserva.ATIVA).size() == 6
                        && reservaService.listarPorStatus(StatusReserva.CANCELADA).size() == 1
                        && reservaService.listarPorStatus(StatusReserva.CONCLUIDA).size() == 1);
        verificar("Indicadores atualizados na hora (5 / 6 / 6) e botão desativado",
                indicador("inicio.clientes").equals("5") && indicador("inicio.recursos").equals("6")
                        && indicador("inicio.reservas").equals("6") && !botao("inicio.dadosExemplo").isEnabled());

        clicar("menu.CLIENTES");
        verificar("Módulo Clientes exibe os clientes de exemplo", tabela("cliente").getRowCount() == 5);
        clicar("menu.RESERVAS");
        verificar("Módulo Reservas exibe as reservas e carrega os combos",
                tabela("reserva").getRowCount() == 8 && combo("reserva.cliente").getItemCount() == 6);
    }

    private static void preencherReservaNaTela(Cliente cliente, Recurso recurso,
                                               LocalDateTime inicio, LocalDateTime fim) {
        clicar("menu.RESERVAS");
        preencherReserva(cliente, recurso, inicio, fim);
    }

    private static String indicador(String nome) {
        return componente(nome, CartaoIndicador.class).getValorExibido();
    }

    // ==================================================================
    // Ações sobre a interface
    // ==================================================================

    private static void preencherCliente(String nome, String cpf, String email, String telefone) {
        clicar("cliente.limpar");
        campo("cliente.nome").setText(nome);
        campo("cliente.cpf").setText(cpf);
        campo("cliente.email").setText(email);
        campo("cliente.telefone").setText(telefone);
        notificador.limpar();
    }

    private static void preencherRecurso(String nome, String tipo, String descricao) {
        clicar("recurso.limpar");
        campo("recurso.nome").setText(nome);
        campo("recurso.tipo").setText(tipo);
        campo("recurso.descricao").setText(descricao);
        notificador.limpar();
    }

    private static void preencherReserva(Cliente cliente, Recurso recurso,
                                         LocalDateTime inicio, LocalDateTime fim) {
        clicar("reserva.limpar");
        combo("reserva.cliente").setSelectedItem(cliente);
        combo("reserva.recurso").setSelectedItem(recurso);
        campoData("reserva.inicio").setValor(inicio);
        campoData("reserva.fim").setValor(fim);
        notificador.limpar();
    }

    private static void selecionarLinhaComValor(String prefixo, Object valorColunaZero) {
        JTable tabela = tabela(prefixo);
        for (int linha = 0; linha < tabela.getRowCount(); linha++) {
            if (valorColunaZero.equals(tabela.getValueAt(linha, 0))) {
                tabela.setRowSelectionInterval(linha, linha);
                return;
            }
        }
        throw new IllegalStateException("Linha não encontrada: " + valorColunaZero);
    }

    private static Object valorDaLinha(String prefixo, Object valorColunaZero, int coluna) {
        JTable tabela = tabela(prefixo);
        for (int linha = 0; linha < tabela.getRowCount(); linha++) {
            if (valorColunaZero.equals(tabela.getValueAt(linha, 0))) {
                return tabela.getValueAt(linha, coluna);
            }
        }
        return null;
    }

    private static void clicar(String nome) {
        botao(nome).doClick();
    }

    private static void selecionarLinhaComTexto(String prefixo, String texto) {
        int linha = linhaComTexto(prefixo, texto);
        if (linha < 0) {
            throw new IllegalStateException("Linha não encontrada: " + texto);
        }
        tabela(prefixo).setRowSelectionInterval(linha, linha);
    }

    private static int linhaComTexto(String prefixo, String texto) {
        JTable tabela = tabela(prefixo);
        for (int linha = 0; linha < tabela.getRowCount(); linha++) {
            for (int coluna = 0; coluna < tabela.getColumnCount(); coluna++) {
                if (texto.equals(String.valueOf(tabela.getValueAt(linha, coluna)))) {
                    return linha;
                }
            }
        }
        return -1;
    }

    private static boolean mensagemVaziaVisivel(String prefixo) {
        return componente(prefixo + ".vazio", JLabel.class).getParent().isVisible();
    }

    // ==================================================================
    // Localização de componentes pelo nome (setName)
    // ==================================================================

    private static AbstractButton botao(String nome) {
        return componente(nome, AbstractButton.class);
    }

    private static JTextField campo(String nome) {
        return componente(nome, JTextField.class);
    }

    private static JComboBox<?> combo(String nome) {
        return componente(nome, JComboBox.class);
    }

    private static CampoDataHora campoData(String nome) {
        return componente(nome, CampoDataHora.class);
    }

    private static JTable tabela(String prefixo) {
        return componente(prefixo + ".tabela", JTable.class);
    }

    private static String texto(String nome) {
        return componente(nome, JLabel.class).getText();
    }

    private static <T extends Component> T componente(String nome, Class<T> tipo) {
        Component encontrado = procurar(janela.getContentPane(), nome);
        if (encontrado == null) {
            throw new IllegalStateException("Componente não encontrado: " + nome);
        }
        return tipo.cast(encontrado);
    }

    private static Component procurar(Container raiz, String nome) {
        for (Component filho : raiz.getComponents()) {
            if (nome.equals(filho.getName())) {
                return filho;
            }
            if (filho instanceof Container) {
                Component achado = procurar((Container) filho, nome);
                if (achado != null) {
                    return achado;
                }
            }
        }
        return null;
    }

    // ==================================================================
    // Registro de resultados
    // ==================================================================

    private static void verificar(String descricao, boolean condicao) {
        if (condicao) {
            aprovados++;
            System.out.println("[OK]     " + descricao);
        } else {
            reprovados++;
            System.out.println("[FALHOU] " + descricao);
            System.out.println("         última mensagem: " + notificador.tipo + " | "
                    + notificador.titulo + " | " + notificador.mensagem);
        }
    }

    private static void titulo(String texto) {
        System.out.println();
        System.out.println("=== " + texto + " ===");
    }

    private static void exibirResumo() {
        System.out.println();
        System.out.println("=== RESUMO DA INTERFACE ===");
        System.out.println("Aprovados: " + aprovados);
        System.out.println("Reprovados: " + reprovados);
        System.out.println(reprovados == 0 ? "Todos os cenários passaram." : "Há cenários com falha!");
    }

    /** Notificador que registra a última mensagem em vez de abrir diálogos. */
    private static class NotificadorDeTeste implements Notificador {

        String tipo;
        String titulo;
        String mensagem;
        boolean respostaConfirmacao = true;
        int confirmacoesPedidas;

        void limpar() {
            tipo = null;
            titulo = null;
            mensagem = null;
            confirmacoesPedidas = 0;
        }

        @Override
        public void sucesso(String mensagem) {
            registrar("sucesso", "Sucesso", mensagem);
        }

        @Override
        public void aviso(String mensagem) {
            registrar("aviso", "Atenção", mensagem);
        }

        @Override
        public void erro(String titulo, String mensagem) {
            registrar("erro", titulo, mensagem);
        }

        @Override
        public boolean confirmar(String titulo, String mensagem) {
            confirmacoesPedidas++;
            return respostaConfirmacao;
        }

        private void registrar(String tipo, String titulo, String mensagem) {
            this.tipo = tipo;
            this.titulo = titulo;
            this.mensagem = mensagem;
        }
    }
}
