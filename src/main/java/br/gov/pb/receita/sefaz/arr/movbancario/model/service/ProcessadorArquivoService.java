package br.gov.pb.receita.sefaz.arr.movbancario.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;

import org.jboss.logging.Logger;

import br.gov.pb.receita.sefaz.arr.movbancario.dto.RegistroCnab;
import br.gov.pb.receita.sefaz.arr.movbancario.repository.MovimentoBancarioH2Repository;
import br.gov.pb.receita.sefaz.arr.movbancario.repository.MovimentoBancarioRedisRepository;
import br.gov.pb.receita.sefaz.arr.movbancario.transformador.TransformadorCnab240;
import br.gov.pb.receita.sefaz.arr.movbancario.validador.ValidadorCnab240;
import br.gov.pb.receita.sefaz.util.io.leitor.LeitorArquivoStream;

/**
 * Serviço responsável pelo processamento
 * streaming dos arquivos CNAB do
 * movimento bancário.
 *
 * Fluxo operacional:
 *
 * Agendador
 *      ↓
 * Controle execução única
 *      ↓
 * Leitura streaming
 *      ↓
 * Validação layout
 *      ↓
 * Persistência temporária Redis
 *      ↓
 * Recuperação batch Redis
 *      ↓
 * Transformação DTO
 *      ↓
 * Persistência JDBC batch
 *      ↓
 * Finalização
 *
 * Estratégia operacional:
 *
 * - processamento streaming
 * - baixo consumo memória
 * - fail-fast registros inválidos
 * - persistência temporária Redis
 * - processamento batch JDBC
 * - rastreabilidade operacional
 * - preservação ordem CNAB
 * - tolerância falhas
 *
 * Benefícios:
 *
 * - desacoplamento leitura/persistência
 * - melhoria throughput ETL
 * - reprocessamento controlado
 * - observabilidade operacional
 * - redução consumo heap JVM
 */
@Stateless
public class ProcessadorArquivoService {

	private static final Logger LOGGER = Logger.getLogger(ProcessadorArquivoService.class);

	/*
	 * Tamanho padrão processamento.
	 */
	private static final int BATCH_SIZE = 10000;

	@EJB
	public MovimentoBancarioH2Repository repository;

	@EJB
	public MovimentoBancarioRedisRepository redisRepository;

	private final ValidadorCnab240 validador = new ValidadorCnab240();

	private final TransformadorCnab240 transformador = new TransformadorCnab240();

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void processar(File arquivo) throws Exception {

		LOGGER.infof("Iniciando processamento arquivo: %s", arquivo.getName());

		/*
		 * Bloco temporário Redis.
		 */
		final List<String> blocoRedis = new ArrayList<>();

		/**
		 * =============================================================================================== 
		 * ETAPA 01 - l leitura streaming + file system lock > validação CNAB > carga temporária Redis
		 * ===============================================================================================		 
		 */
		LeitorArquivoStream.processar(arquivo, (linha, numeroLinha) -> {

			try {

				/*
				 * Ignora header.
				 */
				if (numeroLinha == 1) {

					blocoRedis.add(linha);

					return;

				}

				/*
				 * Validação antecipada.
				 */
				validador.validar(linha, numeroLinha);

				/*
				 * Adiciona somente válidos.
				 */
				blocoRedis.add(linha);

				/*
				 * Persistência batch Redis.
				 */
				if (blocoRedis.size() >= BATCH_SIZE) {

					redisRepository.salvarBlocoValido(arquivo.getName(), blocoRedis);

					LOGGER.infof("Bloco Redis persistido. registros=%d", blocoRedis.size());

					blocoRedis.clear();

				}

			} catch (Exception e) {

				/*
				 * Persistência inválidos.
				 */
				final List<String> invalidos = new ArrayList<>();

				invalidos.add(linha + "|ERRO=" + e.getMessage());

				redisRepository.salvarBlocoInvalido(arquivo.getName(), invalidos);

				LOGGER.errorf(e, "Registro inválido. linha=%d", numeroLinha);

			}

		});

		/*
		 * Saldo final Redis.
		 */
		if (!blocoRedis.isEmpty()) {

			try {

				redisRepository.salvarBlocoValido(arquivo.getName(), blocoRedis);

				LOGGER.infof("Saldo final Redis persistido. registros=%d", blocoRedis.size());

			} finally {

				blocoRedis.clear();

			}

		}

		LOGGER.infof("Carga Redis finalizada: %s", arquivo.getName());

		/**
		 * =============================================================================================== 
		 * ETAPA 02 - Extração Redis > transformação DTO 
		 * ===============================================================================================		 
		 *  
		 */
		long bloco = 0;

		while (true) {

			final List<String> linhas = redisRepository.obterBlocoValido(arquivo.getName(), bloco, BATCH_SIZE);

			/*
			 * Finaliza processamento.
			 */
			if (linhas.isEmpty()) {

				break;

			}

			final List<RegistroCnab> lote = new ArrayList<>();

			long numeroLinha = (bloco * BATCH_SIZE);

			for (String linha : linhas) {

				numeroLinha++;

				/*
				 * Ignora header.
				 */
				if (numeroLinha == 1) {

					continue;

				}

				/*
				 * Transformação DTO.
				 */
				final RegistroCnab registro = transformador.transformar(linha, numeroLinha);

				lote.add(registro);

			}

			/**
			 * =============================================================================================== 
			 *  ETAPA 03 - Persistência no banco com JDBC Batch (em bloco)
			 * ===============================================================================================
			 */
			repository.salvarLote(lote);

			LOGGER.infof("Bloco persistido banco. bloco=%d registros=%d", bloco, lote.size());

			bloco++;

		}

		/**
		 * ===============================================================================================
		 * 	ETAPA 04 - FINALIZAÇÃO + LIMPEZA 
		 * ===============================================================================================
		 *
		 * pós-processamento > limpeza recursos > finalização ETL
		 */
		LOGGER.infof("Processamento finalizado: %s", arquivo.getName());

	}

}