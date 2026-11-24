package view.componentes;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 * Formulário em coluna única montado com GridBagLayout.
 *
 * Cada campo ocupa três linhas: rótulo, componente de entrada e dica de
 * formato (opcional). Os módulos apenas chamam adicionarCampo(...), sem
 * repetir a configuração do GridBagConstraints.
 */
public class FormularioPainel extends JPanel {

    private static final long serialVersionUID = 1L;

    private int proximaLinha = 0;

    public FormularioPainel() {
        super(new GridBagLayout());
        setOpaque(false);
    }

    public void adicionarCampo(String rotulo, boolean obrigatorio, JComponent campo, String dica) {
        adicionar(ComponentesUI.rotuloCampo(rotulo, obrigatorio), 0, 4);
        adicionar(campo, 0, dica == null ? 12 : 3);
        if (dica != null) {
            JLabel textoDica = ComponentesUI.textoSecundario(dica);
            adicionar(textoDica, 0, 12);
        }
    }

    /** Adiciona um componente ocupando a largura toda (ex.: linha de botões). */
    public void adicionarComponente(JComponent componente, int espacoAbaixo) {
        adicionar(componente, 0, espacoAbaixo);
    }

    /** Ocupa o espaço vertical restante, mantendo os campos alinhados ao topo. */
    public void adicionarEspacoFlexivel() {
        GridBagConstraints restricoes = restricoesBase(0);
        restricoes.weighty = 1;
        JPanel vazio = new JPanel();
        vazio.setOpaque(false);
        add(vazio, restricoes);
    }

    private void adicionar(JComponent componente, int espacoAcima, int espacoAbaixo) {
        add(componente, restricoesBase(espacoAcima, espacoAbaixo));
    }

    private GridBagConstraints restricoesBase(int espacoAbaixo) {
        return restricoesBase(0, espacoAbaixo);
    }

    private GridBagConstraints restricoesBase(int espacoAcima, int espacoAbaixo) {
        GridBagConstraints restricoes = new GridBagConstraints();
        restricoes.gridx = 0;
        restricoes.gridy = proximaLinha++;
        restricoes.weightx = 1;
        restricoes.fill = GridBagConstraints.HORIZONTAL;
        restricoes.anchor = GridBagConstraints.NORTHWEST;
        restricoes.insets = new Insets(espacoAcima, 0, espacoAbaixo, 0);
        return restricoes;
    }
}
