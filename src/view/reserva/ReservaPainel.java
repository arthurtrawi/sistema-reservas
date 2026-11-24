package view.reserva;

import exception.SistemaReservasException;
import model.Cliente;
import model.Recurso;
import model.Reserva;
import model.StatusReserva;
import service.ClienteService;
import service.RecursoService;
import service.ReservaService;
import view.Atualizavel;
import view.componentes.Botao;
import view.componentes.CampoDataHora;
import view.componentes.ComponentesUI;
import view.componentes.Formatos;
import view.componentes.FormularioPainel;
import view.componentes.Notificador;
import view.componentes.PainelTabela;
import view.componentes.RenderizadorCombo;
import view.componentes.Tema;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Módulo de Reservas — o módulo central do sistema.
 *
 * Esquerda: formulário de nova reserva (cliente, recurso, início, término),
 *           com verificação de disponibilidade e confirmação.
 * Direita:  tabela de reservas com filtros por cliente, recurso e status,
 *           status colorido e ações de cancelar e concluir.
 *
 * Disponibilidade, conflitos e validade do período são decididos pelo
 * ReservaService; a tela apenas apresenta o resultado.
 */
public class ReservaPainel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private static final int COLUNA_ID = 0;
    private static final int COLUNA_STATUS = 4;
    private static final int LARGURA_FORMULARIO = 360;

    private final ReservaService reservaService;
    private final ClienteService clienteService;
    private final RecursoService recursoService;
    private final Notificador notificador;

    // ---- Formulário de nova reserva ----
    private final JComboBox<Cliente> comboCliente = ComponentesUI.combo();
    private final JComboBox<Recurso> comboRecurso = ComponentesUI.combo();
    private final CampoDataHora campoInicio = new CampoDataHora();
    private final CampoDataHora campoFim = new CampoDataHora();
    private final JLabel rotuloDisponibilidade = new JLabel();
    private final Botao botaoVerificar = new Botao("Verificar disponibilidade", Botao.Estilo.SECUNDARIO);
    private final Botao botaoConfirmar = new Botao("Confirmar reserva", Botao.Estilo.PRIMARIO);
    private final Botao botaoLimpar = new Botao("Limpar", Botao.Estilo.SECUNDARIO);

    // ---- Listagem ----
    private final JComboBox<Cliente> filtroCliente = ComponentesUI.combo();
    private final JComboBox<Recurso> filtroRecurso = ComponentesUI.combo();
    private final JComboBox<StatusReserva> filtroStatus = ComponentesUI.combo();
    private final Botao botaoLimparFiltros = new Botao("Limpar filtros", Botao.Estilo.SECUNDARIO);
    private final JLabel rotuloTotal = ComponentesUI.textoSecundario("");
    private final Botao botaoConcluir = new Botao("Concluir", Botao.Estilo.SECUNDARIO);
    private final Botao botaoCancelar = new Botao("Cancelar reserva", Botao.Estilo.PERIGO);
    private final PainelTabela tabela = new PainelTabela("reserva", "",
            "ID", "Cliente", "Recurso", "Período", "Status");

    /** Evita que os filtros disparem consultas enquanto os combos são recarregados. */
    private boolean recarregandoCombos;

    public ReservaPainel(ReservaService reservaService, ClienteService clienteService,
                         RecursoService recursoService, Notificador notificador) {
        super(new BorderLayout(Tema.ESPACO, 0));
        this.reservaService = reservaService;
        this.clienteService = clienteService;
        this.recursoService = recursoService;
        this.notificador = notificador;
        setOpaque(false);

        configurarCombos();
        nomearComponentes();
        add(criarFormulario(), BorderLayout.WEST);
        add(criarListagem(), BorderLayout.CENTER);
        registrarEventos();
        limparFormulario();
    }

    /** Ao abrir o módulo: recarrega clientes e recursos nos combos e a tabela. */
    @Override
    public void atualizar() {
        recarregarCombos();
        recarregarTabela();
        limparDisponibilidade();
    }

    // ==================================================================
    // Montagem da tela
    // ==================================================================

    private void configurarCombos() {
        comboCliente.setRenderer(new RenderizadorCombo<Cliente>(ReservaPainel::descreverCliente, "Selecione um cliente"));
        comboRecurso.setRenderer(new RenderizadorCombo<Recurso>(ReservaPainel::descreverRecurso, "Selecione um recurso"));
        filtroCliente.setRenderer(new RenderizadorCombo<Cliente>(Cliente::getNome, "Todos"));
        filtroRecurso.setRenderer(new RenderizadorCombo<Recurso>(Recurso::getNome, "Todos"));
        filtroStatus.setRenderer(new RenderizadorCombo<StatusReserva>(StatusReserva::getDescricao, "Todos"));

        List<StatusReserva> status = new ArrayList<>(List.of(StatusReserva.values()));
        preencherCombo(filtroStatus, status);
    }

    private JComponent criarFormulario() {
        rotuloDisponibilidade.setFont(Tema.FONTE_ROTULO);
        rotuloDisponibilidade.setOpaque(true);
        rotuloDisponibilidade.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        FormularioPainel formulario = new FormularioPainel();
        formulario.adicionarCampo("Cliente", true, comboCliente, null);
        formulario.adicionarCampo("Recurso", true, comboRecurso, null);
        formulario.adicionarCampo("Início", true, campoInicio, "Data (dd/mm/aaaa) e hora (hh:mm)");
        formulario.adicionarCampo("Término", true, campoFim, "Deve ser posterior ao início");
        formulario.adicionarComponente(ComponentesUI.linhaBotoes(botaoVerificar), 8);
        formulario.adicionarComponente(rotuloDisponibilidade, 12);
        formulario.adicionarComponente(ComponentesUI.linhaBotoes(botaoConfirmar, botaoLimpar), 14);
        formulario.adicionarComponente(ComponentesUI.textoSecundario("* Campos obrigatórios"), 0);
        formulario.adicionarEspacoFlexivel();

        JPanel cartao = ComponentesUI.cartao("Nova reserva", formulario);
        cartao.setPreferredSize(new Dimension(LARGURA_FORMULARIO, 0));
        return cartao;
    }

    private JComponent criarListagem() {
        JPanel filtros = ComponentesUI.painelTransparente(new GridLayout(2, 4, Tema.ESPACO_PEQUENO, 4));
        filtros.add(ComponentesUI.rotuloCampo("Cliente", false));
        filtros.add(ComponentesUI.rotuloCampo("Recurso", false));
        filtros.add(ComponentesUI.rotuloCampo("Status", false));
        filtros.add(new JLabel());
        filtros.add(filtroCliente);
        filtros.add(filtroRecurso);
        filtros.add(filtroStatus);
        filtros.add(botaoLimparFiltros);

        JPanel rodape = ComponentesUI.painelTransparente(new BorderLayout());
        rodape.add(rotuloTotal, BorderLayout.WEST);
        rodape.add(ComponentesUI.linhaBotoes(botaoConcluir, botaoCancelar), BorderLayout.EAST);

        JPanel conteudo = ComponentesUI.painelTransparente(new BorderLayout(0, Tema.ESPACO - 4));
        conteudo.add(filtros, BorderLayout.NORTH);
        conteudo.add(tabela, BorderLayout.CENTER);
        conteudo.add(rodape, BorderLayout.SOUTH);

        tabela.definirRenderizador(COLUNA_STATUS, new StatusReservaRenderer());
        tabela.definirLarguraColuna(COLUNA_ID, 36);
        tabela.definirLarguraColuna(1, 120);
        tabela.definirLarguraColuna(2, 120);
        tabela.definirLarguraColuna(3, 175);
        tabela.definirLarguraColuna(COLUNA_STATUS, 105);

        return ComponentesUI.cartao("Reservas", conteudo);
    }

    /** Nomes usados para localizar os componentes nos testes automáticos. */
    private void nomearComponentes() {
        comboCliente.setName("reserva.cliente");
        comboRecurso.setName("reserva.recurso");
        campoInicio.setName("reserva.inicio");
        campoFim.setName("reserva.fim");
        rotuloDisponibilidade.setName("reserva.disponibilidade");
        botaoVerificar.setName("reserva.verificar");
        botaoConfirmar.setName("reserva.confirmar");
        botaoLimpar.setName("reserva.limpar");
        filtroCliente.setName("reserva.filtroCliente");
        filtroRecurso.setName("reserva.filtroRecurso");
        filtroStatus.setName("reserva.filtroStatus");
        botaoLimparFiltros.setName("reserva.limparFiltros");
        rotuloTotal.setName("reserva.total");
        botaoConcluir.setName("reserva.concluir");
        botaoCancelar.setName("reserva.cancelar");
    }

    // ==================================================================
    // Eventos
    // ==================================================================

    private void registrarEventos() {
        botaoVerificar.addActionListener(evento -> verificarDisponibilidade());
        botaoConfirmar.addActionListener(evento -> confirmarReserva());
        botaoLimpar.addActionListener(evento -> limparFormulario());
        botaoLimparFiltros.addActionListener(evento -> limparFiltros());
        botaoCancelar.addActionListener(evento -> cancelarSelecionada());
        botaoConcluir.addActionListener(evento -> concluirSelecionada());

        // ItemListener: qualquer mudança no formulário invalida a verificação anterior.
        comboCliente.addItemListener(evento -> aoAlterarFormulario(comboCliente));
        comboRecurso.addItemListener(evento -> aoAlterarFormulario(comboRecurso));
        campoInicio.aoAlterar(this::limparDisponibilidade);
        campoFim.aoAlterar(this::limparDisponibilidade);

        // ActionListener nos filtros: dispara uma única vez por escolha (inclusive "Todos").
        filtroCliente.addActionListener(evento -> aoAlterarFiltro());
        filtroRecurso.addActionListener(evento -> aoAlterarFiltro());
        filtroStatus.addActionListener(evento -> aoAlterarFiltro());

        tabela.aoMudarSelecao(this::atualizarBotoesDeAcao);
    }

    private void aoAlterarFormulario(JComboBox<?> combo) {
        ComponentesUI.marcarErroCombo(combo, false);
        limparDisponibilidade();
    }

    private void aoAlterarFiltro() {
        if (!recarregandoCombos) {
            recarregarTabela();
        }
    }

    // ==================================================================
    // Operações (todas delegadas ao ReservaService)
    // ==================================================================

    private void verificarDisponibilidade() {
        Recurso recurso = selecionado(comboRecurso);
        if (recurso == null) {
            ComponentesUI.marcarErroCombo(comboRecurso, true);
            notificador.aviso("Selecione o recurso para verificar a disponibilidade.");
            return;
        }
        try {
            LocalDateTime inicio = campoInicio.getValor();
            LocalDateTime fim = campoFim.getValor();
            if (reservaService.verificarDisponibilidade(recurso.getId(), inicio, fim)) {
                exibirDisponibilidade(true, recurso.getNome() + " está disponível neste período.");
            } else {
                List<Integer> ids = new ArrayList<>();
                for (Reserva conflito : reservaService.buscarConflitos(recurso.getId(), inicio, fim)) {
                    ids.add(conflito.getId());
                }
                exibirDisponibilidade(false, "Indisponível: conflito com a(s) reserva(s) " + ids + ".");
            }
        } catch (SistemaReservasException | IllegalArgumentException e) {
            notificador.erro(e);
        }
    }

    private void confirmarReserva() {
        Cliente cliente = selecionado(comboCliente);
        Recurso recurso = selecionado(comboRecurso);
        ComponentesUI.marcarErroCombo(comboCliente, cliente == null);
        ComponentesUI.marcarErroCombo(comboRecurso, recurso == null);
        if (cliente == null || recurso == null) {
            notificador.aviso("Selecione o cliente e o recurso da reserva (campos destacados em vermelho).");
            return;
        }
        try {
            Reserva nova = reservaService.criar(cliente.getCpf(), recurso.getId(),
                    campoInicio.getValor(), campoFim.getValor());
            notificador.sucesso("Reserva #" + nova.getId() + " confirmada!\n\n"
                    + "Cliente: " + cliente.getNome() + "\n"
                    + "Recurso: " + recurso.getNome() + "\n"
                    + "Período: " + Formatos.dataHora(nova.getInicio()) + " até " + Formatos.dataHora(nova.getFim()));
            recarregarTabela();
            limparFormulario();
        } catch (SistemaReservasException | IllegalArgumentException e) {
            notificador.erro(e);
        }
    }

    private void cancelarSelecionada() {
        Reserva reserva = reservaSelecionada();
        if (reserva == null) {
            return;
        }
        if (!notificador.confirmar("Cancelar reserva", "Deseja cancelar a reserva abaixo?\n\n"
                + descrever(reserva) + "\n\nO período ficará disponível para novas reservas.")) {
            return;
        }
        try {
            reservaService.cancelar(reserva.getId());
            notificador.sucesso("Reserva #" + reserva.getId() + " cancelada. "
                    + reserva.getRecurso().getNome() + " está liberado(a) neste período.");
            recarregarTabela();
        } catch (SistemaReservasException | IllegalArgumentException e) {
            notificador.erro(e);
        }
    }

    private void concluirSelecionada() {
        Reserva reserva = reservaSelecionada();
        if (reserva == null) {
            return;
        }
        if (!notificador.confirmar("Concluir reserva", "Marcar a reserva abaixo como concluída (utilizada)?\n\n"
                + descrever(reserva))) {
            return;
        }
        try {
            reservaService.concluir(reserva.getId());
            notificador.sucesso("Reserva #" + reserva.getId() + " marcada como concluída.");
            recarregarTabela();
        } catch (SistemaReservasException | IllegalArgumentException e) {
            notificador.erro(e);
        }
    }

    /** Consulta no serviço a reserva da linha selecionada (ou avisa se não houver seleção). */
    private Reserva reservaSelecionada() {
        Object id = tabela.getValorSelecionado(COLUNA_ID);
        if (id == null) {
            notificador.aviso("Selecione uma reserva na tabela.");
            return null;
        }
        try {
            return reservaService.buscarPorId((Integer) id);
        } catch (SistemaReservasException e) {
            notificador.erro(e);
            return null;
        }
    }

    // ==================================================================
    // Carga de dados
    // ==================================================================

    /** Recarrega a tabela aplicando os filtros: cada filtro usa a consulta correspondente do serviço. */
    private void recarregarTabela() {
        Cliente cliente = selecionado(filtroCliente);
        Recurso recurso = selecionado(filtroRecurso);
        StatusReserva status = selecionado(filtroStatus);
        List<Reserva> reservas = new ArrayList<>(reservaService.listar());
        int totalGeral = reservas.size();
        try {
            // Filtros combinados = interseção dos resultados de cada consulta.
            if (cliente != null) {
                reservas.retainAll(reservaService.listarPorCliente(cliente.getCpf()));
            }
            if (recurso != null) {
                reservas.retainAll(reservaService.listarPorRecurso(recurso.getId()));
            }
            if (status != null) {
                reservas.retainAll(reservaService.listarPorStatus(status));
            }
        } catch (SistemaReservasException e) {
            notificador.erro(e);
            limparFiltros();
            return;
        }

        List<Object[]> linhas = new ArrayList<>();
        for (Reserva reserva : reservas) {
            linhas.add(new Object[] {
                reserva.getId(),
                reserva.getCliente().getNome(),
                reserva.getRecurso().getNome(),
                new Periodo(reserva.getInicio(), reserva.getFim()),
                reserva.getStatus()
            });
        }
        boolean filtrando = cliente != null || recurso != null || status != null;
        tabela.setMensagemSemRegistros(filtrando
                ? "Nenhuma reserva corresponde aos filtros selecionados."
                : "Nenhuma reserva registrada. Use o formulário ao lado para criar a primeira.");
        tabela.exibir(linhas);

        String texto = totalGeral == 1 ? "1 reserva" : totalGeral + " reservas";
        rotuloTotal.setText(filtrando ? "Exibindo " + reservas.size() + " de " + texto : texto);
        atualizarBotoesDeAcao();
    }

    /** Recarrega clientes e recursos cadastrados, mantendo a escolha atual quando ainda existir. */
    private void recarregarCombos() {
        recarregandoCombos = true;
        try {
            List<Cliente> clientes = clienteService.listar();
            List<Recurso> recursos = recursoService.listar();
            preencherCombo(comboCliente, clientes);
            preencherCombo(comboRecurso, recursos);
            preencherCombo(filtroCliente, clientes);
            preencherCombo(filtroRecurso, recursos);
        } finally {
            recarregandoCombos = false;
        }
    }

    /** O primeiro item é sempre null ("Selecione..." / "Todos..."). */
    private static <T> void preencherCombo(JComboBox<T> combo, List<T> itens) {
        Object anterior = combo.getSelectedItem();
        DefaultComboBoxModel<T> modelo = new DefaultComboBoxModel<>();
        modelo.addElement(null);
        for (T item : itens) {
            modelo.addElement(item);
        }
        // As listas do back-end são imutáveis e não aceitam contains(null): testar null antes.
        boolean manterAnterior = anterior != null && itens.contains(anterior);
        modelo.setSelectedItem(manterAnterior ? anterior : null);
        combo.setModel(modelo);
    }

    // ==================================================================
    // Estado da tela
    // ==================================================================

    private void limparFormulario() {
        comboCliente.setSelectedItem(null);
        comboRecurso.setSelectedItem(null);
        ComponentesUI.marcarErroCombo(comboCliente, false);
        ComponentesUI.marcarErroCombo(comboRecurso, false);
        // Sugestão inicial: próxima hora cheia, com duração de 1 hora.
        LocalDateTime proximaHora = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS).plusHours(1);
        campoInicio.setValor(proximaHora);
        campoFim.setValor(proximaHora.plusHours(1));
        limparDisponibilidade();
    }

    private void limparFiltros() {
        recarregandoCombos = true;
        filtroCliente.setSelectedItem(null);
        filtroRecurso.setSelectedItem(null);
        filtroStatus.setSelectedItem(null);
        recarregandoCombos = false;
        recarregarTabela();
    }

    private void exibirDisponibilidade(boolean disponivel, String mensagem) {
        rotuloDisponibilidade.setText(mensagem);
        rotuloDisponibilidade.setForeground(disponivel ? Tema.STATUS_ATIVA_TEXTO : Tema.STATUS_CANCELADA_TEXTO);
        rotuloDisponibilidade.setBackground(disponivel ? Tema.STATUS_ATIVA_FUNDO : Tema.STATUS_CANCELADA_FUNDO);
        rotuloDisponibilidade.setVisible(true);
    }

    private void limparDisponibilidade() {
        rotuloDisponibilidade.setText("");
        rotuloDisponibilidade.setVisible(false);
    }

    /**
     * Cancelar/Concluir só ficam ativos para reservas ATIVAS. A tela consulta
     * isAtiva() da própria entidade; a regra continua garantida pelo back-end.
     */
    private void atualizarBotoesDeAcao() {
        Object id = tabela.getValorSelecionado(COLUNA_ID);
        boolean ativa = false;
        if (id != null) {
            try {
                ativa = reservaService.buscarPorId((Integer) id).isAtiva();
            } catch (SistemaReservasException e) {
                ativa = false;
            }
        }
        botaoCancelar.setEnabled(ativa);
        botaoConcluir.setEnabled(ativa);
    }

    // ==================================================================
    // Textos de exibição
    // ==================================================================

    @SuppressWarnings("unchecked")
    private static <T> T selecionado(JComboBox<T> combo) {
        return (T) combo.getSelectedItem();
    }

    private static String descreverCliente(Cliente cliente) {
        return cliente.getNome() + " — " + Formatos.cpf(cliente.getCpf());
    }

    private static String descreverRecurso(Recurso recurso) {
        return recurso.getNome() + " (ID " + recurso.getId() + ")";
    }

    private static String descrever(Reserva reserva) {
        return "Reserva #" + reserva.getId() + "\n"
                + "Cliente: " + reserva.getCliente().getNome() + "\n"
                + "Recurso: " + reserva.getRecurso().getNome() + "\n"
                + "Período: " + Formatos.dataHora(reserva.getInicio()) + " até " + Formatos.dataHora(reserva.getFim());
    }
}
