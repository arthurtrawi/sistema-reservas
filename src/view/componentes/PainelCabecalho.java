package view.componentes;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/**
 * Cabeçalho da área de conteúdo: título da seção atual, descrição breve
 * e um selo com o módulo selecionado.
 */
public class PainelCabecalho extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JLabel titulo = ComponentesUI.titulo("");
    private final JLabel descricao = ComponentesUI.textoSecundario("");
    private final JLabel seloModulo = new JLabel();

    public PainelCabecalho() {
        super(new BorderLayout());
        setBackground(Tema.SUPERFICIE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(18, 24, 18, 24)));

        JPanel textos = new JPanel();
        textos.setLayout(new BoxLayout(textos, BoxLayout.Y_AXIS));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(Box.createVerticalStrut(4));
        textos.add(descricao);
        add(textos, BorderLayout.CENTER);

        seloModulo.setOpaque(true);
        seloModulo.setBackground(Tema.SELECAO);
        seloModulo.setForeground(Tema.PRIMARIA_HOVER);
        seloModulo.setFont(Tema.FONTE_ROTULO);
        seloModulo.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));

        JPanel areaSelo = ComponentesUI.painelTransparente(new java.awt.GridBagLayout()); // centraliza verticalmente
        areaSelo.add(seloModulo);
        add(areaSelo, BorderLayout.EAST);
    }

    /** Atualiza os textos para a seção exibida. */
    public void exibirSecao(String tituloSecao, String descricaoSecao) {
        titulo.setText(tituloSecao);
        descricao.setText(descricaoSecao);
        seloModulo.setText("Módulo: " + tituloSecao);
    }
}
