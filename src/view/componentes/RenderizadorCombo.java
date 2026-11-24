package view.componentes;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import java.awt.Component;
import java.util.function.Function;

/**
 * Define como cada objeto aparece em um JComboBox.
 *
 * Os combos guardam os próprios objetos do model (Cliente, Recurso,
 * StatusReserva) — assim a tela obtém o objeto escolhido diretamente,
 * sem procurar por texto. Este renderizador apenas decide o TEXTO exibido,
 * sem depender do toString() das entidades (que é voltado a depuração).
 *
 * O item null representa "nenhuma escolha" e mostra o texto de orientação.
 *
 * @param <T> tipo dos itens do combo
 */
public class RenderizadorCombo<T> extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L;

    private final transient Function<T, String> descricao;
    private final String textoNenhum;

    public RenderizadorCombo(Function<T, String> descricao, String textoNenhum) {
        this.descricao = descricao;
        this.textoNenhum = textoNenhum;
    }

    @Override
    @SuppressWarnings("unchecked") // o combo que usa este renderizador só contém itens do tipo T
    public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                  boolean selecionado, boolean comFoco) {
        String texto = (valor == null) ? textoNenhum : descricao.apply((T) valor);
        super.getListCellRendererComponent(lista, texto, indice, selecionado, comFoco);
        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        setFont(Tema.FONTE_TEXTO);
        if (valor == null && !selecionado) {
            setForeground(Tema.TEXTO_SECUNDARIO);
        }
        return this;
    }
}
