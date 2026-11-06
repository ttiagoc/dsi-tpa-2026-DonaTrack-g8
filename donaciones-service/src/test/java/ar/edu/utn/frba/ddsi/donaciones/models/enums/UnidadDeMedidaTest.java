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
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadRecurrente;

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

    private final EntidadBeneficiaria comedor =
            new EntidadBeneficiaria("Comedor", "Escobar 123", "123", new ArrayList<>());

    private Bien fideos(long cantidad, double kgPorUnidad) {
        return new Bien("Fideos", cantidad, kgPorUnidad, 0.001, fideos, null, LocalDate.now().plusMonths(6));
    }

    private void donar(Bien bien, Necesidad necesidad) {
        new Donacion(bien, LocalDateTime.now()).asignarA(comedor, necesidad);
    }

    @Test
    @DisplayName("30 kg de fideos: 5 paquetes de 5 kg son 25 kg y no alcanzan; con 1 paquete mas de 5 kg se cubre")
    void treintaKilosConPaquetesDeCincoKilos() {
        Necesidad treintaKilos = new Necesidad(fideos, new NecesidadExtraordinaria(), "Fideos", 30L, UnidadDeMedida.KILOGRAMOS);

        donar(fideos(5, 5.0), treintaKilos);
        assertEquals(25.0, treintaKilos.cantidadAsignada());
        assertFalse(treintaKilos.estaSatisfecha());

        donar(fideos(1, 5.0), treintaKilos);
        assertEquals(30.0, treintaKilos.cantidadAsignada());
        assertTrue(treintaKilos.estaSatisfecha());
    }

    @Test
    @DisplayName("La misma donacion cuenta distinto segun la unidad de la necesidad")
    void mismaDonacionSegunUnidad() {
        Necesidad cincoUnidades = new Necesidad(fideos, new NecesidadExtraordinaria(), "Fideos", 5L, UnidadDeMedida.UNIDADES);
        Necesidad treintaKilos = new Necesidad(fideos, new NecesidadExtraordinaria(), "Fideos", 30L, UnidadDeMedida.KILOGRAMOS);

        donar(fideos(5, 5.0), cincoUnidades);
        donar(fideos(5, 5.0), treintaKilos);

        // 5 paquetes cubren "5 unidades" pero no "30 kg"
        assertEquals(5.0, cincoUnidades.cantidadAsignada());
        assertTrue(cincoUnidades.estaSatisfecha());
        assertEquals(25.0, treintaKilos.cantidadAsignada());
        assertFalse(treintaKilos.estaSatisfecha());
    }

    @Test
    @DisplayName("Una donacion con bienes de distinto peso suma el peso de cada uno")
    void bienesDeDistintoPeso() {
        Necesidad treintaKilos = new Necesidad(fideos, new NecesidadExtraordinaria(), "Fideos", 30L, UnidadDeMedida.KILOGRAMOS);
        Donacion donacion = new Donacion(fideos(4, 5.0), LocalDateTime.now());
        donacion.agregarBien(fideos(20, 0.5));
        donacion.asignarA(comedor, treintaKilos);

        assertEquals(30.0, treintaKilos.cantidadAsignada(), "4 x 5 kg + 20 x 0,5 kg");
        assertTrue(treintaKilos.estaSatisfecha());
    }

    @Test
    @DisplayName("Una necesidad en litros convierte el volumen en m3 de cada unidad")
    void necesidadEnLitros() {
        Subcategoria leche = new Subcategoria("Leche", new Categoria("Alimentos", false, true));
        Necesidad veinteLitros = new Necesidad(leche, new NecesidadExtraordinaria(), "Leche", 20L, UnidadDeMedida.LITROS);
        // 12 cajas de 1 litro y 6 botellas de 1,5 litros
        Donacion donacion = new Donacion(new Bien("Leche", 12L, 1.03, 0.001, leche, null, LocalDate.now().plusMonths(3)),
                LocalDateTime.now());
        donacion.agregarBien(new Bien("Leche", 6L, 1.55, 0.0015, leche, null, LocalDate.now().plusMonths(3)));
        donacion.asignarA(comedor, veinteLitros);

        assertEquals(21.0, veinteLitros.cantidadAsignada());
        assertTrue(veinteLitros.estaSatisfecha());
    }

    @Test
    @DisplayName("Los restos de sumar decimales no dejan sin cubrir una necesidad cubierta justo")
    void sumaDeDecimales() {
        // 0,1 sumado 8 veces en double da 0,7999...: con 8 paquetes de 100 g, 1 kg no se cubre
        // y 8 x 100 g + 2 x 100 g si
        Necesidad unKilo = new Necesidad(fideos, new NecesidadExtraordinaria(), "Fideos", 1L, UnidadDeMedida.KILOGRAMOS);
        for (int i = 0; i < 8; i++) {
            donar(fideos(1, 0.1), unKilo);
        }
        assertEquals(0.8, unKilo.cantidadAsignada());
        assertFalse(unKilo.estaSatisfecha());

        donar(fideos(1, 0.1), unKilo);
        donar(fideos(1, 0.1), unKilo);
        assertEquals(1.0, unKilo.cantidadAsignada());
        assertTrue(unKilo.estaSatisfecha());
    }

    @Test
    @DisplayName("En una necesidad recurrente solo cuentan los kilos del periodo actual")
    void recurrenteEnKilogramos() {
        Necesidad diezKilosPorMes = new Necesidad(fideos, new NecesidadRecurrente(Periodo.MENSUAL), "Fideos", 10L,
                UnidadDeMedida.KILOGRAMOS);
        donar(fideos(2, 5.0), diezKilosPorMes);
        new Donacion(fideos(10, 5.0), LocalDateTime.now().minusMonths(2)).asignarA(comedor, diezKilosPorMes);

        assertEquals(10.0, diezKilosPorMes.cantidadAsignada(), "los 50 kg de hace dos meses no cuentan");
        assertTrue(diezKilosPorMes.estaSatisfecha());
    }
}
