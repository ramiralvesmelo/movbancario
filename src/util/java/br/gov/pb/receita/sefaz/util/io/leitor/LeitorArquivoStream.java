package br.gov.pb.receita.sefaz.util.io.leitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public final class LeitorArquivoStream {

	private static final long LOG_PROGRESSO_LINHAS = 1000;

	private static final String LOCK_EXTENSION = ".lock";

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
	 * Exemplo:
	 *
	 * arquivo.txt.lock
	 */
	public static void processar(File arquivo, ProcessadorLinha processador) throws Exception {

		validarArquivo(arquivo);

		log.infof("Iniciando processamento arquivo=%s", arquivo.getAbsolutePath());

		final File arquivoLock = obterArquivoLock(arquivo);

		criarArquivoLock(arquivoLock);

		try (RandomAccessFile raf = new RandomAccessFile(arquivoLock, "rw");

				FileChannel channel = raf.getChannel();

				BufferedReader reader = new BufferedReader(new FileReader(arquivo))) {

			FileLock lock;

			try {

				lock = channel.tryLock();

			} catch (OverlappingFileLockException e) {

				log.warnf("Arquivo já está sendo processado por outra thread: %s", arquivo.getName());

				return;

			}

			validarLock(lock, arquivo);

			processarLinhas(reader, processador, arquivo);

		} finally {

			removerLock(arquivo);

		}

	}

	/**
	 * ========================================================= LIMPEZA LOCK
	 * =========================================================
	 */
	public static void removerLock(File arquivo) {

		if (arquivo == null) {

			return;

		}

		final File lock = obterArquivoLock(arquivo);

		if (!lock.exists()) {

			return;

		}

		try {

			if (lock.delete()) {

				log.infof("Lock removido arquivo=%s", arquivo.getName());

			} else {

				log.warnf("Nao foi possivel remover lock arquivo=%s", arquivo.getName());

			}

		} catch (Exception e) {

			log.errorf(e, "Erro removendo lock arquivo=%s", arquivo.getName());

		}

	}

	/**
	 * ========================================================= LIMPEZA LOCKS
	 * =========================================================
	 */
	public static void removerLocks(File diretorio) {

		if (diretorio == null) {

			throw new IllegalArgumentException("Diretorio nao informado.");

		}

		if (!diretorio.exists() || !diretorio.isDirectory()) {

			return;

		}

		File[] locks = diretorio.listFiles(file -> file.isFile() && file.getName().endsWith(LOCK_EXTENSION));

		if (locks == null || locks.length == 0) {

			log.infof("Nenhum lock encontrado diretorio=%s", diretorio.getAbsolutePath());

			return;

		}

		for (File lock : locks) {

			try {

				if (lock.delete()) {

					log.infof("Lock removido=%s", lock.getAbsolutePath());

				} else {

					log.warnf("Nao foi possivel remover lock=%s", lock.getAbsolutePath());

				}

			} catch (Exception e) {

				log.errorf(e, "Erro removendo lock=%s", lock.getAbsolutePath());

			}

		}

	}

	/**
	 * ========================================================= LIMPEZA LOCKS
	 * TXT =========================================================
	 */
	public static void removerLocksTxt(File diretorio) {

		if (diretorio == null || !diretorio.exists() || !diretorio.isDirectory()) {

			return;

		}

		File[] arquivos = diretorio.listFiles(file -> file.isFile() && file.getName().toLowerCase().endsWith(".txt"));

		if (arquivos == null) {

			return;

		}

		for (File arquivo : arquivos) {

			removerLock(arquivo);

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

		return new File(arquivo.getAbsolutePath() + LOCK_EXTENSION);

	}

	private static void criarArquivoLock(File arquivoLock) throws Exception {

		if (arquivoLock.exists()) {

			return;

		}

		log.debugf("Criando arquivo lock=%s", arquivoLock.getAbsolutePath());

		arquivoLock.createNewFile();

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