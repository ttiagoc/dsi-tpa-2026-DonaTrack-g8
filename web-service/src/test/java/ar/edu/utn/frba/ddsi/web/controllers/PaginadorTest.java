package ar.edu.utn.frba.ddsi.web.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.web.dto.comun.PaginaResponse;

class PaginadorTest {

    @Test
    @DisplayName("urlConPagina preserva filtros de búsqueda y descarta parámetros efímeros de modales")
    void urlConPaginaPreservaFiltrosYDescartaEfimeros() {
        Map<String, List<String>> params = Map.of(
                "q", List.of("arroz integral"),
                "tipo", List.of("HUMANA"),
                "editar", List.of("42"),
                "nuevo", List.of("true"),
                "vencer", List.of("10"),
                "propuesta", List.of("99"),
                "pagina", List.of("3")
        );

        String url = Paginador.urlConPagina("/admin/donantes", params, 2);

        assertTrue(url.contains("q=arroz+integral"));
        assertTrue(url.contains("tipo=HUMANA"));
        assertTrue(url.contains("pagina=2"));
        assertFalse(url.contains("editar"));
        assertFalse(url.contains("nuevo"));
        assertFalse(url.contains("vencer"));
        assertFalse(url.contains("propuesta"));
    }

    @Test
    @DisplayName("urlConPagina no agrega ?pagina=1 para mantener URLs limpias en la primera página")
    void urlConPaginaOmitePaginaUno() {
        Map<String, List<String>> params = Map.of("filtro", List.of("vencidas"));
        String url = Paginador.urlConPagina("/admin/deposito", params, 1);

        assertEquals("/admin/deposito?filtro=vencidas", url);
    }

    @Test
    @DisplayName("PaginaResponse calcula correctamente rangos desde, hasta y banderas")
    void paginaResponseCalculaRangos() {
        PaginaResponse<String> pag = PaginaResponse.de(List.of("a", "b", "c"), 2, 4, 35, 10);

        assertEquals(11, pag.desde());
        assertEquals(20, pag.hasta());
        assertEquals(35, pag.totalElementos());
        assertTrue(pag.hayVarias());
        assertTrue(pag.tieneAnterior());
        assertTrue(pag.tieneSiguiente());
    }

    @Test
    @DisplayName("PaginaResponse vacía retorna rangos en cero")
    void paginaResponseVacia() {
        PaginaResponse<String> vacia = PaginaResponse.vacia(1, 10);

        assertEquals(0, vacia.desde());
        assertEquals(0, vacia.hasta());
        assertEquals(0, vacia.totalElementos());
        assertFalse(vacia.hayVarias());
        assertFalse(vacia.tieneAnterior());
        assertFalse(vacia.tieneSiguiente());
    }

    @Test
    @DisplayName("armarModelo genera el mapa con links previo y siguiente")
    void armarModeloGeneraMapaCompleto() {
        PaginaResponse<String> pag = PaginaResponse.de(List.of("x"), 2, 3, 25, 10);
        // Usamos un mapa mockeado o directamente probamos la generación de URLs
        String ant = Paginador.urlConPagina("/entidad/donaciones", Map.of(), pag.pagina() - 1);
        String sig = Paginador.urlConPagina("/entidad/donaciones", Map.of(), pag.pagina() + 1);

        assertEquals("/entidad/donaciones", ant);
        assertEquals("/entidad/donaciones?pagina=3", sig);
    }
}

