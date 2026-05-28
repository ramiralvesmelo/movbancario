package br.gov.pb.receita.sefaz.arr.movbancario.model.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import br.gov.pb.receita.sefaz.arr.movbancario.model.entity.RegistroCnab;
import br.gov.pb.receita.sefaz.arr.movbancario.model.repository.MovimentoBancarioH2Repository;
import br.gov.pb.receita.sefaz.arr.movbancario.model.repository.MovimentoBancarioRedisRepository;
import br.gov.pb.receita.sefaz.arr.movbancario.model.transformador.TransformadorCnab240;
import br.gov.pb.receita.sefaz.arr.movbancario.model.validador.ValidadorCnab240;
import br.gov.pb.receita.sefaz.util.io.leitor.LeitorArquivoStream;
import lombok.extern.jbosslog.JBossLog;

/**
 * Serviço responsável pelo processamento streaming
 * dos arquivos CNAB do movimento bancário.
 *
 * Fluxo:
 *
 * ETAPA 01
 * - leitura streaming
 * - validação CNAB
 * - persistência temporária Redis
 *
 * ETAPA 02
 * - recuperação batch Redis
 * - transformação DTO
 *
 * ETAPA 03
 * - persistência JDBC batch
 *
 * ETAPA 04
 * - finalização processamento
 */
@Stateless
@JBossLog
public class ProcessadorArquivoService {

	/*
	 * Quantidade registros por bloco.
	 */
	private static final int BATCH_SIZE = 10000;

	@Inject
	private MovimentoBancarioH2Repository repository;

	@Inject
	private MovimentoBancarioRedisRepository redisRepository;

	private final ValidadorCnab240 validador = new ValidadorCnab240();

	private final TransformadorCnab240 transformador = new TransformadorCnab240();

	/**
	 * Executa processamento completo arquivo.
	 */
	public void processar(RegistroCnab entrada) throws Exception {

		log.infof("Iniciando processamento arquivo: %s", entrada.getDocumento());

		final File arquivo = new File(entrada.getCaminhoArquivo());

		/*
		 * ETAPA 01
		 */
		this.processarCargaRedis(arquivo);

		/*
		 * ETAPA 02 + 03
		 */
		this.processarCargaBanco(arquivo);

		/*
		 * ETAPA 04
		 */
		this.finalizarProcessamento(arquivo);

	}

	/**
	 * =====================================================================================
	 * ETAPA 01
	 *
	 * leitura streaming
	 * validação layout
	 * persistência temporária Redis
	 * =====================================================================================
	 */
	private void processarCargaRedis(File arquivo) throws Exception {

		final List<String> blocoRedis = new ArrayList<>();

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
				 * Adiciona válidos.
				 */
				blocoRedis.add(linha);

				/*
				 * Persistência batch Redis.
				 */
				if (blocoRedis.size() >= BATCH_SIZE) {

					persistirBlocoRedis(arquivo, blocoRedis);

				}

			} catch (Exception e) {

				processarRegistroInvalido(arquivo, linha, numeroLinha, e);

			}

		});

		/*
		 * Persistência saldo final.
		 */
		if (!blocoRedis.isEmpty()) {

			persistirBlocoRedis(arquivo, blocoRedis);

		}

		log.infof("Carga Redis finalizada: %s", arquivo.getName());

	}

	/**
	 * =====================================================================================
	 * ETAPA 02 + 03
	 *
	 * leitura Redis
	 * transformação DTO
	 * persistência JDBC batch
	 * =====================================================================================
	 */
	private void processarCargaBanco(File arquivo) throws Exception {

		long bloco = 0;

		while (true) {

			final List<String> linhas = redisRepository.obterBlocoValido(arquivo.getName(), bloco, BATCH_SIZE);

			/*
			 * Finaliza processamento.
			 */
			if (linhas.isEmpty()) {

				break;

			}

			final List<RegistroCnab> lote = transformarLote(linhas, bloco);

			/*
			 * Persistência JDBC batch.
			 */
			repository.salvarLote(lote);

			log.infof("Bloco persistido banco. bloco=%d registros=%d", bloco, lote.size());

			bloco++;

		}

	}

	/**
	 * Transforma linhas em DTO.
	 */
	private List<RegistroCnab> transformarLote(List<String> linhas, long bloco) throws Exception {

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

			final RegistroCnab registro = transformador.transformar(linha, numeroLinha);

			lote.add(registro);

		}

		return lote;

	}

	/**
	 * Persistência bloco Redis.
	 */
	private void persistirBlocoRedis(File arquivo, List<String> blocoRedis) throws Exception {

		redisRepository.salvarBlocoValido(arquivo.getName(), blocoRedis);

		log.infof("Bloco Redis persistido. registros=%d", blocoRedis.size());

		blocoRedis.clear();

	}

	/**
	 * Processa registros inválidos.
	 */
	private void processarRegistroInvalido(
			File arquivo,
			String linha,
			long numeroLinha,
			Exception e)
			throws Exception {

		final List<String> invalidos = new ArrayList<>();

		invalidos.add(linha + "|ERRO=" + e.getMessage());

		redisRepository.salvarBlocoInvalido(
				arquivo.getName(),
				invalidos);

		log.errorf(e, "Registro inválido. linha=%d", numeroLinha);

	}

	/**
	 * =====================================================================================
	 * ETAPA 04
	 *
	 * finalização processamento
	 * =====================================================================================
	 */
	private void finalizarProcessamento(
			File arquivo) {

		log.infof("Processamento finalizado: %s", arquivo.getName());

	}

}