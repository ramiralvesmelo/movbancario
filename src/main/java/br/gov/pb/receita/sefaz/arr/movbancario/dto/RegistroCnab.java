package br.gov.pb.receita.sefaz.arr.movbancario.dto;

import java.math.BigDecimal;

public class RegistroCnab {

    private String banco;
    private String documento;
    private String nome;
    private BigDecimal valor;

    public String getBanco() {
        return banco;
    }

    public void setBanco(String banco) {
        this.banco = banco;
    }

    public String getDocumento() {
        return documento;
    }

    public void setDocumento(String documento) {
        this.documento = documento;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

}
