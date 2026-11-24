package view;

import exception.SistemaReservasException;
import view.componentes.Botao;
import view.componentes.ComponentesUI;
import view.componentes.FormularioPainel;
import view.componentes.Notificador;
import view.componentes.PainelTabela;
import view.componentes.Tema;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

/**
 * Base abstrata das telas de cadastro (Clientes e Recursos).
 *
 * Implementa o fluxo COMUM a qualquer cadastro:
 *  - layout com formulário à esquerda e tabela com pesquisa à direita;
 *  - modos "cadastro" e "edição" (seleção na tabela carrega o registro);
 *  - botões Cadastrar, Atualizar, Limpar, Editar e Excluir;
 *  - confirmação antes de excluir;
 *  - mensagens de sucesso e exibição dos erros do back-end.
 *
 * As subclasses implementam apenas o que é ESPECÍFICO (campos do formulário
 * e chamadas ao seu Service), por meio dos métodos abstratos "executar...".
 * Este é o padrão de projeto Template Method: o algoritmo geral fica na
 * superclasse; os passos variáveis, nas subclasses (herança + polimorfismo).
 *
 * Uso pelas subclasses: no construtor, depois de criar seus campos,
 * chamar montarTela().
 */
public abstract class PainelCadastro extends JPanel implements Atualizavel {

    private static final long serialVersionUID = 1L;

    private static final int LARGURA_FORMULARIO = 340;

    protected final Notificador notificador;

    private final String prefixo;
    private final String singular;
    private final String plural;
    private final int colunaIdentificador;

    private final PainelTabela tabela;
    private final JTextField campoPesquisa = ComponentesUI.campoTexto();
    private final JLabel rotuloModo = ComponentesUI.rotuloCampo("", false);
    private final JLabel rotuloTotal = ComponentesUI.textoSecundario("");
    private final Botao botaoCadastrar = new Botao("Cadastrar", Botao.Estilo.PRIMARIO);
    private final Botao botaoAtualizar = new Botao("Atualizar", Botao.Estilo.PRIMARIO);
    private final Botao botaoLimpar = new Botao("Limpar", Botao.Estilo.SECUNDARIO);
    private final Botao botaoEditar = new Botao("Editar", Botao.Estilo.SECUNDARIO);
    private final Botao botaoExcluir = new Botao("Excluir", Botao.Estilo.PERIGO);

    /** Identificador do registro em edição; null no modo de cadastro. */
    private Object idEmEdicao;

    /**
     * @param prefixo             prefixo dos nomes de componentes (ex.: "cliente")
     * @param singular            nome da entidade no singular, minúsculo (ex.: "cliente")
     * @param plural              nome no plural, minúsculo (ex.: "clientes")
     * @param colunaIdentificador coluna da tabela que contém o identificador do registro
     * @param colunas             títulos das colunas da tabela
     */
    protected PainelCadastro(Notificador notificador, String prefixo, String singular, String plural,
                             int colunaIdentificador, String... colunas) {
        super(new BorderLayout(Tema.ESPACO, 0));
        setOpaque(false);
        this.notificador = notificador;
        this.prefixo = prefixo;
        this.singular = singular;
        this.plural = plural;
        this.colunaIdentificador = colunaIdentificador;
        this.tabela = new PainelTabela(prefixo,
                "Nenhum " + singular + " cadastrado. Preencha o formulário ao lado para começar.", colunas);
    }

    // ==================================================================
    // Passos específicos, implementados pelas subclasses
    // ==================================================================

    /** Adiciona os campos da entidade ao formulário. */
    protected abstract void adicionarCampos(FormularioPainel formulario);

    /** Todos os campos de texto do formulário (para limpar e para Enter). */
    protected abstract JTextField[] camposFormulario();

    /** Campos que não podem ficar vazios no modo informado. */
    protected abstract JTextField[] camposObrigatorios(boolean modoEdicao);

    /** Busca os registros no Service e os converte em linhas da tabela. */
    protected abstract List<Object[]> carregarLinhas();

    /** Chama o Service para cadastrar. @return mensagem de sucesso */
    protected abstract String executarCadastro();

    /** Chama o Service para atualizar o registro. @return mensagem de sucesso */
    protected abstract String executarAtualizacao(Object id);

    /** Chama o Service para excluir o registro. */
    protected abstract void executarExclusao(Object id);

    /** Consulta o registro no Service e preenche os campos. @return descrição exibida no modo edição */
    protected abstract String executarCarregamento(Object id);

