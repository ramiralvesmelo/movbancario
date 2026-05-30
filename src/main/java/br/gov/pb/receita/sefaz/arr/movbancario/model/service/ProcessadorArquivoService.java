package br.gov.pb.receita.sefaz.arr.movbancario.model.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.ejb.Stateless;
import javax.inject.Inject;

import br.gov.pb.receita.sefaz.arr.movbancario.model.entity.RegistroCnab;
import br.gov.pb.receita.sefaz.arr.movbancario.model.repository.ControleArquivoRedisRepository;
import br.gov.pb.receita.sefaz.arr.movbancario.model.repository.MovimentoBancarioH2Repository;
import br.gov.pb.receita.sefaz.arr.movbancario.model.transformador.TransformadorCnab240;
import br.gov.pb.receita.sefaz.arr.movbancario.model.validador.ValidadorCnab240;
import br.gov.pb.receita.sefaz.util.io.leitor.LeitorArquivoStream;
import lombok.extern.jbosslog.JBossLog;

@Stateless
@JBossLog
public class ProcessadorArquivoService {

	private static final int BATCH_SIZE = 50;

	@Inject
	private MovimentoBancarioH2Repository movimentoBancarioH2Repository;

	@Inject
	private ControleArquivoRedisRepository controleArquivoRedisRepository;

	private final ValidadorCnab240 validador = new ValidadorCnab240();

	private final TransformadorCnab240 transformador = new TransformadorCnab240();

	public void processar(RegistroCnab entrada) throws Exception {

		final File arquivo = new File(entrada.getCaminhoArquivo());

		final long inicio = System.currentTimeMillis();

		final long[] total = { 0L };

		final long[] validos = { 0L };

		final long[] invalidos = { 0L };

		log.infof("Iniciando processamento arquivo=%s", arquivo.getName());

		if (!controleArquivoRedisRepository.iniciarProcessamento(arquivo.getName())) {
			log.warnf("Arquivo já processado anteriormente: %s", arquivo.getName());
			return;
		}

		try {

			processarArquivoParaRedis(arquivo, total, validos, invalidos);

			persistirRedisNoBanco(arquivo);

			controleArquivoRedisRepository.finalizarProcessamento(arquivo.getName(), total[0], validos[0],
					invalidos[0]);

			finalizarProcessamento(arquivo, inicio, total[0], validos[0], invalidos[0]);

		} catch (Exception e) {

			try {

				controleArquivoRedisRepository.registrarErro(arquivo.getName(), e);

			} catch (Exception ex) {

				log.error("Erro ao registrar falha no Redis", ex);

			}

			throw e;

		}

	}

	private void processarArquivoParaRedis(final File arquivo, final long[] total, final long[] validos,
			final long[] invalidos) throws Exception {

		final List<String> blocoRedis = new ArrayList<>(BATCH_SIZE);

		try {

			LeitorArquivoStream.processar(arquivo, (linha, numeroLinha) -> {

				total[0]++;

				try {

					validador.validar(linha, numeroLinha);

					blocoRedis.add(linha);

					validos[0]++;

					if (blocoRedis.size() >= BATCH_SIZE) {

						controleArquivoRedisRepository.salvarBlocoValido(arquivo.getName(),
								new ArrayList<>(blocoRedis));

						controleArquivoRedisRepository.atualizarContadores(arquivo.getName(), total[0], validos[0],
								invalidos[0]);

						blocoRedis.clear();

					}

				} catch (Exception e) {

					invalidos[0]++;

					List<String> erros = new ArrayList<>(1);

					erros.add(linha + "|ERRO=" + tratarMensagemErro(e));

					controleArquivoRedisRepository.salvarBlocoInvalido(arquivo.getName(), erros);

				}

			});

			/*
			 * Persiste último bloco pendente.
			 */
			if (!blocoRedis.isEmpty()) {

				controleArquivoRedisRepository.salvarBlocoValido(arquivo.getName(), new ArrayList<>(blocoRedis));

			}

			controleArquivoRedisRepository.atualizarContadores(arquivo.getName(), total[0], validos[0], invalidos[0]);

		} finally {
			/*
			 * Remove lock físico caso exista.
			 */			
			LeitorArquivoStream.removerLock(arquivo);

		}

	}

	private void persistirRedisNoBanco(File arquivo) throws Exception {

		List<String> linhasValidas = controleArquivoRedisRepository.recuperarRegistrosValidos(arquivo.getName());

		if (linhasValidas == null || linhasValidas.isEmpty()) {

			log.warnf("Nenhum registro válido encontrado no Redis. arquivo=%s", arquivo.getName());

			return;

		}

		List<RegistroCnab> lote = new ArrayList<>(BATCH_SIZE);

		long numeroLinha = 0L;

		for (String linha : linhasValidas) {

			numeroLinha++;

			RegistroCnab registro = transformador.transformar(linha, numeroLinha);

			lote.add(registro);

			if (lote.size() >= BATCH_SIZE) {

				movimentoBancarioH2Repository.salvarLote(new ArrayList<>(lote));

				lote.clear();

			}

		}

		if (!lote.isEmpty()) {

			movimentoBancarioH2Repository.salvarLote(lote);

		}

		log.infof("Persistência H2 concluída. arquivo=%s registros=%d", arquivo.getName(), linhasValidas.size());

	}

	private String tratarMensagemErro(Exception e) {

		if (e == null) {

			return "Erro desconhecido";

		}

		if (e.getMessage() == null || e.getMessage().trim().isEmpty()) {

			return e.getClass().getSimpleName();

		}

		return e.getMessage();

	}

	private void finalizarProcessamento(File arquivo, long inicio, long total, long validos, long invalidos) {

		long tempo = System.currentTimeMillis() - inicio;

		log.infof("Arquivo=%s Total=%d Validos=%d Invalidos=%d Tempo=%d ms", arquivo.getName(), total, validos,
				invalidos, tempo);

	}

}