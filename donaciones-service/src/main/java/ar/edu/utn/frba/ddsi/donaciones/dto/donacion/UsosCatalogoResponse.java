package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

/** Cuantos registros usan una subcategoria (o las de una categoria). Si hay alguno, no se puede eliminar. */
public record UsosCatalogoResponse(long donaciones, long necesidades) {
}
