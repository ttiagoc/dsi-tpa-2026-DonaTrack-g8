package ar.edu.utn.frba.ddsi.donaciones.models.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadExtraordinaria;

@DisplayName("Tests de UnidadDeMedida: cuanto aporta un bien a una necesidad")
class UnidadDeMedidaTest {

    private Subcategoria fideos;
    private Bien paquetesDeFideos;

    @BeforeEach
    void setUp() {
        fideos = new Subcategoria("Fideos secos", new Categoria("Alimentos", false, true));
        // 20 paquetes de 0,5 kg y 0,001 m3 (1 litro) cada uno
        paquetesDeFideos = new Bien("Fideos", 20L, 0.5, 0.001, fideos, null, LocalDate.now().plusMonths(6));
    }

    @Test
    @DisplayName("En unidades cuenta la cantidad de bienes")
    void unidades() {
        assertEquals(20.0, UnidadDeMedida.UNIDADES.cantidadDe(paquetesDeFideos));
    }

    @Test
    @DisplayName("En kilogramos usa el peso por unidad del bien")
    void kilogramos() {
        assertEquals(10.0, UnidadDeMedida.KILOGRAMOS.cantidadDe(paquetesDeFideos));
    }

    @Test
    @DisplayName("En litros usa el volumen por unidad del bien")
    void litros() {
        assertEquals(20.0, UnidadDeMedida.LITROS.cantidadDe(paquetesDeFideos), 0.0001);
    }

    @Test
    @DisplayName("Un bien sin peso no suma a una necesidad en kilogramos")
    void sinPesoNoSuma() {
        Bien sinPeso = new Bien("Fideos", 20L, null, null, fideos, null, LocalDate.now().plusMonths(6));

        assertEquals(0.0, UnidadDeMedida.KILOGRAMOS.cantidadDe(sinPeso));
        assertEquals(0.0, UnidadDeMedida.LITROS.cantidadDe(sinPeso));
        assertEquals(20.0, UnidadDeMedida.UNIDADES.cantidadDe(sinPeso));
    }

    @Test
    @DisplayName("Una necesidad en kilogramos se cubre con el peso de los bienes asignados")
    void necesidadEnKilogramos() {
        EntidadBeneficiaria comedor = new EntidadBeneficiaria("Comedor", "Escobar 123", "123", new ArrayList<>());
        Necesidad diezKilos = new Necesidad(fideos, new NecesidadExtraordinaria(), "Fideos", 10L, UnidadDeMedida.KILOGRAMOS);
        Necesidad cincuentaKilos = new Necesidad(fideos, new NecesidadExtraordinaria(), "Fideos", 50L, UnidadDeMedida.KILOGRAMOS);

        new Donacion(paquetesDeFideos, LocalDateTime.now()).asignarA(comedor, diezKilos);
        new Donacion(paquetesDeFideos, LocalDateTime.now()).asignarA(comedor, cincuentaKilos);

        assertEquals(10.0, diezKilos.cantidadAsignada());
        assertTrue(diezKilos.estaSatisfecha());
        assertEquals(10.0, cincuentaKilos.cantidadAsignada());
        assertFalse(cincuentaKilos.estaSatisfecha());
    }
}
