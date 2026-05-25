package br.gov.pb.receita.sefaz.util.io.validador;

@FunctionalInterface
public interface ValidadorLinha {

    void validar(
            String linha,
            long numeroLinha)
            throws Exception;

}