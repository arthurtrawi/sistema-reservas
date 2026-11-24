package view;

import service.ClienteService;
import service.RecursoService;
import service.ReservaService;
import util.Validador;
import view.cliente.ClientePainel;
import view.inicio.InicioPainel;
import view.recurso.RecursoPainel;
import view.reserva.ReservaPainel;
import view.componentes.MenuLateral;
import view.componentes.Notificador;
import view.componentes.NotificadorDialogo;
import view.componentes.PainelCabecalho;
import view.componentes.PainelRodape;
import view.componentes.Tema;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.EnumMap;
import java.util.Map;

/**
 * Janela principal: organiza menu lateral, cabeçalho, área de conteúdo e
 * rodapé, e controla a navegação entre os módulos.
 *
 * Layout (BorderLayout):
 *   WEST   -> MenuLateral
 *   CENTER -> cabeçalho (NORTH) + área de conteúdo com CardLayout (CENTER) + rodapé (SOUTH)
 *
 * A janela recebe os serviços prontos (injeção de dependência pelo
 * construtor) e os repassa aos painéis. Ela não acessa repositórios.
 */
public class JanelaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final ClienteService clienteService;
    private final RecursoService recursoService;
    private final ReservaService reservaService;
    private final Notificador notificador;

    private final CardLayout cartoes = new CardLayout();
    private final JPanel areaConteudo = new JPanel(cartoes);
    private final Map<Modulo, JComponent> paineis = new EnumMap<>(Modulo.class);

    private final MenuLateral menu = new MenuLateral("Reservas", "Sistema de gestão");
    private final PainelCabecalho cabecalho = new PainelCabecalho();

    private Modulo moduloAtual;

    /** Construtor usado pela aplicação: mensagens em caixas de diálogo. */
    public JanelaPrincipal(ClienteService clienteService, RecursoService recursoService,
                           ReservaService reservaService) {
        this(clienteService, recursoService, reservaService, null);
    }

    /**
     * Construtor que permite trocar a forma de exibir mensagens
     * (usado nos testes automáticos). Se notificador for null, usa diálogos.
     */
    public JanelaPrincipal(ClienteService clienteService, RecursoService recursoService,
                           ReservaService reservaService, Notificador notificador) {
        super("Sistema de Reservas");
        this.clienteService = Validador.exigirNaoNulo(clienteService, "clienteService");
        this.recursoService = Validador.exigirNaoNulo(recursoService, "recursoService");
        this.reservaService = Validador.exigirNaoNulo(reservaService, "reservaService");
        this.notificador = (notificador != null) ? notificador : new NotificadorDialogo(this);

        montarLayout();
        criarPaineis();
        configurarMenu();
        configurarFechamento();

        setMinimumSize(new Dimension(1100, 700));
        setSize(1280, 780);
        setLocationRelativeTo(null); // centraliza na tela

        navegarPara(Modulo.INICIO);
    }

    /**
     * Exibe o módulo informado, atualiza cabeçalho e menu e pede ao painel
     * que recarregue seus dados do back-end.
     */
    public void navegarPara(Modulo modulo) {
        moduloAtual = modulo;
        cartoes.show(areaConteudo, modulo.name());
        cabecalho.exibirSecao(modulo.getTitulo(), modulo.getDescricao());
        menu.marcarAtivo(modulo.name());

        JComponent painel = paineis.get(modulo);
        if (painel instanceof Atualizavel) {
            ((Atualizavel) painel).atualizar();
        }
    }

    public Modulo getModuloAtual() {
        return moduloAtual;
    }

    // ------------------------------------------------------------------

    private void montarLayout() {
        areaConteudo.setBackground(Tema.FUNDO);
        areaConteudo.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JPanel colunaDireita = new JPanel(new BorderLayout());
        colunaDireita.add(cabecalho, BorderLayout.NORTH);
        colunaDireita.add(areaConteudo, BorderLayout.CENTER);
        colunaDireita.add(new PainelRodape(
                "Sistema de Reservas · Protótipo acadêmico · Programação Orientada a Objetos I",
                "Versão 1.0"), BorderLayout.SOUTH);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.add(menu, BorderLayout.WEST);
        raiz.add(colunaDireita, BorderLayout.CENTER);
        setContentPane(raiz);
    }

    /** Cria um painel por módulo e registra no CardLayout. */
    private void criarPaineis() {
        registrarPainel(Modulo.INICIO,
                new InicioPainel(clienteService, recursoService, reservaService, notificador, this::navegarPara));
        registrarPainel(Modulo.CLIENTES, new ClientePainel(clienteService, notificador));
        registrarPainel(Modulo.RECURSOS, new RecursoPainel(recursoService, notificador));
        registrarPainel(Modulo.RESERVAS,
                new ReservaPainel(reservaService, clienteService, recursoService, notificador));
    }

    private void registrarPainel(Modulo modulo, JComponent painel) {
        paineis.put(modulo, painel);
        areaConteudo.add(painel, modulo.name());
    }

    private void configurarMenu() {
        for (Modulo modulo : Modulo.values()) {
            menu.adicionarItem(modulo.name(), modulo.getTitulo(), evento -> navegarPara(modulo));
        }
        menu.adicionarItemRodape("SAIR", "Sair", evento -> sair());
    }

    /** O "X" da janela passa pela mesma confirmação do botão Sair. */
    private void configurarFechamento() {
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                sair();
            }
        });
    }

    private void sair() {
        if (notificador.confirmar("Sair", "Deseja realmente sair do sistema?\n"
                + "Os dados ficam apenas em memória e serão perdidos.")) {
            dispose();
            System.exit(0);
        }
    }
}
