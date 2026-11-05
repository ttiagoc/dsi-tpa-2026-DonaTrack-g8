package ar.edu.utn.frba.ddsi.web.dto.catalogo;

/** Cuantos registros usan una subcategoria (o las de una categoria): si hay alguno, no se puede eliminar. */
public record UsosCatalogoResponse(long donaciones, long necesidades) {

    public boolean enUso() {
        return donaciones > 0 || necesidades > 0;
    }
}
