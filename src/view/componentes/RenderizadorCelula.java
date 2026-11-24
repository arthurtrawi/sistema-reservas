package view.componentes;

import javax.swing.BorderFactory;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.Color;
import java.awt.Component;
import java.time.LocalDateTime;

/**
 * Desenho padrão das células das tabelas: espaçamento interno e linhas
 * alternadas (zebra) para facilitar a leitura.
 *
 * Classes que precisam de um desenho especial (ex.: o status colorido das
 * reservas) herdam desta e sobrescrevem apenas o que muda.
 */
public class RenderizadorCelula extends DefaultTableCellRenderer {

    private static final long serialVersionUID = 1L;

    private static final Color LINHA_ALTERNADA = new Color(0xF8FAFC);

    @Override
    public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                                                   boolean comFoco, int linha, int coluna) {
        super.getTableCellRendererComponent(tabela, valor, selecionada, false, linha, coluna);
        setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
        setFont(Tema.FONTE_TEXTO);
        setHorizontalAlignment(LEFT);
        // Dica com o texto completo, útil quando a coluna é estreita demais.
        setToolTipText(getText().isEmpty() ? null : getText());
        if (selecionada) {
            setBackground(Tema.SELECAO);
            setForeground(Tema.TEXTO);
        } else {
            setBackground(linha % 2 == 0 ? Tema.SUPERFICIE : LINHA_ALTERNADA);
            setForeground(Tema.TEXTO);
        }
        return this;
    }

    /** Datas são guardadas como LocalDateTime (ordenação correta) e exibidas no formato brasileiro. */
    @Override
    protected void setValue(Object valor) {
        if (valor instanceof LocalDateTime) {
            setText(Formatos.dataHora((LocalDateTime) valor));
        } else {
            super.setValue(valor);
        }
    }
}
