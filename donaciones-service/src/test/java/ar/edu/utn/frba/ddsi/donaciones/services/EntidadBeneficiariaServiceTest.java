package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.MedioContactoRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadResponse;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.SubcategoriaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.Coordenadas;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.DireccionInvalidaException;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.Geocodificador;
import ar.edu.utn.frba.ddsi.donaciones.services.geocodificacion.GeocodificadorNoDisponibleException;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.EntidadBeneficiariaServiceImpl;

@DisplayName("Tests del EntidadBeneficiariaService")
class EntidadBeneficiariaServiceTest {

    private EntidadBeneficiariaRepository entidadRepository;
    private SubcategoriaRepository subcategoriaRepository;
    private Geocodificador geocodificador;
    private EntidadBeneficiariaServiceImpl entidadService;

    @BeforeEach
    void setUp() {
        entidadRepository = mock(EntidadBeneficiariaRepository.class);
        subcategoriaRepository = mock(SubcategoriaRepository.class);

        geocodificador = mock(Geocodificador.class);
        when(geocodificador.ubicar(anyString())).thenReturn(new Coordenadas(-34.5986, -58.4202));

        entidadService = new EntidadBeneficiariaServiceImpl(entidadRepository, subcategoriaRepository, geocodificador);
    }

    @Test
    @DisplayName("Debe fallar al crear una entidad beneficiaria si faltan datos obligatorios")
    void crearEntidadFaltanDatos() {
        List<MedioContactoRequest> correos = new ArrayList<>();
        correos.add(new MedioContactoRequest("EMAIL", "entidad@org.com"));

        EntidadBeneficiariaRequest requestSinRazonSocial = new EntidadBeneficiariaRequest(
                "", "Medrano 951", "11223344", correos);

        Exception ex = assertThrows(BusinessException.class, () -> {
            entidadService.crear(requestSinRazonSocial);
        });

        assertTrue(ex.getMessage().contains("razon social no puede ser nula ni estar vacia"));
    }

    @Test
    @DisplayName("Debe poder crear exitosamente la entidad beneficiaria")
    void crearEntidadExitosamente() {
        List<MedioContactoRequest> correos = new ArrayList<>();
        correos.add(new MedioContactoRequest("EMAIL", "entidad@org.com"));

        EntidadBeneficiariaRequest requestValido = new EntidadBeneficiariaRequest(
                "Comedor Los Niños", "Medrano 951", "11223344", correos);

        // Al guardar debe retornar la misma entidad con ID 1
        when(entidadRepository.save(any(EntidadBeneficiaria.class))).thenAnswer(invocation -> {
            EntidadBeneficiaria guardada = invocation.getArgument(0);
            guardada.setId(1L);
            return guardada;
        });

        EntidadBeneficiariaResponse response = entidadService.crear(requestValido);

        assertNotNull(response);
        assertEquals(1L, response.id());
        assertEquals("Comedor Los Niños", response.razonSocial());
        assertEquals("Medrano 951", response.direccion());

        verify(entidadRepository, times(1)).save(any(EntidadBeneficiaria.class));
    }

    @Test
    @DisplayName("Debe fallar al registrar una necesidad con una subcategoria que no esta en el catalogo")
    void registrarNecesidadSubcategoriaInexistente() {
        when(entidadRepository.findById(1L)).thenReturn(Optional.of(new EntidadBeneficiaria()));
        when(subcategoriaRepository.findByNombre("Inexistente")).thenReturn(Optional.empty());

        NecesidadRequest request = new NecesidadRequest(new SubcategoriaRequest("Inexistente"),
                "extraordinaria", null, "Sillas para el aula", 30L, null);

        assertThrows(ResourceNotFoundException.class, () -> entidadService.registrarNecesidad(1L, request));
    }

    @Test
    @DisplayName("La necesidad registrada informa su categoria, lo asignado y si esta cubierta")
    void registrarNecesidadDevuelveAvance() {
        when(entidadRepository.findById(1L)).thenReturn(Optional.of(
                new EntidadBeneficiaria("Comedor", "Calle 123", "1145678901", List.of())));
        when(subcategoriaRepository.findByNombre("Frazadas"))
                .thenReturn(Optional.of(new Subcategoria("Frazadas", new Categoria("Ropa y abrigo", true, false))));

        NecesidadResponse respuesta = entidadService.registrarNecesidad(1L, new NecesidadRequest(
                new SubcategoriaRequest("Frazadas"), "extraordinaria", null, "Para el invierno", 30L, null));

        assertEquals("Frazadas", respuesta.subcategoria());
        assertEquals("Ropa y abrigo", respuesta.categoria());
        assertEquals(0L, respuesta.cantidadAsignada());
        assertFalse(respuesta.satisfecha());
        assertEquals("UNIDADES", respuesta.unidad(), "sin unidad se cuenta en unidades");
    }

    @Test
    @DisplayName("La necesidad guarda la unidad en que se mide la cantidad")
    void registrarNecesidadEnKilogramos() {
        when(entidadRepository.findById(1L)).thenReturn(Optional.of(
                new EntidadBeneficiaria("Comedor", "Calle 123", "1145678901", List.of())));
        when(subcategoriaRepository.findByNombre("Fideos"))
                .thenReturn(Optional.of(new Subcategoria("Fideos", new Categoria("Alimentos", false, true))));

        NecesidadResponse respuesta = entidadService.registrarNecesidad(1L, new NecesidadRequest(
                new SubcategoriaRequest("Fideos"), "extraordinaria", null, "Para el comedor", 50L, "kilogramos"));

        assertEquals("KILOGRAMOS", respuesta.unidad());
    }

