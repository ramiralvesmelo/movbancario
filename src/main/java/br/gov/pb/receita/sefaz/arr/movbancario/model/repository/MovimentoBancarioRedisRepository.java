package br.gov.pb.receita.sefaz.arr.movbancario.model.repository;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;

import br.gov.pb.receita.sefaz.util.io.jdbc.JdbcUtils;
import br.gov.pb.receita.sefaz.util.io.jdbc.JedisPoolFactory;
import lombok.extern.jbosslog.JBossLog;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Stateless
@JBossLog
public class MovimentoBancarioRedisRepository {

	/*
	 * Prefixo registros válidos.
	 */
	private static final String PREFIXO_VALIDO = "movbancario:valido:";

	/*
	 * Prefixo registros inválidos.
	 */
	private static final String PREFIXO_INVALIDO = "movbancario:invalido:";

	/*
	 * Prefixo controle duplicidade.
	 */
	private static final String PREFIXO_CONTROLE = "movbancario:controle:";

	/*
	 * TTL padrão Redis: 1 hora.
	 */
	private static final int TTL_SEGUNDOS = 3600;

	/*
	 * Pool conexões Redis.
	 */
	private JedisPool jedisPool;

	@PostConstruct
	public void init() {

		try {

			log.info("Inicializando Redis...");

			jedisPool = JedisPoolFactory.criarPool();

			log.info("Redis inicializado com sucesso.");

		} catch (Exception e) {

			log.error("Erro ao inicializar Redis", e);

			throw new IllegalStateException("Falha ao inicializar Redis", e);

		}

	}

	/*
	 * Persiste bloco válido Redis.
	 */
	public void salvarBlocoValido(String nomeArquivo, List<String> linhas) throws Exception {

		salvarBloco(montarChaveValida(nomeArquivo), montarChaveControle(nomeArquivo), linhas);

	}

	/*
	 * Persiste bloco inválido Redis.
	 */
	public void salvarBlocoInvalido(String nomeArquivo, List<String> linhas) throws Exception {

		salvarBloco(montarChaveInvalida(nomeArquivo), montarChaveControle(nomeArquivo), linhas);

	}

	/*
	 * Persiste bloco Redis sem duplicidade.
	 */
	private void salvarBloco(String chaveLista, String chaveControle, List<String> linhas) throws Exception {

		try (Jedis jedis = jedisPool.getResource()) {

			long adicionados = 0;

			for (String linha : linhas) {

				boolean existe = jedis.sismember(chaveControle, linha);

				/*
				 * Ignora duplicados.
				 */
				if (existe) {

					log.warn("Registro duplicado ignorado.");

					continue;

				}

				/*
				 * Mantém ordem arquivo.
				 */
				jedis.rpush(chaveLista, linha);

				/*
				 * Controle duplicidade.
				 */
				jedis.sadd(chaveControle, linha);

				adicionados++;

			}

			/*
			 * Expiração chaves.
			 */
			jedis.expire(chaveLista, TTL_SEGUNDOS);

			jedis.expire(chaveControle, TTL_SEGUNDOS);

			log.infof("Bloco Redis persistido. chave=%s adicionados=%s recebidos=%s", chaveLista, adicionados,
					linhas.size());

		} catch (Exception e) {

			log.error("Erro ao salvar bloco Redis", e);

			throw e;

		}

	}

	/*
	 * Recupera bloco válido Redis.
	 */
	public List<String> obterBlocoValido(String nomeArquivo, long bloco, long tamanhoBloco) throws Exception {

		return obterBloco(montarChaveValida(nomeArquivo), bloco, tamanhoBloco);

	}

	/*
	 * Recupera bloco inválido Redis.
	 */
	public List<String> obterBlocoInvalido(String nomeArquivo, long bloco, long tamanhoBloco) throws Exception {

		return obterBloco(montarChaveInvalida(nomeArquivo), bloco, tamanhoBloco);

	}

	/*
	 * Recupera bloco Redis.
	 */
	private List<String> obterBloco(String chave, long bloco, long tamanhoBloco) throws Exception {

		long inicio = bloco * tamanhoBloco;

		long fim = inicio + tamanhoBloco - 1;

		try (Jedis jedis = jedisPool.getResource()) {

			List<String> linhas = jedis.lrange(chave, inicio, fim);

			log.infof("Bloco Redis recuperado. chave=%s bloco=%s quantidade=%s", chave, bloco, linhas.size());

			return new ArrayList<>(linhas);

		} catch (Exception e) {

			log.error("Erro ao recuperar bloco Redis", e);

			throw e;

		}

	}

	public boolean arquivoJaProcessado(String nomeArquivo) throws Exception {

		try (Jedis jedis = jedisPool.getResource()) {

			long quantidade = jedis.scard(montarChaveControle(nomeArquivo));

			return quantidade > 0;

		}

	}

	@PreDestroy
	public void destroy() {

		JdbcUtils.close(jedisPool);

		log.info("Pool Redis finalizado.");

	}

	private String montarChaveValida(String nomeArquivo) {

		return PREFIXO_VALIDO + nomeArquivo;

	}

	private String montarChaveInvalida(String nomeArquivo) {

		return PREFIXO_INVALIDO + nomeArquivo;

	}

	private String montarChaveControle(String nomeArquivo) {

		return PREFIXO_CONTROLE + nomeArquivo;

	}

}