package view.componentes;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Color;

/**
 * Cartão de indicador numérico do painel Início: título, valor em destaque
 * e uma descrição curta, com uma faixa colorida no topo.
 */
public class CartaoIndicador extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel valor = new JLabel("0");

    public CartaoIndicador(String titulo, String descricao, Color cor) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(Tema.SUPERFICIE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(4, 0, 0, 0, cor),       // faixa de destaque
                        BorderFactory.createMatteBorder(0, 1, 1, 1, Tema.BORDA)),
                BorderFactory.createEmptyBorder(16, 20, 18, 20)));

        valor.setFont(Tema.FONTE_INDICADOR);
        valor.setForeground(cor);

        add(ComponentesUI.rotuloCampo(titulo, false));
        add(Box.createVerticalStrut(6));
        add(valor);
        add(Box.createVerticalStrut(2));
        add(ComponentesUI.textoSecundario(descricao));
    }

    public void setValor(int numero) {
        valor.setText(String.valueOf(numero));
    }

    public String getValorExibido() {
        return valor.getText();
    }
}
