package model;

import util.Validador;

import java.util.Objects;

/**
 * Representa um cliente que pode realizar reservas.
 *
 * O CPF é o identificador único: é final e não possui setter, pois mudar o
 * identificador de um registro existente quebraria a integridade das
 * reservas associadas. Os demais dados podem ser atualizados, mas sempre
 * passando pela validação dos setters.
 */
public class Cliente {

    private final String cpf;
    private String nome;
    private String email;
    private String telefone;

    public Cliente(String cpf, String nome, String email, String telefone) {
        this.cpf = Validador.validarCpf(cpf);
        setNome(nome);
        setEmail(email);
        setTelefone(telefone);
    }

    public String getCpf() {
        return cpf;
    }

    public String getNome() {
        return nome;
    }

    public String getEmail() {
        return email;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setNome(String nome) {
        this.nome = Validador.exigirTexto(nome, "nome");
    }

    public void setEmail(String email) {
        this.email = Validador.validarEmail(email);
    }

    public void setTelefone(String telefone) {
        this.telefone = Validador.exigirTexto(telefone, "telefone");
    }

    /** Dois clientes são o mesmo cliente se tiverem o mesmo CPF. */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Cliente)) {
            return false;
        }
        Cliente outro = (Cliente) o;
        return cpf.equals(outro.cpf);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cpf);
    }

    @Override
    public String toString() {
        return "Cliente{cpf=" + cpf + ", nome=" + nome + ", email=" + email + ", telefone=" + telefone + "}";
    }
}
