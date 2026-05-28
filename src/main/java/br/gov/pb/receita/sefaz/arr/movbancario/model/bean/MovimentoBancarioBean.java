package br.gov.pb.receita.sefaz.arr.movbancario.model.bean;

import java.io.File;

import javax.ejb.EJB;
import javax.ejb.Remote;
import javax.ejb.Stateless;

import br.gov.pb.receita.sefaz.arr.movbancario.model.entity.RegistroCnab;
import br.gov.pb.receita.sefaz.arr.movbancario.model.jms.MovimentoBancarioProducer;
import lombok.extern.jbosslog.JBossLog;

/**
 * Serviço responsável por:
 *
 * - localizar arquivos CNAB
 * - validar arquivos
 * - montar DTO processamento
 * - executar processamento
 *
 * Implementação preparada para:
 *
 * - EJB Remoto
 * - execução distribuída
 * - integração scheduler
 * - JMS
 * - processamento batch
 */
@Stateless
@JBossLog
@Remote(MovimentoBancarioRemote.class)
public class MovimentoBancarioBean implements MovimentoBancarioRemote {
	
	@EJB
	private MovimentoBancarioProducer producerJms;	

	/*
	 * Diretório base arquivos CNAB.
	 *
	 * Exemplo:
	 *
	 * -Dfile.server.movimento.bancario=/tmp/cnab
	 */
	//private static final String DIRETORIO_CNAB = System.getProperty("file.server.movimento.bancario");
	private static final String DIRETORIO_CNAB = "/tmp/cnab";
	

	/**
	 * Processa arquivos CNAB.
	 */
	@Override
	public void processarArquivos() {

		/*
		 * Valida configuração.
		 */
		if (DIRETORIO_CNAB == null || DIRETORIO_CNAB.isEmpty()) {

			throw new IllegalStateException("Propriedade nao configurada: " + "file.server.movimento.bancario");

		}

		final File diretorio = new File(DIRETORIO_CNAB);

		/*
		 * Valida existência.
		 */
		if (!diretorio.exists()) {

			throw new IllegalStateException(String.format("Diretorio nao encontrado: %s", diretorio.getAbsolutePath()));

		}

		/*
		 * Valida diretório físico.
		 */
		if (!diretorio.isDirectory()) {

			throw new IllegalStateException(
					String.format("Caminho informado nao e diretorio: %s", diretorio.getAbsolutePath()));

		}

		/*
		 * Lista arquivos válidos.
		 */
		final File[] arquivos = diretorio.listFiles(file -> {

			/*
			 * Somente arquivos físicos.
			 */
			if (!file.isFile()) {

				return false;

			}

			/*
			 * Somente TXT.
			 */
			if (!file.getName().toLowerCase().endsWith(".txt")) {

				return false;

			}

			/*
			 * Ignora arquivos vazios.
			 */
			if (file.length() <= 0) {

				log.warnf("Arquivo vazio ignorado: %s", file.getName());

				return false;

			}

			return true;

		});

		/*
		 * Nenhum arquivo encontrado.
		 */
		if (arquivos == null || arquivos.length == 0) {

			log.warnf("Nenhum arquivo CNAB encontrado em: %s", diretorio.getAbsolutePath());

			return;

		}

		log.infof("Quantidade arquivos encontrados: %d", arquivos.length);

		/*
		 * Processa arquivos.
		 */
		for (File arquivo : arquivos) {

			try {

				log.infof("Processando arquivo: %s", arquivo.getName());

				RegistroCnab registro = new RegistroCnab();

				/*
				 * Documento lógico.
				 */
				registro.setDocumento(arquivo.getName());

				/*
				 * Caminho físico.
				 */
				registro.setCaminhoArquivo(arquivo.getAbsolutePath());
				log.infof(
				        "ENVIANDO JMS arquivo=%s",
				        arquivo.getName());				
				
				producerJms.enviar(registro);

				log.infof("Arquivo processado com sucesso: %s", arquivo.getName());

			} catch (Exception e) {

				log.errorf(e, "Erro processamento arquivo=%s", arquivo.getName());

			}

		}

	}

}