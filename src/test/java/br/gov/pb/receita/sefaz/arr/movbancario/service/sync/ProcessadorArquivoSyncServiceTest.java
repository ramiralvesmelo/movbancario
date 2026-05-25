package br.gov.pb.receita.sefaz.arr.movbancario.service.sync;

import java.io.File;
import java.util.Date;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import br.gov.pb.receita.sefaz.arr.movbancario.repository.MovimentoBancarioRedisRepository;
import br.gov.pb.receita.sefaz.arr.movbancario.service.ProcessadorArquivoService;
import br.gov.pb.receita.sefaz.arr.movbancario.service.factory.ProcessadorArquivoFactory;

public class ProcessadorArquivoSyncServiceTest {

	private static final Logger LOG = LoggerFactory.getLogger(ProcessadorArquivoSyncServiceTest.class);

	private ProcessadorArquivoService service;

	private MovimentoBancarioRedisRepository redisRepository;

	@Before
	public void before() throws Exception {

		LOG.info("Inicializando cenário de teste.");

		service = ProcessadorArquivoFactory.criarService();

		/*
		 * Recupera repositório utilizado no service.
		 */
		redisRepository = service.redisRepository;

		LOG.info("Service inicializado com sucesso.");

	}

	@After
	public void after() throws Exception {

		LOG.info("Iniciando limpeza de chaves Redis.");

		final File diretorio = new File("src/test/resources/cnab");

		/*
		 * Lista arquivos CNAB.
		 */
		final File[] arquivos = diretorio.listFiles((file) -> file.isFile() && file.getName().endsWith(".txt"));

		if (arquivos == null || arquivos.length == 0) {

			LOG.warn("Nenhum arquivo encontrado para limpeza Redis.");

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
		LOG.info("Modo: SEQUENCIAL");
		LOG.info("Quantidade arquivos: {}", arquivos.length);
		LOG.info("Horario inicio: {}", new Date());
		LOG.info("==================================================");

		final long inicioLote = System.currentTimeMillis();

		long totalSucesso = 0;

		long totalErro = 0;

		/*
		 * Processamento sequencial.
		 */
		for (File arquivo : arquivos) {

			final long inicioArquivo = System.currentTimeMillis();

			LOG.info("--------------------------------------------------");
			LOG.info("INICIANDO ARQUIVO: {}", arquivo.getName());
			LOG.info("Tamanho: {} bytes", arquivo.length());
			LOG.info("Horario inicio: {}", new Date());

			try {

				/*
				 * Executa processamento real.
				 */
				service.processar(arquivo);

				totalSucesso++;

				final long tempoArquivo = System.currentTimeMillis() - inicioArquivo;

				LOG.info("STATUS: SUCESSO");
				LOG.info("Tempo total: {} ms", tempoArquivo);
				LOG.info("Tempo total: {} segundos", (tempoArquivo / 1000));

			} catch (Exception e) {

				totalErro++;

				LOG.error("STATUS: ERRO");
				LOG.error("Erro processamento arquivo={}", arquivo.getName(), e);

			}

			LOG.info("FIM ARQUIVO: {}", arquivo.getName());
			LOG.info("--------------------------------------------------");

		}

		final long tempoLote = System.currentTimeMillis() - inicioLote;

		LOG.info("==================================================");
		LOG.info("FIM PROCESSAMENTO LOTE");
		LOG.info("Modo: SEQUENCIAL");
		LOG.info("Quantidade arquivos: {}", arquivos.length);
		LOG.info("Arquivos sucesso: {}", totalSucesso);
		LOG.info("Arquivos erro: {}", totalErro);
		LOG.info("Tempo total lote: {} ms", tempoLote);
		LOG.info("Tempo total lote: {} segundos", (tempoLote / 1000));
		LOG.info("Horario fim: {}", new Date());
		LOG.info("==================================================");

	}

}