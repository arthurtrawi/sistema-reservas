package view.componentes;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

/** Rodapé com o nome do sistema e a versão do protótipo. */
public class PainelRodape extends JPanel {

    private static final long serialVersionUID = 1L;

    public PainelRodape(String textoEsquerda, String textoDireita) {
        super(new BorderLayout());
        setBackground(Tema.SUPERFICIE);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Tema.BORDA),
                BorderFactory.createEmptyBorder(8, 24, 8, 24)));
        add(ComponentesUI.textoSecundario(textoEsquerda), BorderLayout.WEST);
        add(ComponentesUI.textoSecundario(textoDireita), BorderLayout.EAST);
    }
}
