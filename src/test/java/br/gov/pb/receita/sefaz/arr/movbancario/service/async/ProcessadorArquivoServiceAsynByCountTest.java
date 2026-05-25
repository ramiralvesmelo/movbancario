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
import br.gov.pb.receita.sefaz.arr.movbancario.repository.MovimentoBancarioRedisRepository;
import br.gov.pb.receita.sefaz.arr.movbancario.service.ProcessadorArquivoService;

public class ProcessadorArquivoServiceAsynByCountTest {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessadorArquivoServiceAsynByCountTest.class);

	/*
	 * Quantidade fixa threads.
	 */
	private static final int THREADS = 15;

	private ProcessadorArquivoService service;

	private MovimentoBancarioRedisRepository redisRepository;

	@Test
	public void processar() throws Exception {

		final File diretorio = new File("src/test/resources/cnab");

		/*
		 * Lista arquivos CNAB.
		 */
		final File[] arquivos = diretorio.listFiles((file) -> file.isFile() && file.getName().endsWith(".txt"));

		if (arquivos == null || arquivos.length == 0) {

			LOG.warn("Nenhum arquivo encontrado.");

			return;

		}

		LOG.info("==================================================");
		LOG.info("INICIO PROCESSAMENTO LOTE");
		LOG.info("Threads fixas: {}", THREADS);
		LOG.info("Quantidade arquivos: {}", arquivos.length);		
		LOG.info("Horario inicio: {}", new Date());
		LOG.info("==================================================");

		final long inicioLote = System.currentTimeMillis();

		/*
		 * Pool fixo threads.
		 */
		final ExecutorService executor = Executors.newFixedThreadPool(THREADS);

		/*
		 * Processamento arquivos.
		 */
		for (File arquivo : arquivos) {

			executor.submit(() -> {

				final long inicioArquivo = System.currentTimeMillis();

				final String nomeThread = Thread.currentThread().getName();

				LOG.info("--------------------------------------------------");
				LOG.info("THREAD: {}", nomeThread);
				LOG.info("INICIANDO ARQUIVO: {}", arquivo.getName());
				LOG.info("Tamanho: {} bytes", arquivo.length());
				LOG.info("Horario inicio: {}", new Date());

				try {

					/*
					 * Executa processamento real.
					 */
					service.processar(arquivo);

					final long tempoArquivo = System.currentTimeMillis() - inicioArquivo;

					LOG.info("STATUS: SUCESSO");
					LOG.info("Arquivo: {}", arquivo.getName());
					LOG.info("THREAD: {}", nomeThread);
					LOG.info("Tempo total: {} ms", tempoArquivo);
					LOG.info("Tempo total: {} segundos", (tempoArquivo / 1000));

				} catch (Exception e) {

					LOG.error("STATUS: ERRO");
					LOG.error("Arquivo: {}", arquivo.getName());
					LOG.error("THREAD: {}", nomeThread);
					LOG.error("Erro processamento", e);

				}

				LOG.info("FIM ARQUIVO: {}", arquivo.getName());
				LOG.info("THREAD: {}", nomeThread);
				LOG.info("--------------------------------------------------");

			});

		}

		/*
		 * Finaliza pool.
		 */
		executor.shutdown();

		/*
		 * Aguarda execução.
		 */
		while (!executor.awaitTermination(1, TimeUnit.SECONDS)) {

			LOG.info("Threads ativas executando...");

		}

		final long tempoLote = System.currentTimeMillis() - inicioLote;

		LOG.info("==================================================");
		LOG.info("FIM PROCESSAMENTO LOTE");
		LOG.info("Quantidade arquivos: {}", arquivos.length);
		LOG.info("Threads utilizadas: {}", THREADS);
		LOG.info("Tempo total lote: {} ms", tempoLote);
		LOG.info("Tempo total lote: {} segundos", (tempoLote / 1000));
		LOG.info("Horario fim: {}", new Date());
		LOG.info("==================================================");

	}
	
	@Before
	public void before() throws Exception {

		LOG.info("Inicializando cenário de teste multi-thread.");

		service = ProcessadorArquivoFactory.criarService();

		redisRepository = service.redisRepository;

		LOG.info("Service inicializado com sucesso.");

	}

	//@After
	public void after() throws Exception {

		LOG.info("Iniciando limpeza Redis.");

		final File diretorio = new File("src/test/resources/cnab");

		/*
		 * Lista arquivos CNAB.
		 */
		final File[] arquivos = diretorio.listFiles((file) -> file.isFile() && file.getName().endsWith(".txt"));

		if (arquivos == null || arquivos.length == 0) {

			LOG.warn("Nenhum arquivo encontrado para limpeza.");

			return;

		}

		/*
		 * Remove chaves Redis.
		 */
		for (File arquivo : arquivos) {

			try {

				LOG.info("Removendo chaves Redis arquivo={}", arquivo.getName());

				redisRepository.removerArquivoValido(arquivo.getName());

				redisRepository.removerArquivoInvalido(arquivo.getName());

			} catch (Exception e) {

				LOG.error("Erro limpeza Redis arquivo={}", arquivo.getName(), e);

			}

		}

		LOG.info("Finalizada limpeza Redis.");

	}	

}