package view.inicio;

import exception.SistemaReservasException;
import model.StatusReserva;
import service.ClienteService;
import service.RecursoService;
import service.ReservaService;
import view.Atualizavel;
import view.Modulo;
import view.componentes.Botao;
import view.componentes.CartaoIndicador;
import view.componentes.ComponentesUI;
import view.componentes.Notificador;
import view.componentes.Tema;

import javax.swing.JComponent;
import javax.swing.JTextArea;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.function.Consumer;

/**
 * Painel Início (dashboard).
 *
 * - Indicadores com dados REAIS dos serviços: clientes, recursos e
 *   reservas ativas. São recalculados sempre que o painel é aberto.
 * - Acesso rápido aos módulos.
 * - Carga de dados de exemplo para demonstração.
 *
 * A navegação é feita por um Consumer<Modulo> recebido da JanelaPrincipal:
 * o painel não conhece a janela, apenas "pede" para ir a um módulo.
 */
public class InicioPainel extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private final ClienteService clienteService;
    private final RecursoService recursoService;
    private final ReservaService reservaService;
    private final Notificador notificador;
    private final transient Consumer<Modulo> navegador;
    private final transient DadosExemplo dadosExemplo;

    private final CartaoIndicador indicadorClientes = new CartaoIndicador(
            "Clientes", "cadastrados no sistema", Tema.PRIMARIA);
    private final CartaoIndicador indicadorRecursos = new CartaoIndicador(
            "Recursos", "disponíveis para reserva", new java.awt.Color(0x7C3AED));
    private final CartaoIndicador indicadorReservas = new CartaoIndicador(
            "Reservas ativas", "com status Ativa", Tema.STATUS_ATIVA_TEXTO);

    private final Botao botaoDadosExemplo = new Botao("Carregar dados de exemplo", Botao.Estilo.SECUNDARIO);
    private final JTextArea textoDadosExemplo = ComponentesUI.paragrafo("", true);

    public InicioPainel(ClienteService clienteService, RecursoService recursoService,
                        ReservaService reservaService, Notificador notificador,
                        Consumer<Modulo> navegador) {
        super(new BorderLayout(0, Tema.ESPACO));
        this.clienteService = clienteService;
        this.recursoService = recursoService;
        this.reservaService = reservaService;
        this.notificador = notificador;
        this.navegador = navegador;
        this.dadosExemplo = new DadosExemplo(clienteService, recursoService, reservaService);
        setOpaque(false);

        indicadorClientes.setName("inicio.clientes");
        indicadorRecursos.setName("inicio.recursos");
        indicadorReservas.setName("inicio.reservas");
        botaoDadosExemplo.setName("inicio.dadosExemplo");

        add(criarIndicadores(), BorderLayout.NORTH);
        add(criarCartoesInferiores(), BorderLayout.CENTER);
        botaoDadosExemplo.addActionListener(evento -> carregarDadosExemplo());
    }

    /** Recalcula os indicadores a partir dos serviços. */
    @Override
    public void atualizar() {
        indicadorClientes.setValor(clienteService.listar().size());
        indicadorRecursos.setValor(recursoService.listar().size());
        indicadorReservas.setValor(reservaService.listarPorStatus(StatusReserva.ATIVA).size());

        boolean podeCarregar = dadosExemplo.podeCarregar();
        botaoDadosExemplo.setEnabled(podeCarregar);
        textoDadosExemplo.setText(podeCarregar
                ? "Cadastra 5 clientes, 6 recursos e 8 reservas pelos próprios serviços do sistema."
                : "Disponível apenas com o sistema vazio, para não duplicar cadastros.");
    }

    // ==================================================================
    // Montagem
    // ==================================================================

    private JComponent criarIndicadores() {
        JPanel linha = ComponentesUI.painelTransparente(new GridLayout(1, 3, Tema.ESPACO, 0));
        linha.add(indicadorClientes);
        linha.add(indicadorRecursos);
        linha.add(indicadorReservas);
        return linha;
    }

    private JComponent criarCartoesInferiores() {
        JPanel linha = ComponentesUI.painelTransparente(new GridLayout(1, 2, Tema.ESPACO, 0));
        linha.add(criarAcessoRapido());
        linha.add(criarCartaoDadosExemplo());

        // Mantém os cartões no topo, com altura natural, em vez de esticá-los.
        JPanel alinhadoAoTopo = ComponentesUI.painelTransparente(new BorderLayout());
        alinhadoAoTopo.add(linha, BorderLayout.NORTH);
        return alinhadoAoTopo;
    }

    private JComponent criarAcessoRapido() {
        JPanel botoes = ComponentesUI.painelTransparente(new GridLayout(3, 1, 0, Tema.ESPACO_PEQUENO));
        botoes.add(botaoNavegacao("Nova reserva", Botao.Estilo.PRIMARIO, Modulo.RESERVAS, "inicio.irReservas"));
        botoes.add(botaoNavegacao("Gerenciar clientes", Botao.Estilo.SECUNDARIO, Modulo.CLIENTES, "inicio.irClientes"));
        botoes.add(botaoNavegacao("Gerenciar recursos", Botao.Estilo.SECUNDARIO, Modulo.RECURSOS, "inicio.irRecursos"));
        return ComponentesUI.cartao("Acesso rápido", botoes);
    }

    private JComponent criarCartaoDadosExemplo() {
        JPanel conteudo = ComponentesUI.painelTransparente(new BorderLayout(0, Tema.ESPACO));
        conteudo.add(ComponentesUI.paragrafo("Os dados ficam apenas em memória e são perdidos ao fechar "
                + "o sistema. Para uma demonstração rápida, carregue um conjunto de exemplo.", false),
                BorderLayout.NORTH);
        conteudo.add(textoDadosExemplo, BorderLayout.CENTER);
        conteudo.add(ComponentesUI.linhaBotoes(botaoDadosExemplo), BorderLayout.SOUTH);
        return ComponentesUI.cartao("Dados de exemplo", conteudo);
    }

    private Botao botaoNavegacao(String texto, Botao.Estilo estilo, Modulo destino, String nome) {
        Botao botao = new Botao(texto, estilo);
        botao.setName(nome);
        botao.addActionListener(evento -> navegador.accept(destino));
        return botao;
    }

    // ==================================================================
    // Ações
    // ==================================================================

    private void carregarDadosExemplo() {
        if (!notificador.confirmar("Dados de exemplo",
                "Carregar 5 clientes, 6 recursos e 8 reservas de exemplo?")) {
            return;
        }
        try {
            dadosExemplo.carregar();
            atualizar();
            notificador.sucesso("Dados de exemplo carregados: "
                    + clienteService.listar().size() + " clientes, "
                    + recursoService.listar().size() + " recursos e "
                    + reservaService.listar().size() + " reservas.");
        } catch (SistemaReservasException | IllegalArgumentException e) {
            notificador.erro(e);
            atualizar();
        }
    }
}
