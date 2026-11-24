package view.componentes;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Botão padronizado do sistema, com cantos arredondados e efeito de
 * passar o mouse / pressionar.
 *
 * Herda de JButton e sobrescreve paintComponent (polimorfismo): continua
 * sendo um JButton para o Swing (aceita ActionListener, atalhos, foco...),
 * só muda a forma de desenhar o fundo.
 */
public class Botao extends JButton {

    private static final long serialVersionUID = 1L;

    private static final int RAIO = 10;

    /** Variações visuais disponíveis, cada uma com suas cores. */
    public enum Estilo {
        PRIMARIO(Tema.PRIMARIA, Tema.PRIMARIA_HOVER, Color.WHITE, null),
        SECUNDARIO(Tema.SUPERFICIE, Tema.SECUNDARIA_HOVER, Tema.TEXTO, Tema.BORDA_CAMPO),
        PERIGO(Tema.PERIGO, Tema.PERIGO_HOVER, Color.WHITE, null);

        private final Color fundo;
        private final Color fundoHover;
        private final Color texto;
        private final Color borda;

        Estilo(Color fundo, Color fundoHover, Color texto, Color borda) {
            this.fundo = fundo;
            this.fundoHover = fundoHover;
            this.texto = texto;
            this.borda = borda;
        }
    }

    private final Estilo estilo;

    public Botao(String texto, Estilo estilo) {
        super(texto);
        this.estilo = estilo;
        setFont(Tema.FONTE_BOTAO);
        setForeground(estilo.texto);
        setBorder(new EmptyBorder(9, 18, 9, 18));
        setContentAreaFilled(false); // o fundo é desenhado em paintComponent
        setBorderPainted(false);
        setFocusPainted(false);      // o foco é desenhado de forma própria
        setOpaque(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int largura = getWidth() - 1;
        int altura = getHeight() - 1;

        g2.setColor(corDeFundo());
        g2.fillRoundRect(0, 0, largura, altura, RAIO, RAIO);

        if (estilo.borda != null && isEnabled()) {
            g2.setColor(estilo.borda);
            g2.drawRoundRect(0, 0, largura, altura, RAIO, RAIO);
        }
        if (isFocusOwner() && isEnabled()) { // indicação de foco para quem navega pelo teclado
            // Em botões de fundo colorido, um anel azul não apareceria: usa branco translúcido.
            g2.setColor(estilo.borda == null ? new Color(255, 255, 255, 190) : Tema.PRIMARIA);
            g2.drawRoundRect(2, 2, largura - 4, altura - 4, RAIO, RAIO);
        }
        g2.dispose();

        super.paintComponent(g); // desenha o texto (cor de desabilitado vem do Tema)
    }

    private Color corDeFundo() {
        if (!isEnabled()) {
            return Tema.DESABILITADO;
        }
        if (getModel().isPressed()) {
            return estilo.fundoHover.darker();
        }
        if (getModel().isRollover()) {
            return estilo.fundoHover;
        }
        return estilo.fundo;
    }
}
