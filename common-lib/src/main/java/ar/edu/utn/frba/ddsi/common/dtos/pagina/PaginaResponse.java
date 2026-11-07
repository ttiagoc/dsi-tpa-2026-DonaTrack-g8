package ar.edu.utn.frba.ddsi.common.dtos.pagina;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * DTO estandar para respuestas paginadas de los microservicios.
 */
public record PaginaResponse<T>(
        @JsonProperty("elementos") List<T> elementos,
        @JsonProperty("pagina") int pagina,
        @JsonProperty("totalPaginas") int totalPaginas,
        @JsonProperty("totalElementos") long totalElementos,
        @JsonProperty("porPagina") int porPagina
) {
    public static <T> PaginaResponse<T> de(List<T> elementos, int pagina, int totalPaginas, long totalElementos, int porPagina) {
        return new PaginaResponse<>(elementos, pagina, totalPaginas, totalElementos, porPagina);
    }

    public static <T> PaginaResponse<T> vacia(int pagina, int porPagina) {
        return new PaginaResponse<>(List.of(), Math.max(1, pagina), 1, 0, Math.max(1, porPagina));
    }

    public boolean tieneAnterior() {
        return pagina > 1;
    }

    public boolean tieneSiguiente() {
        return pagina < totalPaginas;
    }

    public long desde() {
        return totalElementos == 0 ? 0 : (long) (pagina - 1) * porPagina + 1;
    }

    public long hasta() {
        return Math.min((long) pagina * porPagina, totalElementos);
    }

    public boolean hayVarias() {
        return totalPaginas > 1;
    }
}