    /** Texto que identifica o registro selecionado na confirmação de exclusão. */
    protected abstract String descreverSelecionado();

    /** Ajustes específicos ao trocar de modo (ex.: bloquear o CPF na edição). */
    protected void aoMudarModo(boolean modoEdicao) {
        // Por padrão, nada a fazer.
    }

    // ==================================================================
    // Montagem
    // ==================================================================

    /** Monta a tela. Deve ser chamado ao final do construtor da subclasse. */
    protected final void montarTela() {
        nomearComponentes();
        add(criarFormulario(), BorderLayout.WEST);
        add(criarListagem(), BorderLayout.CENTER);
        registrarEventos();
        entrarModoCadastro();
    }

    /** Chamado ao abrir o módulo: recarrega os dados do back-end. */
    @Override
    public void atualizar() {
        recarregarTabela();
        entrarModoCadastro();
    }

    /** Valor de uma coluna na linha selecionada da tabela. */
    protected Object valorSelecionado(int coluna) {
        return tabela.getValorSelecionado(coluna);
    }

    protected PainelTabela getTabela() {
        return tabela;
    }

    private JComponent criarFormulario() {
        FormularioPainel formulario = new FormularioPainel();
        formulario.adicionarComponente(rotuloModo, 14);
        adicionarCampos(formulario);
        formulario.adicionarComponente(ComponentesUI.linhaBotoes(botaoCadastrar, botaoAtualizar), 8);
        formulario.adicionarComponente(ComponentesUI.linhaBotoes(botaoLimpar), 14);
        formulario.adicionarComponente(ComponentesUI.textoSecundario("* Campos obrigatórios"), 0);
        formulario.adicionarEspacoFlexivel();

        JPanel cartao = ComponentesUI.cartao("Dados do " + singular, formulario);
        cartao.setPreferredSize(new Dimension(LARGURA_FORMULARIO, 0));
        return cartao;
    }

    private JComponent criarListagem() {
        JPanel pesquisa = ComponentesUI.painelTransparente(new BorderLayout(Tema.ESPACO_PEQUENO, 0));
        pesquisa.add(ComponentesUI.rotuloCampo("Pesquisar", false), BorderLayout.WEST);
        pesquisa.add(campoPesquisa, BorderLayout.CENTER);
        pesquisa.add(rotuloTotal, BorderLayout.EAST);

        JPanel acoes = ComponentesUI.painelTransparente(new BorderLayout());
        acoes.add(ComponentesUI.linhaBotoes(botaoEditar, botaoExcluir), BorderLayout.EAST);

        JPanel conteudo = ComponentesUI.painelTransparente(new BorderLayout(0, Tema.ESPACO - 4));
        conteudo.add(pesquisa, BorderLayout.NORTH);
        conteudo.add(tabela, BorderLayout.CENTER);
        conteudo.add(acoes, BorderLayout.SOUTH);

        String titulo = Character.toUpperCase(plural.charAt(0)) + plural.substring(1) + " cadastrados";
        return ComponentesUI.cartao(titulo, conteudo);
    }

    /** Nomes usados para localizar os componentes nos testes automáticos. */
    private void nomearComponentes() {
        campoPesquisa.setName(prefixo + ".pesquisa");
        rotuloTotal.setName(prefixo + ".total");
        rotuloModo.setName(prefixo + ".modo");
        botaoCadastrar.setName(prefixo + ".cadastrar");
        botaoAtualizar.setName(prefixo + ".atualizar");
        botaoLimpar.setName(prefixo + ".limpar");
        botaoEditar.setName(prefixo + ".editar");
        botaoExcluir.setName(prefixo + ".excluir");
    }

    // ==================================================================
    // Eventos
    // ==================================================================

    private void registrarEventos() {
        botaoCadastrar.addActionListener(evento -> cadastrar());
        botaoAtualizar.addActionListener(evento -> atualizarEmEdicao());
        botaoLimpar.addActionListener(evento -> limparFormulario());
        botaoEditar.addActionListener(evento -> camposFormulario()[0].requestFocusInWindow());
        botaoExcluir.addActionListener(evento -> excluirSelecionado());

        // Enter em qualquer campo executa a ação principal do modo atual.
        for (JTextField campo : camposFormulario()) {
            campo.addActionListener(evento -> {
                if (idEmEdicao == null) {
                    cadastrar();
                } else {
                    atualizarEmEdicao();
                }
            });
        }

        tabela.aoMudarSelecao(this::aoMudarSelecao);
        ComponentesUI.aoAlterarTexto(campoPesquisa, () -> {
            tabela.filtrar(campoPesquisa.getText());
            atualizarTotal();
        });
    }

