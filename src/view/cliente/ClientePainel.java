package view.cliente;

import model.Cliente;
import service.ClienteService;
import view.PainelCadastro;
import view.componentes.ComponentesUI;
import view.componentes.Formatos;
import view.componentes.FormularioPainel;
import view.componentes.Notificador;

import javax.swing.JTextField;
import java.util.ArrayList;
import java.util.List;

/**
 * Módulo de Clientes.
 *
 * Herda de PainelCadastro todo o fluxo de tela (layout, modos, botões,
 * pesquisa, confirmações e mensagens) e define apenas o que é próprio de
 * clientes: os campos e as chamadas ao ClienteService.
 *
 * Particularidade: o CPF é o identificador e fica bloqueado durante a edição.
 */
public class ClientePainel extends PainelCadastro {

    private static final long serialVersionUID = 1L;

    private static final int COLUNA_NOME = 0;
    private static final int COLUNA_CPF = 1;

    private final ClienteService clienteService;

    private final JTextField campoNome = ComponentesUI.campoTexto();
    private final JTextField campoCpf = ComponentesUI.campoTexto();
    private final JTextField campoEmail = ComponentesUI.campoTexto();
    private final JTextField campoTelefone = ComponentesUI.campoTexto();

    public ClientePainel(ClienteService clienteService, Notificador notificador) {
        super(notificador, "cliente", "cliente", "clientes", COLUNA_CPF,
                "Nome", "CPF", "E-mail", "Telefone");
        this.clienteService = clienteService;

        campoNome.setName("cliente.nome");
        campoCpf.setName("cliente.cpf");
        campoEmail.setName("cliente.email");
        campoTelefone.setName("cliente.telefone");

        montarTela();
    }

    @Override
    protected void adicionarCampos(FormularioPainel formulario) {
        formulario.adicionarCampo("Nome completo", true, campoNome, null);
        formulario.adicionarCampo("CPF", true, campoCpf, "Somente números ou 000.000.000-00");
        formulario.adicionarCampo("E-mail", true, campoEmail, "Exemplo: nome@dominio.com");
        formulario.adicionarCampo("Telefone", true, campoTelefone, "Exemplo: (11) 91234-5678");
    }

    @Override
    protected JTextField[] camposFormulario() {
        return new JTextField[] {campoNome, campoCpf, campoEmail, campoTelefone};
    }

    @Override
    protected JTextField[] camposObrigatorios(boolean modoEdicao) {
        // Na edição o CPF não é alterado, por isso não entra na verificação.
        return modoEdicao
                ? new JTextField[] {campoNome, campoEmail, campoTelefone}
                : camposFormulario();
    }

    @Override
    protected List<Object[]> carregarLinhas() {
        List<Object[]> linhas = new ArrayList<>();
        for (Cliente cliente : clienteService.listar()) {
            linhas.add(new Object[] {
                cliente.getNome(),
                Formatos.cpf(cliente.getCpf()),
                cliente.getEmail(),
                cliente.getTelefone()
            });
        }
        return linhas;
    }

    @Override
    protected String executarCadastro() {
        Cliente novo = clienteService.cadastrar(campoCpf.getText(), campoNome.getText(),
                campoEmail.getText(), campoTelefone.getText());
        return "Cliente \"" + novo.getNome() + "\" cadastrado com sucesso.";
    }

    @Override
    protected String executarAtualizacao(Object id) {
        Cliente atualizado = clienteService.atualizar((String) id, campoNome.getText(),
                campoEmail.getText(), campoTelefone.getText());
        return "Dados de \"" + atualizado.getNome() + "\" atualizados com sucesso.";
    }

    @Override
    protected void executarExclusao(Object id) {
        clienteService.excluir((String) id);
    }

    @Override
    protected String executarCarregamento(Object id) {
        Cliente cliente = clienteService.buscarPorCpf((String) id);
        campoNome.setText(cliente.getNome());
        campoCpf.setText(Formatos.cpf(cliente.getCpf()));
        campoEmail.setText(cliente.getEmail());
        campoTelefone.setText(cliente.getTelefone());
        return cliente.getNome();
    }

    @Override
    protected String descreverSelecionado() {
        return "\"" + valorSelecionado(COLUNA_NOME) + "\" (CPF " + valorSelecionado(COLUNA_CPF) + ")";
    }

    @Override
    protected void aoMudarModo(boolean modoEdicao) {
        campoCpf.setEditable(!modoEdicao);
    }
}
