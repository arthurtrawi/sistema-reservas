package teste;

import model.Recurso;
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
import view.componentes.Notificador;
import view.componentes.Tema;

import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Gera as capturas de tela usadas na documentação (README).
 *
 * Abre o sistema real, carrega os dados de exemplo pelo botão do Início e
 * monta cada cena (seleções, verificação de disponibilidade, filtros,
 * erros), salvando imagens PNG na pasta informada.
 *
 * Requer ambiente gráfico.
 * Execução: java -cp out teste.GeradorCapturas docs/imagens
 */
public class GeradorCapturas {

    private static JanelaPrincipal janela;
    private static ReservaService reservaService;
    private static File pasta;

    public static void main(String[] args) throws Exception {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        pasta = new File(args.length > 0 ? args[0] : "docs/imagens");
        if (!pasta.isDirectory() && !pasta.mkdirs()) {
            throw new IOException("Não foi possível criar a pasta " + pasta);
        }
        try {
            SwingUtilities.invokeAndWait(GeradorCapturas::abrirSistema);
            gerarCenas();
        } finally {
            System.exit(0);
        }
    }

    private static void abrirSistema() {
        ReservaRepository reservaRepository = new ReservaRepository();
        ClienteService clienteService = new ClienteService(new ClienteRepository(), reservaRepository);
        RecursoService recursoService = new RecursoService(new RecursoRepository(), reservaRepository);
        reservaService = new ReservaService(reservaRepository, clienteService, recursoService);

        Tema.aplicar();
        janela = new JanelaPrincipal(clienteService, recursoService, reservaService, new NotificadorSilencioso());
        janela.setSize(1280, 760);
        janela.setVisible(true);
    }

    private static void gerarCenas() throws Exception {
        // 1. Início vazio e após carregar os dados de exemplo.
        capturarJanela("01-inicio-vazio");
        naEdt(() -> clicar("inicio.dadosExemplo"));
        capturarJanela("02-inicio");

        // 2. Clientes: campos obrigatórios destacados e modo de edição.
        naEdt(() -> {
            janela.navegarPara(Modulo.CLIENTES);
            clicar("cliente.cadastrar");
        });
        capturarJanela("03-clientes-validacao");
        naEdt(() -> selecionarLinha("cliente.tabela", 1));
        capturarJanela("04-clientes-edicao");

        // 3. Recursos em edição.
        naEdt(() -> {
            janela.navegarPara(Modulo.RECURSOS);
            selecionarLinha("recurso.tabela", 2);
        });
        capturarJanela("05-recursos");

        // 4. Reservas: verificação de disponibilidade com conflito e linha selecionada.
        LocalDate amanha = LocalDate.now().plusDays(1);
        naEdt(() -> {
            janela.navegarPara(Modulo.RESERVAS);
            combo("reserva.cliente").setSelectedIndex(4);  // Diego Ramos
            combo("reserva.recurso").setSelectedIndex(1);  // Sala 101
            campoData("reserva.inicio").setValor(amanha.atTime(10, 0));
            campoData("reserva.fim").setValor(amanha.atTime(12, 0));
            clicar("reserva.verificar");
            selecionarLinha("reserva.tabela", 1);
        });
        capturarJanela("06-reservas");

        // 5. Reservas filtradas por status.
        naEdt(() -> {
            clicar("reserva.limpar");
            combo("reserva.filtroStatus").setSelectedItem(StatusReserva.ATIVA);
        });
        capturarJanela("07-reservas-filtro");

        // 6. Diálogo de erro com a mensagem real devolvida pelo back-end.
        naEdt(() -> {
            Recurso sala101 = reservaService.buscarPorId(1).getRecurso();
            LocalDateTime inicio = amanha.atTime(10, 0);
            try {
                reservaService.criar("44477700068", sala101.getId(), inicio, inicio.plusHours(2));
            } catch (RuntimeException e) {
                capturarDialogo("08-erro-conflito", Notificador.tituloPara(e), e.getMessage(),
                        JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    // ==================================================================
    // Captura
    // ==================================================================

    private static void capturarJanela(String nome) throws Exception {
        Thread.sleep(300); // deixa a interface terminar de se redesenhar
        naEdt(() -> salvar(janela.getRootPane(), nome));
    }

    private static void capturarDialogo(String nome, String titulo, String mensagem, int tipo) {
        // Barra de título desenhada pelo Swing (e não pelo sistema operacional),
        // para que ela apareça na imagem.
        JDialog.setDefaultLookAndFeelDecorated(true);
        JOptionPane painel = new JOptionPane(mensagem, tipo);
        JDialog dialogo = painel.createDialog(janela, titulo);
        JDialog.setDefaultLookAndFeelDecorated(false);
        dialogo.pack();
        salvar(dialogo.getRootPane(), nome);
        dialogo.dispose();
    }

    private static void salvar(JComponent componente, String nome) {
        BufferedImage imagem = new BufferedImage(componente.getWidth(), componente.getHeight(),
                BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagem.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        componente.paint(g);
        g.dispose();
        File arquivo = new File(pasta, nome + ".png");
        try {
            ImageIO.write(imagem, "png", arquivo);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao gravar " + arquivo, e);
        }
        System.out.println("Gerado: " + arquivo.getPath());
    }

    // ==================================================================
    // Utilitários
    // ==================================================================

    /** Executa na EDT e espera terminar. */
    private static void naEdt(Runnable acao) throws Exception {
        SwingUtilities.invokeAndWait(acao);
    }

    private static void clicar(String nome) {
        ((AbstractButton) procurar(nome)).doClick();
    }

    private static void selecionarLinha(String nome, int linha) {
        ((JTable) procurar(nome)).setRowSelectionInterval(linha, linha);
    }

    private static JComboBox<?> combo(String nome) {
        return (JComboBox<?>) procurar(nome);
    }

    private static CampoDataHora campoData(String nome) {
        return (CampoDataHora) procurar(nome);
    }

    private static Component procurar(String nome) {
        Component encontrado = procurar(janela.getContentPane(), nome);
        if (encontrado == null) {
            throw new IllegalStateException("Componente não encontrado: " + nome);
        }
        return encontrado;
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

    /** Não abre diálogos durante a geração; confirma tudo automaticamente. */
    private static class NotificadorSilencioso implements Notificador {

        @Override
        public void sucesso(String mensagem) {
            // Silencioso.
        }

        @Override
        public void aviso(String mensagem) {
            // Silencioso.
        }

        @Override
        public void erro(String titulo, String mensagem) {
            // Silencioso.
        }

        @Override
        public boolean confirmar(String titulo, String mensagem) {
            return true;
        }
    }
}
