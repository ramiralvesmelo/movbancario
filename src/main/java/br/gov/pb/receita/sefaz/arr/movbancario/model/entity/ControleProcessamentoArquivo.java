package br.gov.pb.receita.sefaz.arr.movbancario.model.entity;

import java.io.Serializable;

import lombok.Data;

@Data
public class ControleProcessamentoArquivo implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private String arquivo;

    private String status;

    private String inicio;

    private String fim;

    private long tempoMs;

    private long total;

    private long validos;

    private long invalidos;

}