package br.gov.pb.receita.sefaz.util.io.transformador;

public interface TransformadorLinha<T> {

    T transformar(
            String linha,
            long numeroLinha)
            throws Exception;

}
