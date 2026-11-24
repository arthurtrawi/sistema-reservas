package model;

import util.Validador;

import java.util.Objects;

/**
 * Representa um recurso que pode ser reservado (sala, equipamento, quadra...).
 *
 * O id é gerado pela camada de repositório e recebido no construtor; é final
 * para não poder ser alterado depois da criação.
 */
public class Recurso {

    private final int id;
    private String nome;
    private String tipo;
    private String descricao;

    public Recurso(int id, String nome, String tipo, String descricao) {
        if (id <= 0) {
            throw new IllegalArgumentException("O id do recurso deve ser positivo.");
        }
        this.id = id;
        setNome(nome);
        setTipo(tipo);
        setDescricao(descricao);
    }

    public int getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getTipo() {
        return tipo;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setNome(String nome) {
        this.nome = Validador.exigirTexto(nome, "nome");
    }

    public void setTipo(String tipo) {
        this.tipo = Validador.exigirTexto(tipo, "tipo");
    }

    /** Descrição é opcional: nulo vira texto vazio para evitar NullPointerException. */
    public void setDescricao(String descricao) {
        this.descricao = (descricao == null) ? "" : descricao.trim();
    }

    /** Dois recursos são o mesmo recurso se tiverem o mesmo id. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Recurso)) {
            return false;
        }
        Recurso outro = (Recurso) o;
        return id == outro.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "Recurso{id=" + id + ", nome=" + nome + ", tipo=" + tipo + "}";
    }
}
