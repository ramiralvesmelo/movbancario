package br.gov.pb.receita.sefaz.arr.movbancario.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import org.jboss.logging.Logger;

import br.gov.pb.receita.sefaz.arr.movbancario.dto.RegistroCnab;

@Stateless
public class MovimentoBancarioH2Repository {

	private static final Logger LOGGER = Logger.getLogger(MovimentoBancarioH2Repository.class);

	private static final String SQL_INSERT = "INSERT INTO TB_MOV_BANCARIO (BANCO,DOCUMENTO,NOME,VALOR) VALUES (?, ?, ?, ?)";

	@Resource(mappedName = "java:/jdbc/MovBancarioDS")
	DataSource dataSource;

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void salvarLote(List<RegistroCnab> lote) throws Exception {

		try (Connection connection = dataSource.getConnection();

			PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {

			for (RegistroCnab registro : lote) {

				statement.setString(1, registro.getBanco());

				statement.setString(2, registro.getDocumento());

				statement.setString(3, registro.getNome());

				statement.setBigDecimal(4, registro.getValor());

				statement.addBatch();

			}

			statement.executeBatch();

			LOGGER.info("Lote persistido: " + lote.size());

		} catch (Exception e) {

			LOGGER.error("Erro ao persistir lote", e);

			throw e;

		}

	}

}