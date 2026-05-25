package br.gov.pb.receita.sefaz.arr.movbancario.repository;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;
import javax.ejb.Stateless;

import org.jboss.logging.Logger;

import br.gov.pb.receita.sefaz.util.io.jdbc.JdbcUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Stateless
public class MovimentoBancarioRedisRepository {

	private static final Logger LOGGER = Logger.getLogger(MovimentoBancarioRedisRepository.class);

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
	private final JedisPool jedisPool = new JedisPool("localhost", 6379);

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
	 *
	 * Estratégia:
	 *
	 * LIST mantém ordenação CNAB
	 *
	 * SET controle evita duplicidade
	 */
	private void salvarBloco(String chaveLista, String chaveControle, List<String> linhas) throws Exception {

		try (Jedis jedis = jedisPool.getResource()) {

			long adicionados = 0;

			for (String linha : linhas) {

				/*
				 * Verifica duplicidade.
				 */
				boolean existe = jedis.sismember(chaveControle, linha);

				/*
				 * Ignora registros duplicados.
				 */
				if (existe) {

					LOGGER.warn("Registro duplicado ignorado.");

					continue;

				}

				/*
				 * Mantém ordem do arquivo.
				 */
				jedis.rpush(chaveLista, linha);

				/*
				 * Controle duplicidade.
				 */
				jedis.sadd(chaveControle, linha);

				adicionados++;

			}

			/*
			 * Expiração chaves Redis.
			 */
			jedis.expire(chaveLista, TTL_SEGUNDOS);

			jedis.expire(chaveControle, TTL_SEGUNDOS);

			LOGGER.info("Bloco Redis persistido. " + "chave=" + chaveLista + " adicionados=" + adicionados
					+ " recebidos=" + linhas.size());

		} catch (Exception e) {

			LOGGER.error("Erro ao salvar bloco Redis", e);

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

			LOGGER.info(
					"Bloco Redis recuperado. " + "chave=" + chave + " bloco=" + bloco + " quantidade=" + linhas.size());

			return new ArrayList<>(linhas);

		} catch (Exception e) {

			LOGGER.error("Erro ao recuperar bloco Redis", e);

			throw e;

		}

	}

	/*
	 * Quantidade registros válidos.
	 */
	public long quantidadeLinhasValidas(String nomeArquivo) throws Exception {

		return quantidadeLinhas(montarChaveValida(nomeArquivo));

	}

	/*
	 * Quantidade registros inválidos.
	 */
	public long quantidadeLinhasInvalidas(String nomeArquivo) throws Exception {

		return quantidadeLinhas(montarChaveInvalida(nomeArquivo));

	}

	/*
	 * Quantidade linhas Redis.
	 */
	private long quantidadeLinhas(String chave) throws Exception {

		try (Jedis jedis = jedisPool.getResource()) {

			return jedis.llen(chave);

		} catch (Exception e) {

			LOGGER.error("Erro ao consultar quantidade Redis", e);

			throw e;

		}

	}

	/*
	 * Remove registros válidos.
	 */
	public void removerArquivoValido(String nomeArquivo) throws Exception {

		removerArquivo(montarChaveValida(nomeArquivo));

		removerArquivo(montarChaveControle(nomeArquivo));

	}

	/*
	 * Remove registros inválidos.
	 */
	public void removerArquivoInvalido(String nomeArquivo) throws Exception {

		removerArquivo(montarChaveInvalida(nomeArquivo));

	}

	/*
	 * Remove chave Redis.
	 */
	private void removerArquivo(String chave) throws Exception {

		try (Jedis jedis = jedisPool.getResource()) {

			jedis.del(chave);

			LOGGER.info("Arquivo Redis removido: " + chave);

		} catch (Exception e) {

			LOGGER.error("Erro ao remover arquivo Redis", e);

			throw e;

		}

	}

	/*
	 * Monta chave válida Redis.
	 */
	private String montarChaveValida(String nomeArquivo) {

		return PREFIXO_VALIDO + nomeArquivo;

	}

	/*
	 * Monta chave inválida Redis.
	 */
	private String montarChaveInvalida(String nomeArquivo) {

		return PREFIXO_INVALIDO + nomeArquivo;

	}

	/*
	 * Monta chave controle duplicidade.
	 */
	private String montarChaveControle(String nomeArquivo) {

		return PREFIXO_CONTROLE + nomeArquivo;

	}

	/*
	 * Finaliza pool Redis.
	 */
	@PreDestroy
	public void destroy() {

		JdbcUtils.close(jedisPool);

	}

}