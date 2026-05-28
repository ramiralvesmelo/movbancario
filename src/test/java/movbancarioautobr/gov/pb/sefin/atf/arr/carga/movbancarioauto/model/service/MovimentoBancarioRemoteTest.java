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

		Properties properties = new Properties();

		properties.put(Context.INITIAL_CONTEXT_FACTORY, "org.jboss.naming.remote.client.InitialContextFactory");
		properties.put(Context.PROVIDER_URL, "remote://localhost:4447");
		properties.put("jboss.naming.client.ejb.context", true);
		Context context = new InitialContext(properties);
		String jndi = "movbancario/MovimentoBancarioBean!" + MovimentoBancarioRemote.class.getName();
		System.out.println("Lookup JNDI: " + jndi);

		MovimentoBancarioRemote service = (MovimentoBancarioRemote) context.lookup(jndi);

		assertNotNull(service);

		service.processarArquivos();

		System.out.println("EJB remoto executado com sucesso.");

	}

}