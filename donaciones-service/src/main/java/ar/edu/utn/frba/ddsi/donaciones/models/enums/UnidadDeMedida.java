package ar.edu.utn.frba.ddsi.donaciones.models.enums;

import java.math.BigDecimal;
import java.math.RoundingMode;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;

/**
 * En que se mide la cantidad de una necesidad ("30 kilogramos de fideos").
 * Los bienes donados siempre se cuentan en unidades y traen su peso y volumen por unidad,
 * asi que cada unidad de medida sabe cuanto aporta un bien: 5 paquetes de 5 kg = 25 kilogramos.
 * Si al bien le falta el dato (peso o volumen), no suma: no se puede convertir.
 */
public enum UnidadDeMedida {
    UNIDADES, KILOGRAMOS, LITROS;

    private static final double LITROS_POR_M3 = 1000.0;
    // precision con la que se compara y se muestra lo asignado: 10 g, 10 ml o 0,01 unidades
    private static final int DECIMALES = 2;

    public double cantidadDe(Bien bien) {
        if (bien == null || bien.getCantidad() == null)
            return 0.0;

        return switch (this) {
            case UNIDADES -> bien.getCantidad();
            case KILOGRAMOS -> bien.calcularPesoTotal();
            // aproximado: es el volumen del envase
            case LITROS -> bien.calcularVolumenTotal() * LITROS_POR_M3;
        };
    }

    /**
     * Redondea una suma de cantidades. Sumar decimales en double deja restos (7 x 0,1 kg da 0,7999...):
     * sin redondear, una necesidad de 0,8 kg cubierta justo no quedaria satisfecha.
     * Se usa el mismo valor para decidir si esta satisfecha y para mostrarlo, asi nunca difieren.
     */
    public static double redondear(double cantidad) {
        return BigDecimal.valueOf(cantidad).setScale(DECIMALES, RoundingMode.HALF_UP).doubleValue();
    }
}
