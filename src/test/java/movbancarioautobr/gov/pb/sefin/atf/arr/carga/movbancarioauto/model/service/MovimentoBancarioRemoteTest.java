package movbancarioautobr.gov.pb.sefin.atf.arr.carga.movbancarioauto.model.service;

import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;

import org.junit.Test;

import br.gov.pb.receita.sefaz.arr.movbancario.model.bean.MovimentoBancarioRemote;

public class MovimentoBancarioRemoteTest {

	@Test
	public void processarArquivos() throws Exception {

		Properties properties =
				new Properties();

		properties.put(
				Context.INITIAL_CONTEXT_FACTORY,
				"org.jboss.naming.remote.client.InitialContextFactory");

		properties.put(
				Context.PROVIDER_URL,
				"http-remoting://localhost:8080");

		/*
		 * Caso ambiente protegido:
		 */
		// properties.put(
		//         Context.SECURITY_PRINCIPAL,
		//         "admin");

		// properties.put(
		//         Context.SECURITY_CREDENTIALS,
		//         "root");

		properties.put(
				"jboss.naming.client.ejb.context",
				true);

		Context context =
				new InitialContext(properties);

		String jndi =
				"ejb:/movbancarioauto//MovimentoBancarioBean!"
				+ "br.gov.pb.sefin.atf.arr.carga.movbancarioauto.model.bean.MovimentoBancarioRemote";

		System.out.println(
				"JNDI: " + jndi);

		MovimentoBancarioRemote service =
				(MovimentoBancarioRemote)
						context.lookup(jndi);

		assertNotNull(service);

		service.processarArquivos();

		System.out.println(
				"EJB remoto executado com sucesso.");

	}

}