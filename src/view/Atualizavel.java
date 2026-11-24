package view;

/**
 * Contrato dos painéis que exibem dados do back-end.
 *
 * Sempre que um módulo é aberto, a JanelaPrincipal chama atualizar(), e o
 * painel recarrega suas tabelas, combos e indicadores a partir dos serviços.
 * Assim os módulos ficam sincronizados sem precisarem conhecer uns aos outros.
 */
public interface Atualizavel {

    void atualizar();
}
