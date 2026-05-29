package br.gov.pb.receita.sefaz.util.io.jdbc;

import java.util.List;
import java.util.Map;

import br.gov.pb.receita.sefaz.util.io.leitor.YamlUtils;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public final class JedisPoolFactory {

	/*
	 * Arquivo configuração Redis.
	 */
	private static final String REDIS_CONFIG = "/var/atf/configuracoes/redis-conf.yaml";

	private JedisPoolFactory() {
	}

	/**
	 * Cria pool Redis.
	 */
	public static JedisPool criarPool() throws Exception {

		@SuppressWarnings("unchecked")
		Map<String, Object> server = (Map<String, Object>) YamlUtils.load(REDIS_CONFIG).get("singleServerConfig");

		String address = obterAddress(server);

		String host = address.split(":")[0];

		int port = Integer.parseInt(address.split(":")[1]);

		int database = obterInteiro(server, "database", 0);

		JedisPoolConfig poolConfig = criarPoolConfig(server);

		JedisPool pool = new JedisPool(poolConfig, host, port);

		/*
		 * Valida conexão.
		 */
		try (Jedis jedis = pool.getResource()) {

			jedis.select(database);

			jedis.ping();

		}

		return pool;

	}

	/**
	 * Obtém endereço Redis.
	 */
	private static String obterAddress(Map<String, Object> server) {

		Object addressObj = server.get("address");

		String address;

		if (addressObj instanceof List) {

			@SuppressWarnings("unchecked")
			List<String> addresses = (List<String>) addressObj;

			address = addresses.get(0);

		} else {

			address = String.valueOf(addressObj);

		}

		address = address.replace("[", "").replace("]", "").trim();

		address = address.replace("redis://", "").replace("//", "");

		return address;

	}

	/**
	 * Cria configuração do pool.
	 */
	private static JedisPoolConfig criarPoolConfig(Map<String, Object> server) {

		int maxPool = obterInteiro(server, "connectionPoolSize", 50);

		JedisPoolConfig config = new JedisPoolConfig();

		config.setMaxTotal(maxPool);
		config.setMaxIdle(maxPool);
		config.setMinIdle(5);

		config.setTestOnBorrow(true);
		config.setTestWhileIdle(true);

		config.setBlockWhenExhausted(true);

		return config;

	}

	/**
	 * Obtém inteiro do YAML.
	 */
	private static int obterInteiro(Map<String, Object> map, String chave, int valorPadrao) {

		Object valor = map.get(chave);

		if (valor == null) {

			return valorPadrao;

		}

		return Integer.parseInt(String.valueOf(valor));

	}

}