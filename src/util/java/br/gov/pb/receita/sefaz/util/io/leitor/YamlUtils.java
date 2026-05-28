package br.gov.pb.receita.sefaz.util.io.leitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;

import lombok.extern.jbosslog.JBossLog;
import org.yaml.snakeyaml.Yaml;

@JBossLog
public final class YamlUtils {

	private YamlUtils() {

	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> load(String path) throws Exception {

		File file = new File(path);

		if (!file.exists()) {

			throw new IllegalStateException("Arquivo YAML não encontrado: " + path);

		}

		try (InputStream input = new FileInputStream(file)) {

			log.infof("Carregando YAML: %s", path);

			Yaml yaml = new Yaml();

			return (Map<String, Object>) yaml.load(input);

		} catch (Exception e) {

			log.error("Erro ao carregar YAML", e);

			throw e;

		}

	}

}