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
 * Controle de execução única
 *      ↓
 * Leitura streaming do arquivo
 *      ↓
 * Normalização da linha
 *      ↓
 * Validação estrutural/layout
 *      ↓
 * Controle de duplicidade
 *      ↓
 * Separação lógica:
 *      - registros válidos
 *      - registros inválidos
 *      ↓
 * Persistência temporária Redis
 *      ↓
 * Recuperação batch dos registros válidos
 *      ↓
 * Transformação para DTO
 *      ↓
 * Persistência JDBC batch
 *      ↓
 * Commit transacional por lote
 *      ↓
 * Atualização checkpoint/log operacional
 *      ↓
 * Limpeza recursos temporários Redis
 *      ↓
 * Finalização
 *
 * Estratégia operacional:
 *
 * - processamento streaming
 * - baixo consumo memória
 * - fail-fast para registros inválidos
 * - tolerância a falhas
 * - rastreabilidade operacional
 * - prevenção de duplicidade
 * - persistência temporária Redis
 * - separação de registros inválidos
 * - processamento batch JDBC
 * - preservação da ordem posicional CNAB
 * - redução de consumo heap/JVM
 *
 * Caso o registro já exista:
 *
 * - a linha é ignorada
 * - não é persistida novamente
 * - evita reprocessamento duplicado
 * - preserva integridade operacional
 *
 * Benefícios:
 *
 * - reprocessamento controlado
 * - troubleshooting operacional
 * - observabilidade
 * - resiliência do ETL
 * - desacoplamento entre leitura e persistência
 * - prevenção de inconsistência por duplicidade
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

	ValidadorCnab240 validador = new ValidadorCnab240();

	TransformadorCnab240 transformador = new TransformadorCnab240();

	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void processar(File arquivo) throws Exception {

		LOGGER.info("Iniciando processamento arquivo: " + arquivo.getName());

		/*
		 * Bloco temporário Redis.
		 */
		List<String> blocoRedis = new ArrayList<>();

		/*
		 * Etapa 1: Leitura streaming + validação inicial + persistência Redis.
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
				 *
				 * Objetivo: - impedir persistência inválida - evitar lixo
				 * operacional no Redis - detectar erro o mais cedo possível -
				 * reduzir retrabalho
				 */
				validador.validar(linha, numeroLinha);

				/*
				 * Adiciona somente registros válidos.
				 */
				blocoRedis.add(linha);

				/*
				 * Persistência Redis batch.
				 */
				if (blocoRedis.size() >= BATCH_SIZE) {

					redisRepository.salvarBlocoValido(arquivo.getName(), blocoRedis);

					LOGGER.info("Bloco Redis persistido: " + blocoRedis.size());

					blocoRedis.clear();

				}

			} catch (Exception e) {

				/*
				 * Persistência registros inválidos.
				 */
				List<String> invalidos = new ArrayList<>();

				invalidos.add(linha + "|ERRO=" + e.getMessage());

				redisRepository.salvarBlocoInvalido(arquivo.getName(), invalidos);

				LOGGER.error("Registro inválido. " + "linha=" + numeroLinha, e);

			}

		});

		/*
		 * Saldo final Redis.
		 */
		if (!blocoRedis.isEmpty()) {

			try {

				redisRepository.salvarBlocoValido(arquivo.getName(), blocoRedis);

				LOGGER.info("Saldo final Redis persistido. " + "arquivo=" + arquivo.getName() + " registros="
						+ blocoRedis.size());

			} finally {

				blocoRedis.clear();

			}

		}

		LOGGER.info("Carga Redis finalizada: " + arquivo.getName());

		/*
		 * Etapa 2: Recuperação Redis em bloco.
		 */
		long bloco = 0;

		while (true) {

			List<String> linhas = redisRepository.obterBlocoValido(arquivo.getName(), bloco, BATCH_SIZE);

			/*
			 * Finaliza processamento.
			 */
			if (linhas.isEmpty()) {
				break;
			}

			List<RegistroCnab> lote = new ArrayList<>();

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
				RegistroCnab registro = transformador.transformar(linha, numeroLinha);

				lote.add(registro);

			}

			/*
			 * Persistência banco batch.
			 */
			repository.salvarLote(lote);

			LOGGER.info("Bloco persistido banco. " + "bloco=" + bloco + " registros=" + lote.size());

			bloco++;

		}

		LOGGER.info("Processamento finalizado: " + arquivo.getName());

	}

}