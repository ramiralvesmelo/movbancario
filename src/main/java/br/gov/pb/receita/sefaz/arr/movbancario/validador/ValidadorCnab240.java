package br.gov.pb.receita.sefaz.arr.movbancario.validador;

import br.gov.pb.receita.sefaz.util.io.validador.ValidadorLinha;

public class ValidadorCnab240 implements ValidadorLinha {

	/*
	 * Tamanho padrão do registro CNAB240.
	 */
	private static final int TAMANHO_LINHA = 240;

	@Override
	public void validar(String linha, long numeroLinha) throws Exception {

		/*
		 * Remove caracteres de quebra de linha Windows/Linux antes das
		 * validações.
		 */
		linha = normalizarLinha(linha);

		/*
		 * Valida se a linha foi carregada.
		 */
		validarLinhaNula(linha, numeroLinha);

		/*
		 * Valida tamanho fixo CNAB240.
		 */
		validarTamanho(linha, numeroLinha);

		/*
		 * Valida código FEBRABAN do banco.
		 */
		validarCodigoBanco(linha, numeroLinha);

		/*
		 * Valida tipo lógico do registro.
		 */
		validarTipoRegistro(linha, numeroLinha);

	}

	/*
	 * Remove CR/LF da linha para evitar inconsistência no tamanho posicional.
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

			throw new Exception("Linha vazia. " + "Linha: " + numeroLinha);

		}

	}

	/*
	 * Valida tamanho fixo do registro CNAB240.
	 */
	private void validarTamanho(String linha, long numeroLinha) throws Exception {

		if (linha.length() != TAMANHO_LINHA) {

			throw new Exception("Linha com tamanho inválido. " + "Linha: " + numeroLinha + " tamanho=" + linha.length()
					+ " esperado=" + TAMANHO_LINHA);

		}

	}

	/*
	 * Valida código do banco. Exemplo: 001 = Banco do Brasil 104 = Caixa 237 =
	 * Bradesco
	 */
	private void validarCodigoBanco(String linha, long numeroLinha) throws Exception {

		String banco = linha.substring(0, 3);

		if (!banco.matches("\\d{3}")) {

			throw new Exception("Código do banco inválido. " + "Linha: " + numeroLinha + " banco=" + banco);

		}

	}

	/*
	 * Valida tipo do registro CNAB240.
	 *
	 * 0 = Header Arquivo 1 = Header Lote 3 = Detalhe 5 = Trailer Lote 9 =
	 * Trailer Arquivo
	 */
	private void validarTipoRegistro(String linha, long numeroLinha) throws Exception {

		String tipoRegistro = linha.substring(7, 8);

		if (!tipoRegistro.matches("[01359]")) {

			throw new Exception("Tipo de registro inválido. " + "Linha: " + numeroLinha + " tipo=" + tipoRegistro);

		}

	}

}