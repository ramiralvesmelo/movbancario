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

@Stateless
@JBossLog
public class ProcessadorArquivoService {

	private static final int BATCH_SIZE = 10000;

	@Inject
	private MovimentoBancarioH2Repository repository;

	@Inject
	private MovimentoBancarioRedisRepository redisRepository;

	private final ValidadorCnab240 validador = new ValidadorCnab240();

	private final TransformadorCnab240 transformador = new TransformadorCnab240();

	public void processar(RegistroCnab entrada) throws Exception {

		final File arquivo = new File(entrada.getCaminhoArquivo());

		log.infof("Iniciando processamento arquivo=%s", arquivo.getName());

		if (!redisRepository.registrarArquivo(arquivo.getName())) {

			log.warnf("Arquivo já processado anteriormente: %s", arquivo.getName());

			return;

		}

		try {

			/*
			 * ETAPA 01
			 */
			processarCargaRedis(arquivo);

			/*
			 * ETAPA 02 + 03
			 */
			processarCargaBanco(arquivo);

			/*
			 * ETAPA 04
			 */
			finalizarProcessamento(arquivo);

		} catch (Exception e) {

			log.errorf(e, "Erro processando arquivo=%s", arquivo.getName());

			throw e;

		}

	}

	private void processarCargaRedis(File arquivo) throws Exception {

		final List<String> blocoRedis = new ArrayList<>();

		LeitorArquivoStream.processar(arquivo, (linha, numeroLinha) -> {

			try {

				/*
				 * Ignora header.
				 */
				if (numeroLinha == 1) {

					return;

				}

				validador.validar(linha, numeroLinha);

				blocoRedis.add(linha);

				if (blocoRedis.size() >= BATCH_SIZE) {

					processarRegistroValido(arquivo, blocoRedis);

				}

			} catch (Exception e) {

				processarRegistroInvalido(arquivo, linha, numeroLinha, e);

			}

		});

		if (!blocoRedis.isEmpty()) {

			processarRegistroValido(arquivo, blocoRedis);

		}

		log.infof("Carga Redis finalizada arquivo=%s", arquivo.getName());

	}

	private void processarCargaBanco(File arquivo) throws Exception {

		long bloco = 0;

		while (true) {

			final List<String> linhas = redisRepository.obterBlocoValido(arquivo.getName(), bloco, BATCH_SIZE);

			if (linhas == null || linhas.isEmpty()) {

				break;

			}

			final List<RegistroCnab> lote = transformarLote(linhas, bloco);

			if (!lote.isEmpty()) {

				repository.salvarLote(lote);

			}

			log.infof("Bloco persistido banco. arquivo=%s bloco=%d registros=%d", arquivo.getName(), bloco,
					lote.size());

			bloco++;

		}

	}

	private List<RegistroCnab> transformarLote(List<String> linhas, long bloco) throws Exception {

		final List<RegistroCnab> lote = new ArrayList<>();

		long numeroLinha = (bloco * BATCH_SIZE) + 1;

		for (String linha : linhas) {

			numeroLinha++;

			final RegistroCnab registro = transformador.transformar(linha, numeroLinha);

			lote.add(registro);

		}

		return lote;

	}

	private void processarRegistroValido(File arquivo, List<String> blocoRedis) throws Exception {

		if (blocoRedis.isEmpty()) {

			return;

		}

		redisRepository.salvarBlocoValido(arquivo.getName(), blocoRedis);

		log.infof("Bloco Redis persistido. arquivo=%s registros=%d", arquivo.getName(), blocoRedis.size());

		blocoRedis.clear();

	}

	private void processarRegistroInvalido(File arquivo, String linha, long numeroLinha, Exception e) throws Exception {

		final List<String> invalidos = new ArrayList<>(1);

		invalidos.add(linha + "|LINHA=" + numeroLinha + "|ERRO=" + e.getMessage());

		redisRepository.salvarBlocoInvalido(arquivo.getName(), invalidos);

		log.errorf(e, "Registro inválido. arquivo=%s linha=%d", arquivo.getName(), numeroLinha);

	}

	private void finalizarProcessamento(File arquivo) {

		log.infof("Processamento finalizado arquivo=%s", arquivo.getName());

	}

}