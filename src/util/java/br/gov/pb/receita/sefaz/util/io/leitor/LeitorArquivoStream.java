package br.gov.pb.receita.sefaz.util.io.leitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.jboss.logging.Logger;

public class LeitorArquivoStream {

	private static final Logger LOGGER = Logger.getLogger(LeitorArquivoStream.class);

	/**
	 * Lê o arquivo linha por linha utilizando stream, delegando o processamento
	 * para a implementação recebida.
	 *
	 * Utiliza FileLock para impedir processamento concorrente do mesmo arquivo.
	 *
	 * @throws Exception
	 */
	public static void processar(File arquivo, ProcessadorLinha processador) throws Exception {

		LOGGER.infof("Iniciando processamento do arquivo: %s", arquivo.getAbsolutePath());

		/*
		 * Arquivo físico de lock.
		 */
		final File arquivoLock = new File(arquivo.getAbsolutePath() + ".lock");

		/*
		 * Cria arquivo lock caso não exista.
		 */
		if (!arquivoLock.exists()) {

			LOGGER.debugf("Criando arquivo lock: %s", arquivoLock.getAbsolutePath());

			arquivoLock.createNewFile();

		}

		try (RandomAccessFile raf = new RandomAccessFile(arquivoLock, "rw");

				FileChannel channel = raf.getChannel();

				/*
				 * Lock exclusivo do arquivo.
				 */
				FileLock lock = channel.tryLock();

				BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {

			/*
			 * Arquivo já em processamento.
			 */
			if (lock == null) {

				LOGGER.warnf("Arquivo já está em processamento: %s", arquivo.getName());

				throw new IllegalStateException("Arquivo já está em processamento: " + arquivo.getName());

			}

			LOGGER.infof("Lock adquirido com sucesso para arquivo: %s", arquivo.getName());

			String linha;

			long numeroLinha = 0;

			while ((linha = reader.readLine()) != null) {

				numeroLinha++;

				if (numeroLinha % 1000 == 0) {

					LOGGER.debugf("Processadas %d linhas do arquivo %s", numeroLinha, arquivo.getName());

				}

				processador.executar(linha, numeroLinha);

			}

			LOGGER.infof("Processamento finalizado. Arquivo=%s Linhas=%d", arquivo.getName(), numeroLinha);

		} catch (Exception e) {

			LOGGER.errorf(e, "Erro ao processar arquivo: %s", arquivo.getAbsolutePath());

			throw e;

		} finally {

			/*
			 * Remove arquivo lock ao final.
			 */
			if (arquivoLock.exists()) {

				LOGGER.debugf("Removendo arquivo lock: %s", arquivoLock.getAbsolutePath());

				arquivoLock.delete();

			}

			LOGGER.infof("Encerrado processamento do arquivo: %s", arquivo.getAbsolutePath());

		}

	}

	/**
	 * Interface funcional responsável pelo processamento de cada linha do
	 * arquivo.
	 */
	@FunctionalInterface
	public interface ProcessadorLinha {

		void executar(String linha, long numeroLinha) throws Exception;

	}

}