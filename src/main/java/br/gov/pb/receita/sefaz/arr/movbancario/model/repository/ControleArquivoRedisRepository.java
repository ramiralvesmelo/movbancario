package br.gov.pb.receita.sefaz.arr.movbancario.model.repository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import br.gov.pb.receita.sefaz.util.io.jdbc.JdbcUtils;
import br.gov.pb.receita.sefaz.util.io.jdbc.JedisPoolFactory;
import lombok.extern.jbosslog.JBossLog;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

/**
 * Repositório Redis responsável por:
 *
 * - controle de processamento - persistência registros válidos - persistência
 * registros inválidos - auditoria processamento
 */
@Singleton
@Startup
@JBossLog
public class ControleArquivoRedisRepository {

	private static final String PREFIXO_VALIDOS = "movbancario:validos:";

	private static final String PREFIXO_INVALIDOS = "movbancario:invalidos:";

	private static final String PREFIXO_CONTROLE = "movbancario:arquivo:";

	private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

	private JedisPool jedisPool;

	@PostConstruct
	public void init() {

		try {

			jedisPool = JedisPoolFactory.criarPool();

			log.info("Redis inicializado com sucesso.");

		} catch (Exception e) {

			log.error("Erro ao inicializar Redis", e);

			throw new IllegalStateException("Falha ao inicializar Redis", e);

		}

	}
	
	/**
	 * =========================================================================
	 * RECUPERA REGISTROS VÁLIDOS
	 * =========================================================================
	 */
	public List<String> recuperarRegistrosValidos(
			String arquivo) {

		try (Jedis jedis =
				jedisPool.getResource()) {

			return new ArrayList<>(
					jedis.lrange(
							PREFIXO_VALIDOS + arquivo,
							0,
							-1));

		}

	}	

	/**
	 * =========================================================================
	 * CONTROLE PROCESSAMENTO
	 * =========================================================================
	 */
	public boolean iniciarProcessamento(String arquivo) {

	    try (Jedis jedis = jedisPool.getResource()) {

	        String chave = PREFIXO_CONTROLE + arquivo;

	        String statusAtual =
	                jedis.hget(chave, "status");

	        /*
	         * Já concluído.
	         */
	        if ("CONCLUIDO".equals(statusAtual)) {

	            return false;

	        }

	        /*
	         * Já em execução.
	         */
	        if ("PROCESSANDO".equals(statusAtual)) {

	            return true;

	        }

	        jedis.hset(chave, "arquivo", arquivo);
	        jedis.hset(chave, "status", "PROCESSANDO");
	        jedis.hset(chave, "inicio", agora());
	        jedis.hset(chave, "fim", "");
	        jedis.hset(chave, "erro", "");
	        jedis.hset(chave, "total", "0");
	        jedis.hset(chave, "validos", "0");
	        jedis.hset(chave, "invalidos", "0");

	        return true;

	    }

	}

	/**
	 * =========================================================================
	 * ATUALIZA CONTADORES
	 * =========================================================================
	 */
	public void atualizarContadores(String arquivo, long total, long validos, long invalidos) {

		try (Jedis jedis = jedisPool.getResource()) {

			String chave = PREFIXO_CONTROLE + arquivo;

			jedis.hset(chave, "total", String.valueOf(total));

			jedis.hset(chave, "validos", String.valueOf(validos));

			jedis.hset(chave, "invalidos", String.valueOf(invalidos));

		}

	}

	/**
	 * =========================================================================
	 * FINALIZA PROCESSAMENTO
	 * =========================================================================
	 */
	public void finalizarProcessamento(String arquivo, long total, long validos, long invalidos) {

		try (Jedis jedis = jedisPool.getResource()) {

			String chave = PREFIXO_CONTROLE + arquivo;

			jedis.hset(chave, "status", "CONCLUIDO");
			jedis.hset(chave, "fim", agora());
			jedis.hset(chave, "total", String.valueOf(total));
			jedis.hset(chave, "validos", String.valueOf(validos));
			jedis.hset(chave, "invalidos", String.valueOf(invalidos));

		}

		log.infof("Processamento concluído arquivo=%s total=%d validos=%d invalidos=%d", arquivo, total, validos,
				invalidos);

	}

	/**
	 * =========================================================================
	 * REGISTRA ERRO
	 * =========================================================================
	 */
	public void registrarErro(String arquivo, Exception e) {

		try (Jedis jedis = jedisPool.getResource()) {

			String mensagem = e == null ? "Erro desconhecido" : String.valueOf(e.getMessage());

			if (mensagem == null || "null".equals(mensagem)) {

				mensagem = e.getClass().getName();

			}

			jedis.hset("movbancario:controle:" + arquivo, "erro", mensagem);

		}

	}

	/**
	 * =========================================================================
	 * REGISTROS VÁLIDOS
	 * =========================================================================
	 */
	public void salvarBlocoValido(String arquivo, List<String> linhas) {

		if (linhas == null || linhas.isEmpty()) {

			return;

		}

		try (Jedis jedis = jedisPool.getResource()) {

			jedis.rpush(PREFIXO_VALIDOS + arquivo, linhas.toArray(new String[0]));

		}

	}

	/**
	 * =========================================================================
	 * REGISTROS INVÁLIDOS
	 * =========================================================================
	 */
	public void salvarBlocoInvalido(String arquivo, List<String> linhas) {

		if (linhas == null || linhas.isEmpty()) {

			return;

		}

		try (Jedis jedis = jedisPool.getResource()) {

			jedis.rpush(PREFIXO_INVALIDOS + arquivo, linhas.toArray(new String[0]));

		}

	}

	/**
	 * =========================================================================
	 * CONSULTA PROCESSAMENTO
	 * =========================================================================
	 */
	public boolean arquivoJaProcessado(String arquivo) {

		try (Jedis jedis = jedisPool.getResource()) {

			String status = jedis.hget(PREFIXO_CONTROLE + arquivo, "status");

			return "CONCLUIDO".equals(status);

		}

	}

	/**
	 * =========================================================================
	 * LIMPEZA
	 * =========================================================================
	 */
	public void removerArquivo(String arquivo) {

		try (Jedis jedis = jedisPool.getResource()) {

			jedis.del(PREFIXO_CONTROLE + arquivo);
			jedis.del(PREFIXO_VALIDOS + arquivo);
			jedis.del(PREFIXO_INVALIDOS + arquivo);

		}

	}

	@PreDestroy
	public void destroy() {

		JdbcUtils.close(jedisPool);

		log.info("Pool Redis finalizado.");

	}

	private String agora() {

		return LocalDateTime.now().format(DATE_FORMAT);

	}

}