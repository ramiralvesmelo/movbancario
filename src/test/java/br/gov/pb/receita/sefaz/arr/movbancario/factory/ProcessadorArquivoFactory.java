package br.gov.pb.receita.sefaz.arr.movbancario.service.factory;

import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.Statement;

import javax.sql.DataSource;

import org.h2.jdbcx.JdbcDataSource;

import br.gov.pb.receita.sefaz.arr.movbancario.repository.MovimentoBancarioH2Repository;
import br.gov.pb.receita.sefaz.arr.movbancario.repository.MovimentoBancarioRedisRepository;
import br.gov.pb.receita.sefaz.arr.movbancario.service.ProcessadorArquivoService;

public final class ProcessadorArquivoFactory {

	private ProcessadorArquivoFactory() {
	}

	public static ProcessadorArquivoService criarService() throws Exception {

		/*
		 * Inicializa datasource H2.
		 */
		JdbcDataSource ds = new JdbcDataSource();

		ds.setURL("jdbc:h2:mem:test;" + "DB_CLOSE_DELAY=-1");

		ds.setUser("sa");

		ds.setPassword("");

		DataSource dataSource = ds;

		/*
		 * Cria estrutura banco.
		 */
		try (Connection connection = dataSource.getConnection();

				Statement statement = connection.createStatement()) {

			statement.execute("CREATE TABLE TB_MOV_BANCARIO (" + "BANCO VARCHAR(10), " + "DOCUMENTO VARCHAR(50), "
					+ "NOME VARCHAR(100), " + "VALOR DECIMAL(15,2)" + ")");

		}

		/*
		 * Inicializa repositories.
		 */
		MovimentoBancarioH2Repository h2Repository = new MovimentoBancarioH2Repository();

		MovimentoBancarioRedisRepository redisRepository = new MovimentoBancarioRedisRepository();

		/*
		 * Injeta datasource H2.
		 */
		Field field = MovimentoBancarioH2Repository.class.getDeclaredField("dataSource");

		field.setAccessible(true);

		field.set(h2Repository, dataSource);

		/*
		 * Inicializa service.
		 */
		ProcessadorArquivoService service = new ProcessadorArquivoService();

		/*
		 * Injeta repositories.
		 */
		service.repository = h2Repository;

		service.redisRepository = redisRepository;

		return service;

	}

}