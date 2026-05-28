package br.gov.pb.receita.sefaz.arr.movbancario.model.entity;

import java.io.Serializable;
import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroCnab
        implements Serializable {

    private static final long serialVersionUID =
            1L;

    /*
     * Código banco FEBRABAN.
     */
    private String banco;

    /*
     * Documento cliente.
     */
    private String documento;

    /*
     * Nome cliente.
     */
    private String nome;

    /*
     * Valor financeiro.
     */
    private BigDecimal valor;

    /*
     * Caminho físico arquivo origem.
     */
    private String caminhoArquivo;

}