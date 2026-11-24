package view.reserva;

import model.StatusReserva;
import view.componentes.RenderizadorCelula;
import view.componentes.Tema;

import javax.swing.JTable;
import java.awt.Color;
import java.awt.Component;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/**
 * Exibe o status da reserva como um "selo" colorido:
 *   Ativa = verde, Cancelada = vermelho, Concluída = cinza.
 *
 * Herda de RenderizadorCelula (mantém fundo, zebra e seleção da linha) e
 * sobrescreve o desenho para acrescentar o selo — exemplo de herança com
 * especialização de comportamento.
 */
public class StatusReservaRenderer extends RenderizadorCelula {

    private static final long serialVersionUID = 1L;

    private static final int ALTURA_SELO = 22;
    private static final int MARGEM_TEXTO = 10;

    private Color corTexto = Tema.TEXTO;
    private Color corFundoSelo;

    @Override
    public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                                                   boolean comFoco, int linha, int coluna) {
        super.getTableCellRendererComponent(tabela, valor, selecionada, comFoco, linha, coluna);
        if (valor instanceof StatusReserva) {
            StatusReserva status = (StatusReserva) valor;
            setText(status.getDescricao());
            definirCores(status);
        } else {
            corFundoSelo = null;
        }
        return this;
    }

    private void definirCores(StatusReserva status) {
        switch (status) {
            case ATIVA:
                corTexto = Tema.STATUS_ATIVA_TEXTO;
                corFundoSelo = Tema.STATUS_ATIVA_FUNDO;
                break;
            case CANCELADA:
                corTexto = Tema.STATUS_CANCELADA_TEXTO;
                corFundoSelo = Tema.STATUS_CANCELADA_FUNDO;
                break;
            default:
                corTexto = Tema.STATUS_CONCLUIDA_TEXTO;
                corFundoSelo = Tema.STATUS_CONCLUIDA_FUNDO;
                break;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (corFundoSelo == null) {
            super.paintComponent(g);
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        g2.setColor(getBackground()); // fundo da linha (zebra/seleção)
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setFont(Tema.FONTE_ROTULO);
        FontMetrics medidas = g2.getFontMetrics();
        int larguraSelo = medidas.stringWidth(getText()) + 2 * MARGEM_TEXTO;
        int x = getInsets().left;
        int y = (getHeight() - ALTURA_SELO) / 2;

        g2.setColor(corFundoSelo);
        g2.fillRoundRect(x, y, larguraSelo, ALTURA_SELO, ALTURA_SELO, ALTURA_SELO);

        g2.setColor(corTexto);
        int linhaBase = y + (ALTURA_SELO - medidas.getHeight()) / 2 + medidas.getAscent();
        g2.drawString(getText(), x + MARGEM_TEXTO, linhaBase);
        g2.dispose();
    }
}
