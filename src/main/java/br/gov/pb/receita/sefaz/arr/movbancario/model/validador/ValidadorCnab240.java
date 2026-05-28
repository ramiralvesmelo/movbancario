package br.gov.pb.receita.sefaz.arr.movbancario.model.validador;

import lombok.extern.jbosslog.JBossLog;
import br.gov.pb.receita.sefaz.util.io.validador.ValidadorLinha;

@JBossLog
public class ValidadorCnab240 implements ValidadorLinha {

	/*
	 * Tamanho padrão registro CNAB240.
	 */
	private static final int TAMANHO_LINHA = 240;

	@Override
	public void validar(String linha, long numeroLinha) throws Exception {

		try {

			log.debugf("Iniciando validação CNAB240. linha=%d", numeroLinha);

			/*
			 * Remove CR/LF.
			 */
			linha = normalizarLinha(linha);

			/*
			 * Valida linha carregada.
			 */
			validarLinhaNula(linha, numeroLinha);

			/*
			 * Valida tamanho fixo.
			 */
			validarTamanho(linha, numeroLinha);

			/*
			 * Valida código banco.
			 */
			validarCodigoBanco(linha, numeroLinha);

			/*
			 * Valida tipo registro.
			 */
			validarTipoRegistro(linha, numeroLinha);

			log.debugf("Linha CNAB240 validada com sucesso. linha=%d", numeroLinha);

		} catch (Exception e) {

			log.errorf(e, "Erro validação CNAB240. linha=%d", numeroLinha);

			throw e;

		}

	}

	/*
	 * Remove CR/LF.
	 */
	private String normalizarLinha(String linha) {

		if (linha == null) {

			return null;

		}

		return linha.replace("\r", "").replace("\n", "");

	}

	/*
	 * Valida linha vazia/nula.
	 */
	private void validarLinhaNula(String linha, long numeroLinha) throws Exception {

		if (linha == null || linha.trim().isEmpty()) {

			log.warnf("Linha vazia detectada. linha=%d", numeroLinha);

			throw new Exception("Linha vazia. linha=" + numeroLinha);

		}

	}

	/*
	 * Valida tamanho fixo CNAB240.
	 */
	private void validarTamanho(String linha, long numeroLinha) throws Exception {

		if (linha.length() != TAMANHO_LINHA) {

			log.warnf("Tamanho inválido. linha=%d tamanho=%d esperado=%d", numeroLinha, linha.length(), TAMANHO_LINHA);

			throw new Exception("Linha com tamanho inválido. " + "linha=" + numeroLinha + " tamanho=" + linha.length()
					+ " esperado=" + TAMANHO_LINHA);

		}

	}

	/*
	 * Valida código FEBRABAN banco.
	 */
	private void validarCodigoBanco(String linha, long numeroLinha) throws Exception {

		String banco = linha.substring(0, 3);

		if (!banco.matches("\\d{3}")) {

			log.warnf("Banco inválido. linha=%d banco=%s", numeroLinha, banco);

			throw new Exception("Código banco inválido. " + "linha=" + numeroLinha + " banco=" + banco);

		}

	}

	/*
	 * Valida tipo registro CNAB.
	 *
	 * 0 = Header Arquivo 1 = Header Lote 3 = Detalhe 5 = Trailer Lote 9 =
	 * Trailer Arquivo
	 */
	private void validarTipoRegistro(String linha, long numeroLinha) throws Exception {

		String tipoRegistro = linha.substring(7, 8);

		if (!tipoRegistro.matches("[01359]")) {

			log.warnf("Tipo registro inválido. linha=%d tipo=%s", numeroLinha, tipoRegistro);

			throw new Exception("Tipo registro inválido. " + "linha=" + numeroLinha + " tipo=" + tipoRegistro);

		}

	}

}