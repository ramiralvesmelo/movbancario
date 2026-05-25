package br.gov.pb.receita.sefaz.util.io.jdbc;

import java.sql.Connection;

import org.jboss.logging.Logger;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public final class JdbcUtils {

	private static final Logger LOGGER = Logger.getLogger(JdbcUtils.class);

	private JdbcUtils() {
	}

	/*
	 * Realiza rollback transacional.
	 */
	public static void rollback(Connection connection) {

		if (connection == null) {

			LOGGER.warn("Rollback ignorado. Connection nula.");

			return;

		}

		try {

			LOGGER.debug("Realizando rollback JDBC.");

			connection.rollback();

			LOGGER.debug("Rollback JDBC realizado com sucesso.");

		} catch (Exception e) {

			LOGGER.error("Erro ao realizar rollback JDBC.", e);

		}

	}

	/*
	 * Fecha conexão JDBC.
	 */
	public static void close(Connection connection) {

		if (connection == null) {

			LOGGER.warn("Close JDBC ignorado. Connection nula.");

			return;

		}

		try {

			LOGGER.debug("Fechando conexão JDBC.");

			connection.close();

			LOGGER.debug("Conexão JDBC fechada com sucesso.");

		} catch (Exception e) {

			LOGGER.error("Erro ao fechar conexão JDBC.", e);

		}

	}

	/*
	 * Restaura auto commit padrão.
	 */
	public static void resetAutoCommit(Connection connection) {

		if (connection == null) {

			LOGGER.warn("Reset autoCommit ignorado. Connection nula.");

			return;

		}

		try {

			LOGGER.debug("Restaurando autoCommit=true.");

			connection.setAutoCommit(true);

			LOGGER.debug("autoCommit restaurado com sucesso.");

		} catch (Exception e) {

			LOGGER.error("Erro ao restaurar autoCommit.", e);

		}

	}

	/*
	 * Fecha conexão Redis Jedis.
	 */
	public static void close(Jedis jedis) {

		if (jedis == null) {

			LOGGER.warn("Close Redis ignorado. Jedis nulo.");

			return;

		}

		try {

			LOGGER.debug("Fechando conexão Redis Jedis.");

			jedis.close();

			LOGGER.debug("Conexão Redis Jedis fechada com sucesso.");

		} catch (Exception e) {

			LOGGER.error("Erro ao fechar conexão Redis Jedis.", e);

		}

	}

	/*
	 * Fecha pool Redis.
	 */
	public static void close(JedisPool jedisPool) {

		if (jedisPool == null) {

			LOGGER.warn("Close Redis Pool ignorado. JedisPool nulo.");

			return;

		}

		try {

			LOGGER.debug("Fechando pool Redis.");

			jedisPool.close();

			LOGGER.debug("Pool Redis fechado com sucesso.");

		} catch (Exception e) {

			LOGGER.error("Erro ao fechar pool Redis.", e);

		}

	}

}