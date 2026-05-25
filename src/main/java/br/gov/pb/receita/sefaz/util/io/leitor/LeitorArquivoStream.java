package br.gov.pb.receita.sefaz.util.io.leitor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class LeitorArquivoStream {

    /**
     * Lê o arquivo linha por linha utilizando stream,
     * delegando o processamento para a implementação recebida.
     * @throws Exception 
     */
    public static void processar(
            File arquivo,
            ProcessadorLinha processador)
            throws Exception {

        try (BufferedReader reader =
                     new BufferedReader(new FileReader(arquivo))) {

            String linha;
            long numeroLinha = 0;

            while ((linha = reader.readLine()) != null) {

                numeroLinha++;

                processador.executar(
                        linha,
                        numeroLinha);

            }

        }

    }

    /**
     * Interface funcional responsável pelo processamento
     * de cada linha do arquivo.
     */
    @FunctionalInterface
    public interface ProcessadorLinha {

        void executar(
                String linha,
                long numeroLinha)
                throws Exception;

    }

}