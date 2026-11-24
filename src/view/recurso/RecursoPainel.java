package view.recurso;

import model.Recurso;
import service.RecursoService;
import view.PainelCadastro;
import view.componentes.ComponentesUI;
import view.componentes.FormularioPainel;
import view.componentes.Notificador;

import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;

/**
 * Módulo de Recursos (salas, equipamentos e demais itens reserváveis).
 *
 * Herda o fluxo de tela de PainelCadastro e define os campos de Recurso e
 * as chamadas ao RecursoService. O ID é gerado pelo back-end: não é
 * digitado pelo usuário, apenas exibido na tabela e no modo de edição.
 */
public class RecursoPainel extends PainelCadastro {

    private static final long serialVersionUID = 1L;

    private static final int COLUNA_ID = 0;
    private static final int COLUNA_NOME = 1;

    private final RecursoService recursoService;

    private final JTextField campoNome = ComponentesUI.campoTexto();
    private final JTextField campoTipo = ComponentesUI.campoTexto();
    private final JTextField campoDescricao = ComponentesUI.campoTexto();

    public RecursoPainel(RecursoService recursoService, Notificador notificador) {
        super(notificador, "recurso", "recurso", "recursos", COLUNA_ID,
                "ID", "Nome", "Tipo", "Descrição");
        this.recursoService = recursoService;

        campoNome.setName("recurso.nome");
        campoTipo.setName("recurso.tipo");
        campoDescricao.setName("recurso.descricao");

        montarTela();
        // Proporções das colunas (o JTable redistribui conforme a largura disponível).
        getTabela().definirLarguraColuna(COLUNA_ID, 40);
        getTabela().definirLarguraColuna(COLUNA_NOME, 190);
        getTabela().definirLarguraColuna(2, 150);
        getTabela().definirLarguraColuna(3, 260);
    }

    @Override
    protected void adicionarCampos(FormularioPainel formulario) {
        formulario.adicionarCampo("Nome", true, campoNome, "Exemplo: Sala 101, Projetor Epson");
        formulario.adicionarCampo("Tipo", true, campoTipo, "Exemplo: Sala de reunião, Equipamento");
        formulario.adicionarCampo("Descrição", false, campoDescricao, "Opcional: capacidade, localização...");
    }

    @Override
    protected JTextField[] camposFormulario() {
        return new JTextField[] {campoNome, campoTipo, campoDescricao};
    }

    @Override
    protected JTextField[] camposObrigatorios(boolean modoEdicao) {
        return new JTextField[] {campoNome, campoTipo};
    }

    @Override
    protected List<Object[]> carregarLinhas() {
        List<Object[]> linhas = new ArrayList<>();
        for (Recurso recurso : recursoService.listar()) {
            linhas.add(new Object[] {
                recurso.getId(),
                recurso.getNome(),
                recurso.getTipo(),
                recurso.getDescricao()
            });
        }
        return linhas;
    }

    @Override
    protected String executarCadastro() {
        Recurso novo = recursoService.cadastrar(campoNome.getText(), campoTipo.getText(),
                campoDescricao.getText());
        return "Recurso \"" + novo.getNome() + "\" cadastrado com sucesso (ID " + novo.getId() + ").";
    }

    @Override
    protected String executarAtualizacao(Object id) {
        Recurso atualizado = recursoService.atualizar((Integer) id, campoNome.getText(),
                campoTipo.getText(), campoDescricao.getText());
        return "Recurso \"" + atualizado.getNome() + "\" atualizado com sucesso.";
    }

    @Override
    protected void executarExclusao(Object id) {
        recursoService.excluir((Integer) id);
    }

    @Override
    protected String executarCarregamento(Object id) {
        Recurso recurso = recursoService.buscarPorId((Integer) id);
        campoNome.setText(recurso.getNome());
        campoTipo.setText(recurso.getTipo());
        campoDescricao.setText(recurso.getDescricao());
        return recurso.getNome() + " (ID " + recurso.getId() + ")";
    }

    @Override
    protected String descreverSelecionado() {
        return "\"" + valorSelecionado(COLUNA_NOME) + "\" (ID " + valorSelecionado(COLUNA_ID) + ")";
    }
}
