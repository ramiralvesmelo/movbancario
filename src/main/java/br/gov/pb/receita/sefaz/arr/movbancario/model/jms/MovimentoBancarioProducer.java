package br.gov.pb.receita.sefaz.arr.movbancario.model.jms;

import javax.annotation.Resource;
import javax.ejb.Stateless;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.MessageProducer;
import javax.jms.ObjectMessage;
import javax.jms.Queue;
import javax.jms.Session;

import org.jboss.logging.Logger;

import br.gov.pb.receita.sefaz.arr.movbancario.model.entity.RegistroCnab;

@Stateless
public class MovimentoBancarioProducer {

	private static final Logger LOGGER = Logger.getLogger(MovimentoBancarioProducer.class);

	@Resource(mappedName = "java:/JmsXA")
	private ConnectionFactory connectionFactory;

	@Resource(mappedName = "java:/jms/queue/cargaArquivoMovimentoBancario")
	private Queue queue;

	public void enviar(RegistroCnab registro) throws Exception {

		Connection connection = null;

		Session session = null;

		MessageProducer producer = null;

		try {

			connection = connectionFactory.createConnection();

			connection.start();

			session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

			producer = session.createProducer(queue);

			ObjectMessage message = session.createObjectMessage();

			message.setObject(registro);

			producer.send(message);

			LOGGER.info("Mensagem JMS enviada: " + registro);

		} finally {

			try {

				if (producer != null) {
					producer.close();
				}

			} catch (Exception e) {
				LOGGER.error(e);
			}

			try {

				if (session != null) {
					session.close();
				}

			} catch (Exception e) {
				LOGGER.error(e);
			}

			try {

				if (connection != null) {
					connection.close();
				}

			} catch (Exception e) {
				LOGGER.error(e);
			}

		}

	}

}