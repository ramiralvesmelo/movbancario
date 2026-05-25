package br.gov.pb.receita.sefaz.arr.movbancario.service.async;

import java.io.File;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.gov.pb.receita.sefaz.arr.movbancario.service.ProcessadorArquivoService;
import br.gov.pb.receita.sefaz.arr.movbancario.service.factory.ProcessadorArquivoFactory;

public class ProcessadorArquivoServiceAsyncByFileTest {

	private static final Logger LOG =
			LoggerFactory.getLogger(ProcessadorArquivoServiceAsyncByFileTest.class);

	private ProcessadorArquivoService service;

	// private MovimentoBancarioRedisRepository redisRepository;

	@Before
	public void before() throws Exception {

		LOG.info("Inicializando cenário de teste.");

		service =
				ProcessadorArquivoFactory.criarService();

		// redisRepository = service.redisRepository;

		LOG.info("Service inicializado com sucesso.");

	}

	@After
	public void after() throws Exception {

		LOG.info("Finalizando cenário de teste.");

		// redisRepository.removerArquivoValido("cnab240.txt");
		// redisRepository.removerArquivoInvalido("cnab240.txt");

	}

	@Test
	public void processar() throws Exception {

		final File arquivo =
				new File(
						"src/test/resources/cnab240-grande.txt");

		// final File arquivo =
		//      new File(
		//              "src/test/resources/cnab240.txt");

		final long inicio =
				System.currentTimeMillis();

		LOG.info("==================================================");
		LOG.info("INICIO PROCESSAMENTO");
		LOG.info("Arquivo: {}", arquivo.getName());
		LOG.info("Tamanho: {} bytes", arquivo.length());
		LOG.info("Thread: {}", Thread.currentThread().getName());
		LOG.info("Horario inicio: {}", new Date());
		LOG.info("==================================================");

		try {

			/*
			 * Executa processamento real.
			 */
			service.processar(arquivo);

			final long fim =
					System.currentTimeMillis();

			final long tempo =
					(fim - inicio);

			LOG.info("STATUS: SUCESSO");
			LOG.info("Arquivo: {}", arquivo.getName());
			LOG.info("Horario fim: {}", new Date());
			LOG.info("Tempo total: {} ms", tempo);
			LOG.info("Tempo total: {} segundos",
					(tempo / 1000));

		} catch (Exception e) {

			LOG.error("STATUS: ERRO");
			LOG.error("Arquivo: {}", arquivo.getName());
			LOG.error("Thread: {}", Thread.currentThread().getName());
			LOG.error("Erro processamento", e);

			throw e;

		}

		LOG.info("==================================================");
		LOG.info("FIM PROCESSAMENTO");
		LOG.info("Arquivo: {}", arquivo.getName());
		LOG.info("==================================================");

	}

}