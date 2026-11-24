package view.componentes;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.plaf.basic.BasicArrowButton;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;

/**
 * Fábrica de componentes visuais padronizados.
 *
 * Em vez de cada tela configurar fontes, cores e bordas de seus rótulos e
 * campos, todas pedem os componentes a esta classe. Isso elimina duplicação
 * e garante que o sistema inteiro tenha a mesma aparência.
 */
public final class ComponentesUI {

    private static final Border BORDA_CAMPO_NORMAL = criarBordaCampo(Tema.BORDA_CAMPO);
    private static final Border BORDA_CAMPO_ERRO = criarBordaCampo(Tema.PERIGO);
    private static final Border BORDA_COMBO_NORMAL = BorderFactory.createLineBorder(Tema.BORDA_CAMPO);
    private static final Border BORDA_COMBO_ERRO = BorderFactory.createLineBorder(Tema.PERIGO);

    private ComponentesUI() {
        // Não instanciável.
    }

    // ---- Textos ---------------------------------------------------------

    public static JLabel titulo(String texto) {
        return rotuloCom(texto, Tema.FONTE_TITULO, Tema.TEXTO);
    }

    public static JLabel tituloSecao(String texto) {
        return rotuloCom(texto, Tema.FONTE_SECAO, Tema.TEXTO);
    }

    public static JLabel texto(String texto) {
        return rotuloCom(texto, Tema.FONTE_TEXTO, Tema.TEXTO);
    }

    public static JLabel textoSecundario(String texto) {
        return rotuloCom(texto, Tema.FONTE_PEQUENA, Tema.TEXTO_SECUNDARIO);
    }

    /**
     * Parágrafo de texto com quebra de linha automática (JTextArea somente
     * leitura, sem borda e transparente), para textos explicativos longos.
     */
    public static JTextArea paragrafo(String texto, boolean secundario) {
        JTextArea area = new JTextArea(texto);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setFocusable(false);
        area.setOpaque(false);
        area.setBorder(null);
        area.setFont(secundario ? Tema.FONTE_PEQUENA : Tema.FONTE_TEXTO);
        area.setForeground(secundario ? Tema.TEXTO_SECUNDARIO : Tema.TEXTO);
        return area;
    }

    /** Rótulo de campo de formulário; obrigatórios recebem " *". */
    public static JLabel rotuloCampo(String texto, boolean obrigatorio) {
        return rotuloCom(obrigatorio ? texto + " *" : texto, Tema.FONTE_ROTULO, Tema.TEXTO);
    }

    private static JLabel rotuloCom(String texto, java.awt.Font fonte, Color cor) {
        JLabel rotulo = new JLabel(texto);
        rotulo.setFont(fonte);
        rotulo.setForeground(cor);
        return rotulo;
    }

    // ---- Campos ---------------------------------------------------------

    public static JTextField campoTexto() {
        JTextField campo = new JTextField(18);
        campo.setFont(Tema.FONTE_TEXTO);
        campo.setForeground(Tema.TEXTO);
        campo.setBorder(BORDA_CAMPO_NORMAL);
        // Ao digitar, remove o destaque de erro que possa ter sido aplicado.
        aoAlterarTexto(campo, () -> marcarErro(campo, false));
        return campo;
    }

    /** Executa a ação sempre que o texto do campo mudar (DocumentListener). */
    public static void aoAlterarTexto(JTextField campo, Runnable acao) {
        campo.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                acao.run();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                acao.run();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                acao.run();
            }
        });
    }

    /**
     * Verificação de USABILIDADE: destaca os campos obrigatórios deixados em
     * branco e coloca o foco no primeiro deles. Não valida formato nem regras
     * (CPF, e-mail, duplicidade...) — isso continua sendo feito pelo back-end.
     *
     * @return true se todos os campos informados estão preenchidos
     */
    public static boolean verificarPreenchidos(JTextField... campos) {
        JTextField primeiroVazio = null;
        for (JTextField campo : campos) {
            boolean vazio = campo.getText().isBlank();
            marcarErro(campo, vazio);
            if (vazio && primeiroVazio == null) {
                primeiroVazio = campo;
            }
        }
        if (primeiroVazio != null) {
            primeiroVazio.requestFocusInWindow();
            return false;
        }
        return true;
    }

    /** JComboBox padronizado (fundo branco, borda igual à dos campos de texto). */
    public static <T> JComboBox<T> combo() {
        JComboBox<T> combo = new JComboBox<>();
        combo.setUI(new ComboPlanoUI());
        combo.setFont(Tema.FONTE_TEXTO);
        combo.setBackground(Tema.SUPERFICIE);
        combo.setBorder(BORDA_COMBO_NORMAL);
        return combo;
    }

    /**
     * Aparência plana para JComboBox: substitui o botão de seta com degradê
     * do Metal por uma seta simples sobre fundo branco.
     */
    private static class ComboPlanoUI extends BasicComboBoxUI {
        @Override
        protected javax.swing.JButton createArrowButton() {
            BasicArrowButton seta = new BasicArrowButton(SwingConstants.SOUTH,
                    Tema.SUPERFICIE, Tema.SUPERFICIE, Tema.TEXTO_SECUNDARIO, Tema.SUPERFICIE);
            seta.setBorder(BorderFactory.createEmptyBorder());
            seta.setName("ComboBox.arrowButton");
            return seta;
        }
    }

    /** Destaque vermelho para combo obrigatório sem seleção. */
    public static void marcarErroCombo(JComboBox<?> combo, boolean comErro) {
        combo.setBorder(comErro ? BORDA_COMBO_ERRO : BORDA_COMBO_NORMAL);
    }

    /** Aplica (ou remove) o destaque vermelho de campo obrigatório não preenchido. */
    public static void marcarErro(JComponent campo, boolean comErro) {
        campo.setBorder(comErro ? BORDA_CAMPO_ERRO : BORDA_CAMPO_NORMAL);
    }

    private static Border criarBordaCampo(Color cor) {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor, 1),
                BorderFactory.createEmptyBorder(6, 8, 6, 8));
    }

    // ---- Contêineres ----------------------------------------------------

    /**
     * Cartão branco com borda suave e título, usado para agrupar
     * formulários, tabelas e indicadores.
     */
    public static JPanel cartao(String titulo, JComponent conteudo) {
        JPanel cartao = new JPanel(new BorderLayout(0, Tema.ESPACO_PEQUENO + 4));
        cartao.setBackground(Tema.SUPERFICIE);
        cartao.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Tema.BORDA, 1),
                BorderFactory.createEmptyBorder(Tema.ESPACO, Tema.ESPACO, Tema.ESPACO, Tema.ESPACO)));
        if (titulo != null) {
            cartao.add(tituloSecao(titulo), BorderLayout.NORTH);
        }
        conteudo.setOpaque(false); // deixa o fundo branco do cartão aparecer
        cartao.add(conteudo, BorderLayout.CENTER);
        return cartao;
    }

    /** Linha de botões com larguras iguais. */
    public static JPanel linhaBotoes(JButton... botoes) {
        JPanel linha = painelTransparente(new GridLayout(1, botoes.length, Tema.ESPACO_PEQUENO, 0));
        for (JButton botao : botoes) {
            linha.add(botao);
        }
        return linha;
    }

    /** Painel transparente, para organizar componentes dentro de cartões. */
    public static JPanel painelTransparente(java.awt.LayoutManager layout) {
        JPanel painel = new JPanel(layout);
        painel.setOpaque(false);
        return painel;
    }
}
