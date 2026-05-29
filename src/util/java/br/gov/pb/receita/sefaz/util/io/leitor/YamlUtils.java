package br.gov.pb.receita.sefaz.util.io.leitor;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.yaml.snakeyaml.Yaml;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
public final class YamlUtils {

	private static final Map<String, Map<String, Object>> CACHE = new ConcurrentHashMap<>();

	private YamlUtils() {
	}

	@SuppressWarnings("unchecked")
	public static Map<String, Object> load(String path) throws Exception {

		Map<String, Object> cache = CACHE.get(path);

		if (cache != null) {

			return cache;

		}

		synchronized (YamlUtils.class) {

			cache = CACHE.get(path);

			if (cache != null) {

				return cache;

			}

			File file = new File(path);

			if (!file.exists()) {

				throw new IllegalStateException("Arquivo YAML não encontrado: " + path);

			}

			try (InputStream input = new FileInputStream(file)) {

				log.infof("Carregando YAML: %s", path);

				Yaml yaml = new Yaml();

				cache = (Map<String, Object>) yaml.load(input);

				CACHE.put(path, cache);

				return cache;

			}

		}

	}

}