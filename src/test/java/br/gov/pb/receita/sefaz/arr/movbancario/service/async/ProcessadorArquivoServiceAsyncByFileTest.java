package br.gov.pb.receita.sefaz.arr.movbancario.service.async;

import java.io.File;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.gov.pb.receita.sefaz.arr.movbancario.factory.ProcessadorArquivoFactory;
import br.gov.pb.receita.sefaz.arr.movbancario.service.ProcessadorArquivoService;

/**
 * Processamento concorrente:
 *
 * 1 thread por arquivo.
 *
 * Exemplo:
 *
 * 5 arquivos = 5 threads
 * 20 arquivos = 20 threads
 */
public class ProcessadorArquivoServiceAsyncByFileTest {

	private static final Logger LOG =
			LoggerFactory.getLogger(
					ProcessadorArquivoServiceAsyncByFileTest.class);

	private ProcessadorArquivoService service;

	@Before
	public void before()
			throws Exception {

		LOG.info("Inicializando service.");

		service =
				ProcessadorArquivoFactory.criarService();

		LOG.info("Service inicializado.");

	}

	@Test
	public void processar()
			throws Exception {

		final File diretorio =
				new File("src/test/resources/cnab");

		/*
		 * Lista arquivos CNAB.
		 */
		final File[] arquivos =
				diretorio.listFiles(
						(file) -> file.isFile()
								&& file.getName().endsWith(".txt"));

		if (arquivos == null
				|| arquivos.length == 0) {

			LOG.warn("Nenhum arquivo encontrado.");

			return;

		}

		/*
		 * Quantidade threads =
		 * quantidade arquivos.
		 */
		final int threads =
				arquivos.length;

		LOG.info("==================================================");
		LOG.info("INICIO PROCESSAMENTO");
		LOG.info("Quantidade threads: {}", threads);
		LOG.info("Quantidade arquivos: {}", arquivos.length);		
		LOG.info("Horario inicio: {}", new Date());
		LOG.info("==================================================");

		final long inicioLote =
				System.currentTimeMillis();

		/*
		 * 1 thread por arquivo.
		 */
		final ExecutorService executor =
				Executors.newFixedThreadPool(
						threads);

		/*
		 * Processa arquivos.
		 */
		for (File arquivo : arquivos) {

			executor.submit(() -> {

				final long inicioArquivo =
						System.currentTimeMillis();

				final String nomeThread =
						Thread.currentThread().getName();

				LOG.info("--------------------------------------------------");
				LOG.info("THREAD INICIADA");
				LOG.info("Thread JVM: {}", nomeThread);
				LOG.info("Arquivo: {}", arquivo.getName());				
				LOG.info("Tamanho: {} bytes", arquivo.length());
				LOG.info("Horario inicio: {}", new Date());

				try {

					/*
					 * Executa processamento real.
					 */
					service.processar(arquivo);

					final long tempoArquivo =
							System.currentTimeMillis()
									- inicioArquivo;

					LOG.info("STATUS: SUCESSO");
					LOG.info("Arquivo: {}", arquivo.getName());
					LOG.info("Thread JVM: {}", nomeThread);
					LOG.info("Tempo total: {} ms", tempoArquivo);
					LOG.info("Tempo total: {} segundos",
							(tempoArquivo / 1000));

				} catch (Exception e) {

					LOG.error("STATUS: ERRO");
					LOG.error("Arquivo: {}", arquivo.getName());
					LOG.error("Thread JVM: {}", nomeThread);
					LOG.error("Erro processamento", e);

				}

				LOG.info("THREAD FINALIZADA");
				LOG.info("Arquivo: {}", arquivo.getName());
				LOG.info("Thread JVM: {}", nomeThread);
				LOG.info("--------------------------------------------------");

			});

		}

		/*
		 * Finaliza pool.
		 */
		executor.shutdown();

		/*
		 * Aguarda conclusão.
		 */
		while (!executor.awaitTermination(
				1,
				TimeUnit.SECONDS)) {

			LOG.info("Aguardando processamento dos arquivos...");

		}

		final long tempoTotal =
				System.currentTimeMillis()
						- inicioLote;

		LOG.info("==================================================");
		LOG.info("FIM PROCESSAMENTO");
		LOG.info("Quantidade threads: {}", threads);
		LOG.info("Quantidade arquivos: {}", arquivos.length);		
		LOG.info("Tempo total lote: {} ms", tempoTotal);
		LOG.info("Tempo total lote: {} segundos",
				(tempoTotal / 1000));
		LOG.info("Horario fim: {}", new Date());
		LOG.info("==================================================");

	}

}