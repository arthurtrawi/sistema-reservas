package view.componentes;

import javax.swing.JOptionPane;
import java.awt.Component;

/**
 * Implementação de Notificador com caixas de diálogo (JOptionPane),
 * centralizadas sobre a janela principal.
 */
public class NotificadorDialogo implements Notificador {

    private final Component janelaPai;

    public NotificadorDialogo(Component janelaPai) {
        this.janelaPai = janelaPai;
    }

    @Override
    public void sucesso(String mensagem) {
        JOptionPane.showMessageDialog(janelaPai, mensagem, "Sucesso", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void aviso(String mensagem) {
        JOptionPane.showMessageDialog(janelaPai, mensagem, "Atenção", JOptionPane.WARNING_MESSAGE);
    }

    @Override
    public void erro(String titulo, String mensagem) {
        JOptionPane.showMessageDialog(janelaPai, mensagem, titulo, JOptionPane.ERROR_MESSAGE);
    }

    @Override
    public boolean confirmar(String titulo, String mensagem) {
        Object[] opcoes = {"Sim", "Não"};
        int resposta = JOptionPane.showOptionDialog(janelaPai, mensagem, titulo,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, opcoes, opcoes[1]);
        return resposta == 0; // "Não" é o padrão: operações destrutivas exigem escolha explícita
    }
}
