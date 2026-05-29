package br.gov.pb.receita.sefaz.arr.movbancario.model.transformador;

import java.math.BigDecimal;

import br.gov.pb.receita.sefaz.arr.movbancario.model.entity.RegistroCnab;
import br.gov.pb.receita.sefaz.util.io.transformador.TransformadorLinha;
import lombok.extern.jbosslog.JBossLog;

@JBossLog
public class TransformadorCnab240 implements TransformadorLinha<RegistroCnab> {

	@Override
	public RegistroCnab transformar(String linha, long numeroLinha) throws Exception {

		try {

			log.debugf("Transformando linha CNAB. linha=%d", numeroLinha);

			RegistroCnab dto = new RegistroCnab();

			dto.setBanco(linha.substring(0, 3).trim());

			dto.setDocumento(linha.substring(20, 40).trim());

			dto.setNome(linha.substring(40, 70).trim());

			String valor = linha.substring(70, 85).replaceAll("[^0-9]", "").trim();

			if (valor.isEmpty()) {

				valor = "0";

			}

			dto.setValor(new BigDecimal(valor).movePointLeft(2));

			log.debugf("Linha CNAB transformada com sucesso. linha=%d documento=%s", numeroLinha, dto.getDocumento());

			return dto;

		} catch (Exception e) {

			log.errorf(e, "Erro ao transformar linha CNAB. linha=%d", numeroLinha);

			throw e;

		}

	}

}