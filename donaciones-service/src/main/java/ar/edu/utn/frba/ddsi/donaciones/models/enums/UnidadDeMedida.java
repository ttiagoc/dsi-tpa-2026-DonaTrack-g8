package ar.edu.utn.frba.ddsi.donaciones.models.enums;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;

/**
 * En que se mide la cantidad de una necesidad ("50 kilogramos de fideos").
 * Los bienes donados siempre se cuentan en unidades y traen su peso y volumen por unidad,
 * asi que cada unidad de medida sabe cuanto aporta un bien: 20 paquetes de 0,5 kg = 10 kilogramos.
 * Si al bien le falta el dato (peso o volumen), no suma: no se puede convertir.
 */
public enum UnidadDeMedida {
    UNIDADES, KILOGRAMOS, LITROS;

    private static final double LITROS_POR_M3 = 1000.0;

    public double cantidadDe(Bien bien) {
        if (bien == null || bien.getCantidad() == null)
            return 0.0;

        return switch (this) {
            case UNIDADES -> bien.getCantidad();
            case KILOGRAMOS -> bien.getPesoKgPorUnidad() != null ? bien.calcularPesoTotal() : 0.0;
            // aproximado: es el volumen del envase
            case LITROS -> bien.getVolumenM3PorUnidad() != null ? bien.calcularVolumenTotal() * LITROS_POR_M3 : 0.0;
        };
    }
}