    private void aoMudarSelecao() {
        boolean selecionado = tabela.temSelecao();
        botaoEditar.setEnabled(selecionado);
        botaoExcluir.setEnabled(selecionado);
        if (selecionado) {
            carregarParaEdicao(tabela.getValorSelecionado(colunaIdentificador));
        }
    }

    // ==================================================================
    // Fluxo das operações (comum a todos os cadastros)
    // ==================================================================

    private void cadastrar() {
        if (!camposPreenchidos(false)) {
            return;
        }
        try {
            notificador.sucesso(executarCadastro());
            recarregarTabela();
            limparFormulario();
        } catch (SistemaReservasException | IllegalArgumentException e) {
            notificador.erro(e);
        }
    }

    private void atualizarEmEdicao() {
        if (idEmEdicao == null) {
            notificador.aviso("Selecione um " + singular + " na tabela para atualizar.");
            return;
        }
        if (!camposPreenchidos(true)) {
            return;
        }
        try {
            notificador.sucesso(executarAtualizacao(idEmEdicao));
            recarregarTabela();
            limparFormulario();
        } catch (SistemaReservasException | IllegalArgumentException e) {
            notificador.erro(e);
        }
    }

    private void excluirSelecionado() {
        if (!tabela.temSelecao()) {
            notificador.aviso("Selecione um " + singular + " na tabela para excluir.");
            return;
        }
        Object id = tabela.getValorSelecionado(colunaIdentificador);
        String descricao = descreverSelecionado();
        boolean confirmado = notificador.confirmar("Excluir " + singular,
                "Deseja excluir o " + singular + " " + descricao + "?\nEsta operação não pode ser desfeita.");
        if (!confirmado) {
            return;
        }
        try {
            executarExclusao(id);
            notificador.sucesso("O " + singular + " " + descricao + " foi excluído com sucesso.");
            recarregarTabela();
            limparFormulario();
        } catch (SistemaReservasException | IllegalArgumentException e) {
            notificador.erro(e);
        }
    }

    private void carregarParaEdicao(Object id) {
        try {
            String descricao = executarCarregamento(id);
            idEmEdicao = id;
            definirModo(true, descricao);
        } catch (SistemaReservasException | IllegalArgumentException e) {
            notificador.erro(e);
            recarregarTabela();
        }
    }

    /** Verificação de usabilidade: campos obrigatórios em branco são destacados. */
    private boolean camposPreenchidos(boolean modoEdicao) {
        if (ComponentesUI.verificarPreenchidos(camposObrigatorios(modoEdicao))) {
            return true;
        }
        notificador.aviso("Preencha os campos obrigatórios destacados em vermelho.");
        return false;
    }

    // ==================================================================
    // Estado da tela
    // ==================================================================

    private void recarregarTabela() {
        tabela.exibir(carregarLinhas());
        atualizarTotal();
        aoMudarSelecao();
    }

    private void limparFormulario() {
        tabela.limparSelecao();
        entrarModoCadastro();
        camposFormulario()[0].requestFocusInWindow();
    }

    private void entrarModoCadastro() {
        idEmEdicao = null;
        for (JTextField campo : camposFormulario()) {
            campo.setText("");
        }
        definirModo(false, null);
    }

    private void definirModo(boolean modoEdicao, String descricao) {
        botaoCadastrar.setEnabled(!modoEdicao);
        botaoAtualizar.setEnabled(modoEdicao);
        rotuloModo.setText(modoEdicao ? "Editando: " + descricao : "Novo cadastro");
        rotuloModo.setForeground(modoEdicao ? Tema.PRIMARIA : Tema.TEXTO_SECUNDARIO);
        for (JTextField campo : camposFormulario()) {
            ComponentesUI.marcarErro(campo, false);
        }
        aoMudarModo(modoEdicao);
    }

    private void atualizarTotal() {
        int total = tabela.getTotalLinhas();
        int visiveis = tabela.getLinhasVisiveis();
        String texto = total + " " + (total == 1 ? singular : plural);
        rotuloTotal.setText(visiveis == total ? texto : visiveis + " de " + texto);
    }
}
