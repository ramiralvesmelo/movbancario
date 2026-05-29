package br.gov.pb.receita.sefaz.arr.movbancario;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;

import br.gov.pb.receita.sefaz.arr.movbancario.model.repository.H2Startup;

@Startup
@Singleton
public class AppStartUp {

	@EJB
	private H2Startup h2Startup;

	@PostConstruct
	public void init() {

		h2Startup.create();

	}

}