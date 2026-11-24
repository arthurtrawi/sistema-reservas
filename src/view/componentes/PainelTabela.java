package view.componentes;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Tabela reutilizável, somente leitura, com:
 *  - estilo padronizado (cabeçalho, linhas alternadas, seleção);
 *  - seleção de uma linha por vez;
 *  - pesquisa por texto (filtro apenas visual, sobre os dados já carregados);
 *  - mensagem amigável quando não há registros ou a pesquisa não encontra nada.
 *
 * Usa CardLayout para alternar entre a tabela e a mensagem de "vazio".
 */
public class PainelTabela extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final String CARTAO_TABELA = "tabela";
    private static final String CARTAO_VAZIO = "vazio";
    private static final int ALTURA_LINHA = 32;

    private final CardLayout cartoes = new CardLayout();
    private final DefaultTableModel modelo;
    private final JTable tabela;
    private final TableRowSorter<DefaultTableModel> ordenador;
    private final JLabel mensagem = new JLabel("", SwingConstants.CENTER);
    private String mensagemSemRegistros;
    private String filtroAtual = "";

    /**
     * @param nome                 prefixo para identificar os componentes (ex.: "cliente")
     * @param mensagemSemRegistros texto exibido quando a lista está vazia
     * @param colunas              títulos das colunas
     */
    public PainelTabela(String nome, String mensagemSemRegistros, String... colunas) {
        super();
        setLayout(cartoes);
        setOpaque(false);
        this.mensagemSemRegistros = mensagemSemRegistros;

        modelo = new ModeloSomenteLeitura(colunas);
        tabela = new JTable(modelo);
        tabela.setName(nome + ".tabela");
        ordenador = new TableRowSorter<>(modelo);
        tabela.setRowSorter(ordenador); // também permite ordenar clicando no cabeçalho
        estilizarTabela();

        JScrollPane rolagem = new JScrollPane(tabela);
        rolagem.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        rolagem.getViewport().setBackground(Tema.SUPERFICIE);

        mensagem.setName(nome + ".vazio");
        mensagem.setFont(Tema.FONTE_TEXTO);
        mensagem.setForeground(Tema.TEXTO_SECUNDARIO);
        JPanel painelVazio = new JPanel(new BorderLayout());
        painelVazio.setBackground(Tema.SUPERFICIE);
        painelVazio.setBorder(BorderFactory.createLineBorder(Tema.BORDA));
        painelVazio.add(mensagem, BorderLayout.CENTER);

        add(rolagem, CARTAO_TABELA);
        add(painelVazio, CARTAO_VAZIO);
        atualizarEstadoVazio();
    }

    // ---- Dados ----------------------------------------------------------

    /** Substitui todas as linhas da tabela. Mantém o filtro de pesquisa ativo. */
    public void exibir(List<Object[]> linhas) {
        modelo.setRowCount(0);
        for (Object[] linha : linhas) {
            modelo.addRow(linha);
        }
        atualizarEstadoVazio();
    }

    /** Altera o texto exibido quando não há registros (ex.: conforme filtros aplicados). */
    public void setMensagemSemRegistros(String mensagem) {
        this.mensagemSemRegistros = mensagem;
        atualizarEstadoVazio();
    }

    /** Filtra as linhas visíveis pelo texto (em qualquer coluna, sem diferenciar maiúsculas). */
    public void filtrar(String texto) {
        filtroAtual = (texto == null) ? "" : texto.trim();
        if (filtroAtual.isEmpty()) {
            ordenador.setRowFilter(null);
        } else {
            ordenador.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(filtroAtual)));
        }
        atualizarEstadoVazio();
    }

    public int getTotalLinhas() {
        return modelo.getRowCount();
    }

    public int getLinhasVisiveis() {
        return tabela.getRowCount();
    }

    // ---- Seleção --------------------------------------------------------

    public boolean temSelecao() {
        return tabela.getSelectedRow() >= 0;
    }

    /**
     * Valor da coluna na linha selecionada, ou null se nada estiver selecionado.
     * Converte o índice visual (afetado por ordenação/filtro) para o do modelo.
     */
    public Object getValorSelecionado(int coluna) {
        int linhaVisual = tabela.getSelectedRow();
        if (linhaVisual < 0) {
            return null;
        }
        return modelo.getValueAt(tabela.convertRowIndexToModel(linhaVisual), coluna);
    }

    public void limparSelecao() {
        tabela.clearSelection();
    }

    /** Registra uma ação a executar quando a seleção mudar (evento ListSelection). */
    public void aoMudarSelecao(Runnable acao) {
        tabela.getSelectionModel().addListSelectionListener(evento -> {
            if (!evento.getValueIsAdjusting()) {
                acao.run();
            }
        });
    }

    // ---- Aparência ------------------------------------------------------

    public void definirRenderizador(int coluna, TableCellRenderer renderizador) {
        tabela.getColumnModel().getColumn(coluna).setCellRenderer(renderizador);
    }

    public void definirLarguraColuna(int coluna, int largura) {
        tabela.getColumnModel().getColumn(coluna).setPreferredWidth(largura);
    }

    public JTable getTabela() {
        return tabela;
    }

    private void estilizarTabela() {
        tabela.setRowHeight(ALTURA_LINHA);
        tabela.setFont(Tema.FONTE_TEXTO);
        tabela.setShowVerticalLines(false);
        tabela.setShowHorizontalLines(true);
        tabela.setGridColor(Tema.BORDA);
        tabela.setIntercellSpacing(new Dimension(0, 1));
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setFillsViewportHeight(true);
        tabela.setBackground(Tema.SUPERFICIE);
        // Number também precisa ser registrado: o JTable tem um renderizador
        // próprio para números, que seria usado no lugar do de Object.
        RenderizadorCelula renderizador = new RenderizadorCelula();
        tabela.setDefaultRenderer(Object.class, renderizador);
        tabela.setDefaultRenderer(Number.class, renderizador);

        JTableHeader cabecalho = tabela.getTableHeader();
        cabecalho.setReorderingAllowed(false);
        cabecalho.setDefaultRenderer(new RenderizadorCabecalho());
        cabecalho.setPreferredSize(new Dimension(0, ALTURA_LINHA + 4));
    }

    private void atualizarEstadoVazio() {
        if (modelo.getRowCount() == 0) {
            mensagem.setText(mensagemSemRegistros);
            cartoes.show(this, CARTAO_VAZIO);
        } else if (tabela.getRowCount() == 0) {
            mensagem.setText("Nenhum resultado encontrado para \"" + filtroAtual + "\".");
            cartoes.show(this, CARTAO_VAZIO);
        } else {
            cartoes.show(this, CARTAO_TABELA);
        }
    }

    /** Modelo de tabela que impede edição direta das células. */
    private static class ModeloSomenteLeitura extends DefaultTableModel {

        private static final long serialVersionUID = 1L;

        ModeloSomenteLeitura(String[] colunas) {
            super(colunas, 0);
        }

        @Override
        public boolean isCellEditable(int linha, int coluna) {
            return false; // alterações passam sempre pelo formulário e pelo Service
        }

        /**
         * Informa o tipo real da coluna (ex.: Integer para IDs), para que a
         * ordenação seja numérica ("2" antes de "10") e não alfabética.
         */
        @Override
        public Class<?> getColumnClass(int coluna) {
            if (getRowCount() > 0 && getValueAt(0, coluna) != null) {
                return getValueAt(0, coluna).getClass();
            }
            return Object.class;
        }
    }

    /** Cabeçalho plano, em negrito, com linha inferior. */
    private static class RenderizadorCabecalho extends DefaultTableCellRenderer {

        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                                                       boolean comFoco, int linha, int coluna) {
            super.getTableCellRendererComponent(tabela, valor, false, false, linha, coluna);
            setFont(Tema.FONTE_ROTULO);
            setForeground(Tema.TEXTO_SECUNDARIO);
            setBackground(new java.awt.Color(0xF8FAFC));
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Tema.BORDA_CAMPO),
                    BorderFactory.createEmptyBorder(0, 10, 0, 10)));
            return this;
        }
    }
}
