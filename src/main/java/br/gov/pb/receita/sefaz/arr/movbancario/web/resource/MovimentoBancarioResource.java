package br.gov.pb.receita.sefaz.arr.movbancario.web.resource;

import javax.ejb.EJB;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import br.gov.pb.receita.sefaz.arr.movbancario.model.bean.MovimentoBancarioRemote;
import lombok.extern.jbosslog.JBossLog;

@Path("/movimento-bancario")
@Produces(MediaType.APPLICATION_JSON)
@JBossLog
public class MovimentoBancarioResource {

	@EJB
	private MovimentoBancarioRemote movimentoBancarioRemote;

	/**
	 * Endpoint responsável pelo processamento manual CNAB.
	 *
	 * Exemplo:
	 *
	 * POST /api/movimento-bancario/processar
	 */
	@GET
	@Path("/processar")
	public Response processar() {

		try {

			log.info("Iniciando processamento manual CNAB");

			/*
			 * Executa processamento.
			 */
			movimentoBancarioRemote.processarArquivos();

			log.info("Processamento concluido com sucesso");

			return Response.ok().entity("Processamento iniciado com sucesso").build();

		} catch (IllegalArgumentException e) {

			log.warnf("Erro validacao processamento: %s", e.getMessage());

			return Response.status(Response.Status.BAD_REQUEST).entity(e.getMessage()).build();

		} catch (Exception e) {

			log.error("Erro processamento arquivo CNAB", e);

			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity("Erro interno processamento arquivo")
					.build();

		}

	}

}