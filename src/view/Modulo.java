package view;

/**
 * Módulos navegáveis da aplicação, com o título e a descrição exibidos
 * no cabeçalho. Usar enum evita "strings mágicas" na navegação.
 */
public enum Modulo {

    INICIO("Início", "Visão geral do sistema e acesso rápido aos módulos"),
    CLIENTES("Clientes", "Cadastre, pesquise, atualize e exclua clientes"),
    RECURSOS("Recursos", "Gerencie salas, equipamentos e demais itens reserváveis"),
    RESERVAS("Reservas", "Crie, consulte, filtre e cancele reservas de recursos");

    private final String titulo;
    private final String descricao;

    Modulo(String titulo, String descricao) {
        this.titulo = titulo;
        this.descricao = descricao;
    }

    public String getTitulo() {
        return titulo;
    }

    public String getDescricao() {
        return descricao;
    }
}