    @Test
    @DisplayName("Debe fallar al registrar una necesidad con una unidad que no existe")
    void registrarNecesidadUnidadInvalida() {
        when(entidadRepository.findById(1L)).thenReturn(Optional.of(new EntidadBeneficiaria()));
        when(subcategoriaRepository.findByNombre("Fideos"))
                .thenReturn(Optional.of(new Subcategoria("Fideos", new Categoria("Alimentos", false, true))));

        NecesidadRequest request = new NecesidadRequest(new SubcategoriaRequest("Fideos"),
                "extraordinaria", null, "Para el comedor", 50L, "toneladas");

        Exception ex = assertThrows(BusinessException.class, () -> entidadService.registrarNecesidad(1L, request));
        assertTrue(ex.getMessage().contains("Unidad"));
    }

    @Test
    @DisplayName("Debe fallar al registrar una necesidad recurrente sin periodo")
    void registrarNecesidadRecurrenteSinPeriodo() {
        when(entidadRepository.findById(1L)).thenReturn(Optional.of(new EntidadBeneficiaria()));
        when(subcategoriaRepository.findByNombre("Fideos"))
                .thenReturn(Optional.of(new Subcategoria("Fideos", new Categoria("Alimentos", false, true))));

        NecesidadRequest request = new NecesidadRequest(new SubcategoriaRequest("Fideos"),
                "recurrente", null, "Fideos para el comedor", 100L, null);

        Exception ex = assertThrows(BusinessException.class, () -> entidadService.registrarNecesidad(1L, request));
        assertTrue(ex.getMessage().contains("periodo"));
    }

    // ---- Direccion verificada y ubicada con Georef (mapa de entregas) ----

    private EntidadBeneficiariaRequest requestEn(String direccion) {
        return new EntidadBeneficiariaRequest("Comedor Los Niños", direccion, "11223344",
                List.of(new MedioContactoRequest("EMAIL", "entidad@org.com")));
    }

    private void guardarDevuelveLaMisma() {
        when(entidadRepository.save(any(EntidadBeneficiaria.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Al crear la entidad guarda donde queda su direccion")
    void crearGuardaLaUbicacion() {
        guardarDevuelveLaMisma();

        entidadService.crear(requestEn("Medrano 951, Almagro, CABA"));

        verify(entidadRepository).save(org.mockito.ArgumentMatchers.argThat(e ->
                e.getLatitud() == -34.5986 && e.getLongitud() == -58.4202));
    }

    @Test
    @DisplayName("No se crea la entidad si su direccion no existe")
    void crearConDireccionInexistente() {
        when(geocodificador.ubicar("Calle Inventada 99999, CABA"))
                .thenThrow(DireccionInvalidaException.inexistente("Calle Inventada 99999, CABA"));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> entidadService.crear(requestEn("Calle Inventada 99999, CABA")));

        assertTrue(ex.getMessage().startsWith("La dirección"), "la web lo marca en el campo dirección");
        verify(entidadRepository, never()).save(any());
    }

    @Test
    @DisplayName("Si Georef no responde, la entidad se crea igual, sin ubicacion")
    void crearSinGeoref() {
        guardarDevuelveLaMisma();
        when(geocodificador.ubicar(anyString())).thenThrow(new GeocodificadorNoDisponibleException("sin red", null));

        entidadService.crear(requestEn("Medrano 951, Almagro, CABA"));

        verify(entidadRepository).save(org.mockito.ArgumentMatchers.argThat(e -> !e.estaUbicada()));
    }

    @Test
    @DisplayName("Al cambiar la direccion se vuelve a ubicar; si no cambia, no se consulta Georef")
    void actualizarDireccion() {
        guardarDevuelveLaMisma();
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Comedor", "Medrano 951, Almagro, CABA", "1145678901", List.of());
        entidad.ubicarEn(-34.5986, -58.4202);
        when(entidadRepository.findById(1L)).thenReturn(Optional.of(entidad));

        entidadService.actualizar(1L, requestEn("Medrano 951, Almagro, CABA"));
        verify(geocodificador, never()).ubicar(anyString());

        when(geocodificador.ubicar("Av. Rivadavia 6500, Flores, CABA")).thenReturn(new Coordenadas(-34.6283, -58.4639));
        entidadService.actualizar(1L, requestEn("Av. Rivadavia 6500, Flores, CABA"));
        assertEquals(-34.6283, entidad.getLatitud());
        assertEquals(-58.4639, entidad.getLongitud());
    }

    @Test
    @DisplayName("Ubica las entidades que todavia no tienen ubicacion y saltea las que no se encuentran")
    void ubicarPendientes() {
        guardarDevuelveLaMisma();
        EntidadBeneficiaria yaUbicada = new EntidadBeneficiaria("A", "Medrano 951, Almagro, CABA", "1", List.of());
        yaUbicada.ubicarEn(-34.6, -58.4);
        EntidadBeneficiaria sinUbicar = new EntidadBeneficiaria("B", "Av. Rivadavia 6500, Flores, CABA", "1", List.of());
        EntidadBeneficiaria inexistente = new EntidadBeneficiaria("C", "Calle 123", "1", List.of());
        when(entidadRepository.findAll()).thenReturn(List.of(yaUbicada, sinUbicar, inexistente));
        when(geocodificador.ubicar("Calle 123")).thenThrow(DireccionInvalidaException.incompleta());

        assertEquals(1, entidadService.ubicarPendientes());
        assertTrue(sinUbicar.estaUbicada());
        assertFalse(inexistente.estaUbicada());
        verify(geocodificador, never()).ubicar("Medrano 951, Almagro, CABA");
    }
}
