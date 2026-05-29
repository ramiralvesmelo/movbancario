package br.gov.pb.receita.sefaz.arr.movbancario.model.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import br.gov.pb.receita.sefaz.arr.movbancario.model.entity.RegistroCnab;
import lombok.extern.jbosslog.JBossLog;

@Stateless
@JBossLog
public class MovimentoBancarioH2Repository {

	private static final Object LOCK = new Object();

	private static final String SQL_INSERT = "INSERT INTO TB_MOV_BANCARIO " + "(BANCO, DOCUMENTO, NOME, VALOR) "
			+ "VALUES (?, ?, ?, ?)";

	@Resource(mappedName = "java:/jdbc/MovBancarioDS")
	private DataSource dataSource;

	@TransactionAttribute(TransactionAttributeType.REQUIRED)
	public void salvarLote(List<RegistroCnab> lote) throws Exception {

		synchronized (LOCK) {

			if (lote == null || lote.isEmpty()) {

				log.warn("Lote vazio recebido para persistência");

				return;

			}

			log.infof("Thread=%s Persistindo lote=%d", Thread.currentThread().getName(), lote.size());

			try (Connection connection = dataSource.getConnection();

					PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {

				for (RegistroCnab registro : lote) {

					adicionarBatch(statement, registro);

				}

				int[] resultado = statement.executeBatch();

				log.infof("Lote persistido com sucesso. Registros=%d", resultado.length);

			} catch (Exception e) {

				log.errorf(e, "Erro ao persistir lote. Quantidade registros=%d", lote.size());

				throw e;

			}


		}

	}

	private void adicionarBatch(PreparedStatement statement, RegistroCnab registro) throws Exception {

		statement.setString(1, registro.getBanco());

		statement.setString(2, registro.getDocumento());

		statement.setString(3, registro.getNome());

		statement.setBigDecimal(4, registro.getValor());

		statement.addBatch();

	}

}