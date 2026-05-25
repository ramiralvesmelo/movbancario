package br.gov.pb.receita.sefaz.util.io.jdbc;

import java.sql.Connection;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public final class JdbcUtils {

    private JdbcUtils() {
    }

    /*
     * Realiza rollback transacional.
     */
    public static void rollback(
            Connection connection) {

        if (connection == null) {
            return;
        }

        try {

            connection.rollback();

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    /*
     * Fecha conexão JDBC.
     */
    public static void close(
            Connection connection) {

        if (connection == null) {
            return;
        }

        try {

            connection.close();

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    /*
     * Restaura auto commit padrão.
     */
    public static void resetAutoCommit(
            Connection connection) {

        if (connection == null) {
            return;
        }

        try {

            connection.setAutoCommit(
                    true);

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    /*
     * Fecha conexão Redis Jedis.
     */
    public static void close(
            Jedis jedis) {

        if (jedis == null) {
            return;
        }

        try {

            jedis.close();

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

    /*
     * Fecha pool Redis.
     */
    public static void close(
            JedisPool jedisPool) {

        if (jedisPool == null) {
            return;
        }

        try {

            jedisPool.close();

        } catch (Exception e) {

            e.printStackTrace();

        }

    }

}