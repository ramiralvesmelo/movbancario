package br.gov.pb.receita.sefaz.arr.movbancario.model.repository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import javax.annotation.Resource;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.sql.DataSource;

import br.gov.pb.receita.sefaz.arr.movbancario.model.entity.RegistroCnab;
import lombok.extern.jbosslog.JBossLog;

@Singleton
@Lock(LockType.WRITE)
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
@JBossLog
public class MovimentoBancarioH2Repository {

	private static final String SQL_INSERT = "INSERT INTO TB_MOV_BANCARIO " + "(BANCO, DOCUMENTO, NOME, VALOR) "
			+ "VALUES (?, ?, ?, ?)";

	@Resource(mappedName = "java:/jdbc/MovBancarioDS")
	private DataSource dataSource;

	public void salvarLote(List<RegistroCnab> lote) throws Exception {

		if (lote == null || lote.isEmpty()) {

			log.warn("Lote vazio recebido para persistência");

			return;

		}

		long inicio = System.currentTimeMillis();

		log.infof("Thread=%s Persistindo lote=%d", Thread.currentThread().getName(), lote.size());

		try (Connection connection = dataSource.getConnection();
				PreparedStatement statement = connection.prepareStatement(SQL_INSERT)) {

			connection.setAutoCommit(false);

			for (RegistroCnab registro : lote) {

				adicionarBatch(statement, registro);

			}

			int[] resultado = statement.executeBatch();

			statement.clearBatch();

			connection.commit();

			log.infof("Lote persistido com sucesso. Registros=%d Tempo=%dms", resultado.length,
					(System.currentTimeMillis() - inicio));

		} catch (Exception e) {

			log.errorf(e, "Erro ao persistir lote. Quantidade registros=%d", lote.size());

			throw e;

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