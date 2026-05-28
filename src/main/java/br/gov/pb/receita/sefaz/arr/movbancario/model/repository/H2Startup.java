package br.gov.pb.receita.sefaz.arr.movbancario.model.repository;

import java.sql.Connection;
import java.sql.Statement;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.sql.DataSource;

import org.jboss.logging.Logger;

@Startup
@Singleton
public class H2Startup {

	private static final Logger LOGGER = Logger.getLogger(H2Startup.class);

	private static final String SQL_CREATE_TABLE = "CREATE TABLE IF NOT EXISTS TB_MOV_BANCARIO ("
			+ "BANCO VARCHAR(10), " + "DOCUMENTO VARCHAR(50), " + "NOME VARCHAR(100), " + "VALOR DECIMAL(15,2)" + ")";

	@Resource(mappedName = "java:/jdbc/MovBancarioDS")
	private DataSource dataSource;

	@PostConstruct
	public void init() {

		LOGGER.info("Inicializando estrutura H2.");

		try (Connection connection = dataSource.getConnection();

				Statement statement = connection.createStatement()) {

			/*
			 * Cria tabela movimentação bancária.
			 */
			statement.execute(SQL_CREATE_TABLE);

			LOGGER.info("Tabela TB_MOV_BANCARIO criada com sucesso.");

		} catch (Exception e) {

			LOGGER.error("Erro inicializando H2", e);

			throw new RuntimeException(e);

		}

	}

}