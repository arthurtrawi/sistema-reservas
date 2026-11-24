package view.componentes;

import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.ColorUIResource;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import java.awt.Color;
import java.awt.Font;

/**
 * Identidade visual centralizada: cores, fontes e espaçamentos.
 *
 * Todas as telas usam estas constantes; mudar uma cor aqui muda o sistema
 * inteiro, garantindo consistência visual. Classe final e não instanciável:
 * é apenas um agrupador de constantes.
 */
public final class Tema {

    // ---- Cores gerais -------------------------------------------------
    public static final Color FUNDO = new Color(0xF1F5F9);
    public static final Color SUPERFICIE = Color.WHITE;
    public static final Color BORDA = new Color(0xE2E8F0);
    public static final Color BORDA_CAMPO = new Color(0xCBD5E1);
    public static final Color TEXTO = new Color(0x0F172A);
    public static final Color TEXTO_SECUNDARIO = new Color(0x64748B);
    public static final Color SELECAO = new Color(0xDBEAFE);

    // ---- Ações --------------------------------------------------------
    public static final Color PRIMARIA = new Color(0x2563EB);
    public static final Color PRIMARIA_HOVER = new Color(0x1D4ED8);
    public static final Color PERIGO = new Color(0xDC2626);
    public static final Color PERIGO_HOVER = new Color(0xB91C1C);
    public static final Color SECUNDARIA_HOVER = new Color(0xF8FAFC);
    public static final Color DESABILITADO = new Color(0xE2E8F0);
    public static final Color TEXTO_DESABILITADO = new Color(0x94A3B8);

    // ---- Menu lateral -------------------------------------------------
    public static final Color MENU_FUNDO = new Color(0x1E293B);
    public static final Color MENU_HOVER = new Color(0x334155);
    public static final Color MENU_TEXTO = new Color(0xCBD5E1);

    // ---- Status de reserva (texto sobre fundo) ------------------------
    public static final Color STATUS_ATIVA_TEXTO = new Color(0x15803D);
    public static final Color STATUS_ATIVA_FUNDO = new Color(0xDCFCE7);
    public static final Color STATUS_CANCELADA_TEXTO = new Color(0xB91C1C);
    public static final Color STATUS_CANCELADA_FUNDO = new Color(0xFEE2E2);
    public static final Color STATUS_CONCLUIDA_TEXTO = new Color(0x475569);
    public static final Color STATUS_CONCLUIDA_FUNDO = new Color(0xE2E8F0);

    // ---- Fontes (SansSerif existe em qualquer sistema operacional) ----
    private static final String FAMILIA = Font.SANS_SERIF;
    public static final Font FONTE_TITULO = new Font(FAMILIA, Font.BOLD, 22);
    public static final Font FONTE_SECAO = new Font(FAMILIA, Font.BOLD, 15);
    public static final Font FONTE_TEXTO = new Font(FAMILIA, Font.PLAIN, 13);
    public static final Font FONTE_ROTULO = new Font(FAMILIA, Font.BOLD, 12);
    public static final Font FONTE_BOTAO = new Font(FAMILIA, Font.BOLD, 13);
    public static final Font FONTE_MENU = new Font(FAMILIA, Font.PLAIN, 14);
    public static final Font FONTE_PEQUENA = new Font(FAMILIA, Font.PLAIN, 12);
    public static final Font FONTE_INDICADOR = new Font(FAMILIA, Font.BOLD, 32);

    // ---- Espaçamentos -------------------------------------------------
    public static final int ESPACO = 16;
    public static final int ESPACO_PEQUENO = 8;
    public static final int LARGURA_MENU = 220;

    private Tema() {
        // Não instanciável.
    }

    /**
     * Configura o Look and Feel "Metal" (multiplataforma, nativo do JDK) e
     * os padrões globais de fontes e cores. Deve ser chamado uma vez, antes
     * de criar qualquer componente.
     */
    public static void aplicar() {
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (UnsupportedLookAndFeelException e) {
            // Metal faz parte do JDK; se falhar, mantém o padrão sem interromper o programa.
        }
        FontUIResource fonte = new FontUIResource(FONTE_TEXTO);
        String[] chavesFonte = {
            "Label.font", "Button.font", "TextField.font", "ComboBox.font",
            "Table.font", "TableHeader.font", "OptionPane.messageFont",
            "OptionPane.buttonFont", "Spinner.font", "FormattedTextField.font",
            "TextArea.font", "ToolTip.font"
        };
        for (String chave : chavesFonte) {
            UIManager.put(chave, fonte);
        }
        UIManager.put("Panel.background", new ColorUIResource(FUNDO));
        UIManager.put("Button.disabledText", new ColorUIResource(TEXTO_DESABILITADO));
        UIManager.put("OptionPane.background", new ColorUIResource(SUPERFICIE));
        UIManager.put("ComboBox.background", new ColorUIResource(SUPERFICIE));
        UIManager.put("ComboBox.selectionBackground", new ColorUIResource(SELECAO));
        UIManager.put("ComboBox.selectionForeground", new ColorUIResource(TEXTO));
    }
}
