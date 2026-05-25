package br.gov.pb.receita.sefaz.util.io.leitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

public class LeitorArquivoStream {

	/**
	 * Lê o arquivo linha por linha utilizando stream, delegando o processamento
	 * para a implementação recebida.
	 *
	 * Utiliza FileLock para impedir processamento concorrente do mesmo arquivo.
	 *
	 * @throws Exception
	 */
	public static void processar(File arquivo, ProcessadorLinha processador) throws Exception {

		/*
		 * Arquivo físico de lock.
		 */
		final File arquivoLock = new File(arquivo.getAbsolutePath() + ".lock");

		/*
		 * Cria arquivo lock caso não exista.
		 */
		if (!arquivoLock.exists()) {

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

				throw new IllegalStateException("Arquivo já está em processamento: " + arquivo.getName());

			}

			String linha;

			long numeroLinha = 0;

			while ((linha = reader.readLine()) != null) {

				numeroLinha++;

				processador.executar(linha, numeroLinha);

			}

		} finally {

			/*
			 * Remove arquivo lock ao final.
			 */
			if (arquivoLock.exists()) {

				arquivoLock.delete();

			}

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