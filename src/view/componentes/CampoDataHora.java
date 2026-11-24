package view.componentes;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JFormattedTextField;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;
import java.util.Date;

/**
 * Campo composto de data (dd/MM/yyyy) e hora (HH:mm), usando JSpinner.
 *
 * O usuário escolhe pelas setas ou digita; não há texto livre a interpretar.
 * O componente converte o valor para LocalDateTime, que é o tipo usado pelo
 * back-end. JSpinner trabalha com java.util.Date, por isso a conversão
 * passa pelo fuso horário do sistema.
 */
public class CampoDataHora extends JPanel {

    private static final long serialVersionUID = 1L;

    private static final int LARGURA_HORA = 86;

    private final JSpinner data;
    private final JSpinner hora;

    public CampoDataHora() {
        super(new BorderLayout(Tema.ESPACO_PEQUENO, 0));
        setOpaque(false);

        data = criarSpinner(Calendar.DAY_OF_MONTH, "dd/MM/yyyy");
        hora = criarSpinner(Calendar.MINUTE, "HH:mm");
        hora.setPreferredSize(new Dimension(LARGURA_HORA, hora.getPreferredSize().height));

        add(data, BorderLayout.CENTER);
        add(hora, BorderLayout.EAST);
    }

    /**
     * Data e hora escolhidas (segundos zerados).
     *
     * @throws IllegalArgumentException se o texto digitado não estiver no formato esperado
     */
    public LocalDateTime getValor() {
        confirmarDigitacao(data, "Data inválida: use o formato dd/MM/aaaa.");
        confirmarDigitacao(hora, "Hora inválida: use o formato HH:mm.");
        LocalDateTime diaEscolhido = paraLocalDateTime((Date) data.getValue());
        LocalDateTime horaEscolhida = paraLocalDateTime((Date) hora.getValue());
        return diaEscolhido.toLocalDate()
                .atTime(horaEscolhida.toLocalTime())
                .truncatedTo(ChronoUnit.MINUTES);
    }

    public void setValor(LocalDateTime valor) {
        Date convertido = Date.from(valor.atZone(ZoneId.systemDefault()).toInstant());
        data.setValue(convertido);
        hora.setValue(convertido);
    }

    /** Executa a ação sempre que a data ou a hora mudarem. */
    public void aoAlterar(Runnable acao) {
        data.addChangeListener(evento -> acao.run());
        hora.addChangeListener(evento -> acao.run());
    }

    @Override
    public void setName(String nome) {
        super.setName(nome);
        data.setName(nome + ".data");
        hora.setName(nome + ".hora");
    }

    private static JSpinner criarSpinner(int campoCalendario, String padrao) {
        JSpinner spinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, campoCalendario));
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, padrao);
        spinner.setEditor(editor);
        spinner.setFont(Tema.FONTE_TEXTO);
        spinner.setBorder(BorderFactory.createLineBorder(Tema.BORDA_CAMPO));
        JFormattedTextField texto = editor.getTextField();
        texto.setFont(Tema.FONTE_TEXTO);
        texto.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 4));
        return spinner;
    }

    /** Aplica o que o usuário digitou (sem precisar sair do campo) ou acusa formato inválido. */
    private static void confirmarDigitacao(JSpinner spinner, String mensagemErro) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            try {
                ((JSpinner.DefaultEditor) editor).getTextField().commitEdit();
            } catch (ParseException e) {
                throw new IllegalArgumentException(mensagemErro);
            }
        }
    }

    private static LocalDateTime paraLocalDateTime(Date valor) {
        return LocalDateTime.ofInstant(valor.toInstant(), ZoneId.systemDefault());
    }
}
