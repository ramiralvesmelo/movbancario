package br.gov.pb.receita.sefaz.util.io.leitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public final class LeitorArquivoStream {

	private static final long LOG_PROGRESSO_LINHAS = 1000;

	private LeitorArquivoStream() {

		throw new IllegalStateException("Classe utilitária não pode ser instanciada.");

	}

	/**
	 * ========================================================= PROCESSAMENTO
	 * STREAM =========================================================
	 *
	 * Lê o arquivo linha por linha utilizando stream, delegando o processamento
	 * para implementação recebida.
	 *
	 * Utiliza FileLock para impedir processamento concorrente.
	 *
	 * Exemplo lock:
	 *
	 * arquivo.txt.lock
	 *
	 * @throws Exception
	 */
	public static void processar(File arquivo, ProcessadorLinha processador) throws Exception {

		validarArquivo(arquivo);

		log.infof("Iniciando processamento arquivo=%s", arquivo.getAbsolutePath());

		final File arquivoLock = obterArquivoLock(arquivo);

		criarArquivoLock(arquivoLock);

		try (RandomAccessFile raf = new RandomAccessFile(arquivoLock, "rw");

				FileChannel channel = raf.getChannel();

				FileLock lock = channel.tryLock();

				BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {

			validarLock(lock, arquivo);

			processarLinhas(reader, processador, arquivo);

		} catch (Exception e) {

			log.errorf(e, "Erro ao processar arquivo=%s", arquivo.getAbsolutePath());

			throw e;

		} finally {

			removerArquivoLock(arquivoLock);

			log.infof("Encerrado processamento arquivo=%s", arquivo.getAbsolutePath());

		}

	}

	/**
	 * ========================================================= VALIDAÇÕES
	 * =========================================================
	 */

	private static void validarArquivo(File arquivo) {

		if (arquivo == null) {

			throw new IllegalArgumentException("Arquivo não informado.");

		}

		if (!arquivo.exists()) {

			throw new IllegalArgumentException("Arquivo não encontrado: " + arquivo.getAbsolutePath());

		}

		if (!arquivo.isFile()) {

			throw new IllegalArgumentException("O caminho informado não é um arquivo: " + arquivo.getAbsolutePath());

		}

	}

	private static void validarLock(FileLock lock, File arquivo) {

		if (lock != null) {

			log.infof("Lock adquirido com sucesso arquivo=%s", arquivo.getName());

			return;

		}

		log.warnf("Arquivo já está em processamento: %s", arquivo.getName());

		throw new IllegalStateException("Arquivo já está em processamento: " + arquivo.getName());

	}

	/**
	 * ========================================================= LOCK
	 * =========================================================
	 */

	private static File obterArquivoLock(File arquivo) {

		return new File(arquivo.getAbsolutePath() + ".lock");

	}

	private static void criarArquivoLock(File arquivoLock) throws Exception {

		if (arquivoLock.exists()) {

			return;

		}

		log.debugf("Criando arquivo lock=%s", arquivoLock.getAbsolutePath());

		arquivoLock.createNewFile();

	}

	private static void removerArquivoLock(File arquivoLock) {

		if (!arquivoLock.exists()) {

			return;

		}

		log.debugf("Removendo arquivo lock=%s", arquivoLock.getAbsolutePath());

		if (!arquivoLock.delete()) {

			log.warnf("Não foi possível remover arquivo lock=%s", arquivoLock.getAbsolutePath());

		}

	}

	/**
	 * ========================================================= PROCESSAMENTO
	 * =========================================================
	 */

	private static void processarLinhas(BufferedReader reader, ProcessadorLinha processador, File arquivo)
			throws Exception {

		String linha;

		long numeroLinha = 0;

		while ((linha = reader.readLine()) != null) {

			numeroLinha++;

			logarProgresso(numeroLinha, arquivo);

			processador.executar(linha, numeroLinha);

		}

		log.infof("Processamento finalizado arquivo=%s linhas=%d", arquivo.getName(), numeroLinha);

	}

	private static void logarProgresso(long numeroLinha, File arquivo) {

		if (numeroLinha % LOG_PROGRESSO_LINHAS != 0) {

			return;

		}

		log.debugf("Processadas %d linhas arquivo=%s", numeroLinha, arquivo.getName());

	}

	/**
	 * ========================================================= CALLBACK
	 * PROCESSAMENTO =========================================================
	 */

	@FunctionalInterface
	public interface ProcessadorLinha {

		void executar(String linha, long numeroLinha) throws Exception;

	}

}