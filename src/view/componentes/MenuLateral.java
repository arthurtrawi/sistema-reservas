package view.componentes;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.event.ActionListener;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Menu de navegação vertical, fixo à esquerda da janela.
 *
 * É genérico: não conhece os módulos do sistema. Quem o usa adiciona itens
 * com um identificador, um rótulo e uma ação (ActionListener), e informa
 * qual item está ativo. Isso permite reutilizá-lo em qualquer janela.
 */
public class MenuLateral extends JPanel {

    private static final long serialVersionUID = 1L;

    private final JPanel areaItens;
    private final JPanel areaRodape;
    private final Map<String, ItemMenu> itens = new LinkedHashMap<>();

    public MenuLateral(String nomeSistema, String subtitulo) {
        super(new BorderLayout());
        setBackground(Tema.MENU_FUNDO);
        setPreferredSize(new Dimension(Tema.LARGURA_MENU, 0));
        setBorder(BorderFactory.createEmptyBorder(24, 12, 16, 12));

        add(criarIdentificacao(nomeSistema, subtitulo), BorderLayout.NORTH);

        areaItens = criarColuna();
        JPanel alinhadoAoTopo = new JPanel(new BorderLayout());
        alinhadoAoTopo.setOpaque(false);
        alinhadoAoTopo.add(areaItens, BorderLayout.NORTH);
        add(alinhadoAoTopo, BorderLayout.CENTER);

        areaRodape = criarColuna();
        add(areaRodape, BorderLayout.SOUTH);
    }

    /** Adiciona um item de navegação. */
    public void adicionarItem(String id, String rotulo, ActionListener acao) {
        ItemMenu item = new ItemMenu(rotulo);
        item.setName("menu." + id); // identifica o componente nos testes automáticos
        item.addActionListener(acao);
        itens.put(id, item);
        areaItens.add(item);
        areaItens.add(Box.createVerticalStrut(4));
    }

    /** Adiciona um item fixo na parte de baixo do menu (ex.: Sair). */
    public void adicionarItemRodape(String id, String rotulo, ActionListener acao) {
        ItemMenu item = new ItemMenu(rotulo);
        item.setName("menu." + id);
        item.addActionListener(acao);
        areaRodape.add(item);
    }

    /** Destaca o item informado e remove o destaque dos demais. */
    public void marcarAtivo(String id) {
        for (Map.Entry<String, ItemMenu> entrada : itens.entrySet()) {
            entrada.getValue().setAtivo(entrada.getKey().equals(id));
        }
    }

    private JPanel criarIdentificacao(String nomeSistema, String subtitulo) {
        JPanel painel = criarColuna();
        painel.setBorder(BorderFactory.createEmptyBorder(0, 10, 28, 0));

        JLabel nome = new JLabel(nomeSistema);
        nome.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        nome.setForeground(Color.WHITE);

        JLabel sub = new JLabel(subtitulo);
        sub.setFont(Tema.FONTE_PEQUENA);
        sub.setForeground(Tema.MENU_TEXTO);

        painel.add(nome);
        painel.add(Box.createVerticalStrut(4));
        painel.add(sub);
        return painel;
    }

    private static JPanel criarColuna() {
        JPanel coluna = new JPanel();
        coluna.setLayout(new BoxLayout(coluna, BoxLayout.Y_AXIS));
        coluna.setOpaque(false);
        return coluna;
    }

    /**
     * Botão do menu: fundo transparente, destaque ao passar o mouse e,
     * quando ativo, fundo mais claro com uma barra azul à esquerda.
     */
    private static class ItemMenu extends JButton {

        private static final long serialVersionUID = 1L;

        private static final int ALTURA = 42;
        private static final int LARGURA_BARRA = 4;

        private boolean ativo;

        ItemMenu(String rotulo) {
            super(rotulo);
            setFont(Tema.FONTE_MENU);
            setForeground(Tema.MENU_TEXTO);
            setHorizontalAlignment(LEFT);
            setAlignmentX(Component.LEFT_ALIGNMENT);
            setMaximumSize(new Dimension(Integer.MAX_VALUE, ALTURA));
            setPreferredSize(new Dimension(Tema.LARGURA_MENU - 24, ALTURA));
            setBorder(BorderFactory.createEmptyBorder(0, 18, 0, 10));
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        void setAtivo(boolean ativo) {
            this.ativo = ativo;
            setForeground(ativo ? Color.WHITE : Tema.MENU_TEXTO);
            setFont(ativo ? Tema.FONTE_MENU.deriveFont(Font.BOLD) : Tema.FONTE_MENU);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (ativo || getModel().isRollover()) {
                g.setColor(Tema.MENU_HOVER);
                g.fillRect(0, 0, getWidth(), getHeight());
            } else if (isFocusOwner()) { // foco do teclado: só um contorno, para não parecer selecionado
                g.setColor(Tema.MENU_HOVER);
                g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            }
            if (ativo) {
                g.setColor(Tema.PRIMARIA);
                g.fillRect(0, 0, LARGURA_BARRA, getHeight());
            }
            super.paintComponent(g);
        }
    }
}
