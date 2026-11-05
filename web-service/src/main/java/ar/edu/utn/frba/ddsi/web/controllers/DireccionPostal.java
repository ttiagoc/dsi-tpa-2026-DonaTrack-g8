package ar.edu.utn.frba.ddsi.web.controllers;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * La direccion se carga en campos separados (calle, altura, localidad y provincia) y se le manda a la API
 * en un solo texto, "Calle altura, localidad, provincia", que es como la geocodifica con Georef.
 * Las comas separan las partes: por eso se sacan de lo que escribio la persona.
 */
public final class DireccionPostal {

    /** Como las nombra Georef (Tierra del Fuego, abreviada: el nombre completo tiene comas). */
    public static final List<String> PROVINCIAS = List.of(
            "Ciudad Autónoma de Buenos Aires", "Buenos Aires", "Catamarca", "Chaco", "Chubut", "Córdoba",
            "Corrientes", "Entre Ríos", "Formosa", "Jujuy", "La Pampa", "La Rioja", "Mendoza", "Misiones",
            "Neuquén", "Río Negro", "Salta", "San Juan", "San Luis", "Santa Cruz", "Santa Fe",
            "Santiago del Estero", "Tierra del Fuego", "Tucumán");

    private DireccionPostal() {
    }

    public static String componer(String calle, String altura, String localidad, String provincia) {
        return limpiar(calle) + " " + limpiar(altura) + ", " + limpiar(localidad) + ", " + limpiar(provincia);
    }

    /** Las opciones del select, con la elegida marcada. */
    public static List<Map<String, Object>> opciones(String elegida) {
        return PROVINCIAS.stream().map(nombre -> {
            Map<String, Object> opcion = new LinkedHashMap<>();
            opcion.put("nombre", nombre);
            opcion.put("elegida", nombre.equals(elegida));
            return opcion;
        }).toList();
    }

    private static String limpiar(String parte) {
        return parte == null ? "" : parte.replace(',', ' ').trim().replaceAll("\s+", " ");
    }
}
