package br.gov.pb.receita.sefaz.arr.movbancario.model.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Stateless;

import br.gov.pb.receita.sefaz.util.io.jdbc.JdbcUtils;
import br.gov.pb.receita.sefaz.util.io.leitor.YamlUtils;
import lombok.extern.jbosslog.JBossLog;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

@Stateless
@JBossLog
public class MovimentoBancarioRedisRepository {

	/*
	 * Arquivo configuração Redis.
	 */
	private static final String REDIS_CONFIG =
			"/var/atf/configuracoes/redis-conf.yaml";

	/*
	 * Prefixo registros válidos.
	 */
	private static final String PREFIXO_VALIDO =
			"movbancario:valido:";

	/*
	 * Prefixo registros inválidos.
	 */
	private static final String PREFIXO_INVALIDO =
			"movbancario:invalido:";

	/*
	 * Prefixo controle duplicidade.
	 */
	private static final String PREFIXO_CONTROLE =
			"movbancario:controle:";

	/*
	 * TTL padrão Redis.
	 */
	private static final int TTL_SEGUNDOS =
			3600;

	/*
	 * Timeout conexão Redis.
	 */
	private static final int TIMEOUT =
			10000;

	/*
	 * Pool conexões Redis.
	 */
	private JedisPool jedisPool;

	@PostConstruct
	public void init() {

		try {

			log.infof(
					"Inicializando Redis. config=%s",
					REDIS_CONFIG);

			/*
			 * Carrega YAML.
			 */
			Map<String, Object> dados =
					YamlUtils.load(REDIS_CONFIG);

			@SuppressWarnings("unchecked")
			Map<String, Object> singleServer =
					(Map<String, Object>) dados.get("singleServerConfig");

			/*
			 * Exemplo YAML:
			 *
			 * address:
			 *   - "//10.10.253.79:6379"
			 */
			@SuppressWarnings("unchecked")
			List<String> addresses =
					(List<String>) singleServer.get("address");

			if (addresses == null
					|| addresses.isEmpty()) {

				throw new IllegalStateException(
						"Endereco Redis nao configurado.");

			}

			/*
			 * Primeiro endereço configurado.
			 */
			String address =
					addresses.get(0);

			/*
			 * Remove prefixos.
			 */
			address =
					address
							.replace("redis://", "")
							.replace("rediss://", "")
							.replace("//", "")
							.trim();

			/*
			 * host:porta
			 */
			String[] hostPort =
					address.split(":");

			if (hostPort.length != 2) {

				throw new IllegalStateException(
						"Endereco Redis invalido: "
								+ address);

			}

			String host =
					hostPort[0];

			int port =
					Integer.parseInt(
							hostPort[1]);

			int database =
					Integer.parseInt(
							String.valueOf(
									singleServer.get("database")));

			int maxPool =
					Integer.parseInt(
							String.valueOf(
									singleServer.get("connectionPoolSize")));

			String password =
					String.valueOf(
							singleServer.get("password"));

			log.infof(
					"Configuracao Redis carregada. host=%s port=%d database=%d",
					host,
					port,
					database);

			/*
			 * Configuração pool Jedis.
			 */
			JedisPoolConfig poolConfig =
					new JedisPoolConfig();

			poolConfig.setMaxTotal(maxPool);

			poolConfig.setMaxIdle(maxPool);

			poolConfig.setMinIdle(5);

			poolConfig.setTestOnBorrow(true);

			poolConfig.setTestWhileIdle(true);

			poolConfig.setTestOnReturn(true);

			/*
			 * Inicializa pool Redis.
			 */
			if (password != null
					&& !"null".equals(password)
					&& !password.trim().isEmpty()) {

				jedisPool =
						new JedisPool(
								poolConfig,
								host,
								port,
								TIMEOUT,
								password,
								database);

			} else {

				jedisPool =
						new JedisPool(
								poolConfig,
								host,
								port,
								TIMEOUT,
								null,
								database);

			}

			log.infof(
					"Redis inicializado com sucesso. host=%s port=%d database=%d",
					host,
					port,
					database);

		} catch (Exception e) {

			log.error(
					"Erro ao inicializar Redis",
					e);

			throw new RuntimeException(e);

		}

	}

	/*
	 * Persiste bloco válido Redis.
	 */
	public void salvarBlocoValido(
			String nomeArquivo,
			List<String> linhas)
			throws Exception {

		salvarBloco(
				montarChaveValida(nomeArquivo),
				montarChaveControle(nomeArquivo),
				linhas);

	}

	/*
	 * Persiste bloco inválido Redis.
	 */
	public void salvarBlocoInvalido(
			String nomeArquivo,
			List<String> linhas)
			throws Exception {

		salvarBloco(
				montarChaveInvalida(nomeArquivo),
				montarChaveControle(nomeArquivo),
				linhas);

	}

