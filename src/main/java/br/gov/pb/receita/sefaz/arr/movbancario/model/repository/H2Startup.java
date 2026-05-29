package br.gov.pb.receita.sefaz.arr.movbancario.model.repository;

import java.io.File;
import java.sql.Connection;
import java.sql.Statement;

import javax.annotation.Resource;
import javax.ejb.Singleton;
import javax.sql.DataSource;

import br.gov.pb.receita.sefaz.arr.movbancario.model.bean.MovimentoBancarioBean;
import br.gov.pb.receita.sefaz.util.io.leitor.LeitorArquivoStream;
import lombok.extern.jbosslog.JBossLog;

@Singleton
@JBossLog
public class H2Startup {

	private static final String SQL_DROP_TABLE = "DROP TABLE IF EXISTS TB_MOV_BANCARIO";

	private static final String SQL_CREATE_TABLE = "CREATE TABLE TB_MOV_BANCARIO (" + "BANCO VARCHAR(10), "
			+ "DOCUMENTO VARCHAR(50), " + "NOME VARCHAR(100), " + "VALOR DECIMAL(15,2)" + ")";

	@Resource(mappedName = "java:/jdbc/MovBancarioDS")
	private DataSource dataSource;

	public void create() {

		log.info("Inicializando estrutura H2.");

		try (Connection connection = dataSource.getConnection(); Statement statement = connection.createStatement()) {

			log.infof("URL H2: %s", connection.getMetaData().getURL());

			log.infof("Catalog: %s", connection.getCatalog());

			log.infof("User: %s", connection.getMetaData().getUserName());

			/*
			 * Remove tabela anterior.
			 */
			statement.execute(SQL_DROP_TABLE);

			log.info("Tabela TB_MOV_BANCARIO removida.");

			/*
			 * Cria tabela novamente.
			 */
			statement.execute(SQL_CREATE_TABLE);

			log.info("Tabela TB_MOV_BANCARIO criada com sucesso.");

		} catch (Exception e) {

			log.error("Erro inicializando H2", e);

			throw new RuntimeException(e);

		}

		LeitorArquivoStream.removerLocksTxt(new File(MovimentoBancarioBean.DIRETORIO_CNAB));

	}

}