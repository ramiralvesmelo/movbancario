package br.gov.pb.receita.sefaz.arr.movbancario.model.jms;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.EJB;
import javax.ejb.MessageDriven;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.ObjectMessage;

import org.jboss.logging.Logger;

import br.gov.pb.receita.sefaz.arr.movbancario.model.entity.RegistroCnab;
import br.gov.pb.receita.sefaz.arr.movbancario.model.service.ProcessadorArquivoService;

@MessageDriven(activationConfig = {

	@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue"),

	@ActivationConfigProperty(propertyName = "destination", propertyValue = "java:/jms/queue/cargaArquivoMovimentoBancario"),

	@ActivationConfigProperty(propertyName = "acknowledgeMode", propertyValue = "Auto-acknowledge"),

	@ActivationConfigProperty(propertyName = "maxSession", propertyValue = "10")

})
public class MovimentoBancarioMDB implements MessageListener {

	private static final Logger LOGGER = Logger.getLogger(MovimentoBancarioMDB.class);

	@EJB
	private ProcessadorArquivoService processadorService;

	@Override
	public void onMessage(Message message) {

		try {

			ObjectMessage objectMessage = (ObjectMessage) message;

			RegistroCnab registro = (RegistroCnab) objectMessage.getObject();

			LOGGER.infof("Mensagem recebida: %s", registro.getDocumento());

			/*
			 * Processamento arquivo CNAB.
			 */
			processadorService.processar(registro);

			LOGGER.infof("Processamento concluído: %s", registro.getDocumento());

		} catch (Exception e) {

			LOGGER.error("Erro processamento JMS", e);

			/*
			 * Força rollback JMS.
			 */
			throw new RuntimeException(e);

		}

	}

}