	/*
	 * Persiste bloco Redis sem duplicidade.
	 */
	private void salvarBloco(
			String chaveLista,
			String chaveControle,
			List<String> linhas)
			throws Exception {

		if (linhas == null
				|| linhas.isEmpty()) {

			log.warnf(
					"Bloco vazio ignorado. chave=%s",
					chaveLista);

			return;

		}

		try (Jedis jedis =
					 jedisPool.getResource()) {

			long adicionados = 0;

			for (String linha : linhas) {

				/*
				 * SADD:
				 *
				 * 1 = adicionou
				 * 0 = duplicado
				 */
				Long added =
						jedis.sadd(
								chaveControle,
								linha);

				/*
				 * Ignora duplicados.
				 */
				if (added == 0) {

					log.warnf(
							"Registro duplicado ignorado. chave=%s",
							chaveControle);

					continue;

				}

				/*
				 * Mantém ordem arquivo.
				 */
				jedis.rpush(
						chaveLista,
						linha);

				adicionados++;

			}

			/*
			 * Expiração chaves.
			 */
			jedis.expire(
					chaveLista,
					TTL_SEGUNDOS);

			jedis.expire(
					chaveControle,
					TTL_SEGUNDOS);

			log.infof(
					"Bloco Redis persistido. chave=%s adicionados=%d recebidos=%d",
					chaveLista,
					adicionados,
					linhas.size());

		} catch (Exception e) {

			log.errorf(
					e,
					"Erro ao salvar bloco Redis. chave=%s",
					chaveLista);

			throw e;

		}

	}

	/*
	 * Recupera bloco válido Redis.
	 */
	public List<String> obterBlocoValido(
			String nomeArquivo,
			long bloco,
			long tamanhoBloco)
			throws Exception {

		return obterBloco(
				montarChaveValida(nomeArquivo),
				bloco,
				tamanhoBloco);

	}

	/*
	 * Recupera bloco inválido Redis.
	 */
	public List<String> obterBlocoInvalido(
			String nomeArquivo,
			long bloco,
			long tamanhoBloco)
			throws Exception {

		return obterBloco(
				montarChaveInvalida(nomeArquivo),
				bloco,
				tamanhoBloco);

	}

	/*
	 * Recupera bloco Redis.
	 */
	private List<String> obterBloco(
			String chave,
			long bloco,
			long tamanhoBloco)
			throws Exception {

		long inicio =
				bloco * tamanhoBloco;

		long fim =
				inicio + tamanhoBloco - 1;

		try (Jedis jedis =
					 jedisPool.getResource()) {

			List<String> linhas =
					jedis.lrange(
							chave,
							inicio,
							fim);

			log.infof(
					"Bloco Redis recuperado. chave=%s bloco=%d quantidade=%d",
					chave,
					bloco,
					linhas.size());

			return new ArrayList<>(linhas);

		} catch (Exception e) {

			log.errorf(
					e,
					"Erro ao recuperar bloco Redis. chave=%s",
					chave);

			throw e;

		}

	}

	/*
	 * Remove registros válidos.
	 */
	public void removerArquivoValido(
			String nomeArquivo)
			throws Exception {

		removerArquivo(
				montarChaveValida(nomeArquivo));

		removerArquivo(
				montarChaveControle(nomeArquivo));

	}

	/*
	 * Remove registros inválidos.
	 */
	public void removerArquivoInvalido(
			String nomeArquivo)
			throws Exception {

		removerArquivo(
				montarChaveInvalida(nomeArquivo));

	}

	/*
	 * Remove chave Redis.
	 */
	private void removerArquivo(
			String chave)
			throws Exception {

		try (Jedis jedis =
					 jedisPool.getResource()) {

			long removidos =
					jedis.del(chave);

			log.infof(
					"Arquivo Redis removido. chave=%s removidos=%d",
					chave,
					removidos);

		} catch (Exception e) {

			log.errorf(
					e,
					"Erro ao remover arquivo Redis. chave=%s",
					chave);

			throw e;

		}

	}

	@PreDestroy
	public void destroy() {

		log.info(
				"Finalizando pool Redis.");

		JdbcUtils.close(jedisPool);

		log.info(
				"Pool Redis finalizado.");

	}

	private String montarChaveValida(
			String nomeArquivo) {

		return PREFIXO_VALIDO + nomeArquivo;

	}

	private String montarChaveInvalida(
			String nomeArquivo) {

		return PREFIXO_INVALIDO + nomeArquivo;

	}

	private String montarChaveControle(
			String nomeArquivo) {

		return PREFIXO_CONTROLE + nomeArquivo;

	}

}