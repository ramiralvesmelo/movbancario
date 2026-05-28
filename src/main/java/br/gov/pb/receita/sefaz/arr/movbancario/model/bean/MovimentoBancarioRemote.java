package br.gov.pb.receita.sefaz.arr.movbancario.model.bean;

import javax.ejb.Remote;

/**
 * Interface EJB Remota.
 */
@Remote
public interface MovimentoBancarioRemote {

	/**
	 * Processa arquivos CNAB.
	 */
	void processarArquivos();

}