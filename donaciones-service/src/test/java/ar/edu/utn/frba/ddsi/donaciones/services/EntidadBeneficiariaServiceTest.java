package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.EntidadBeneficiariaRepository;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.SubcategoriaRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.EntidadBeneficiariaServiceImpl;

@DisplayName("Tests del EntidadBeneficiariaService")
class EntidadBeneficiariaServiceTest {

    private EntidadBeneficiariaRepository entidadRepository;
    private SubcategoriaRepository subcategoriaRepository;
    private EntidadBeneficiariaServiceImpl entidadService;

    @BeforeEach
    void setUp() {
        entidadRepository = mock(EntidadBeneficiariaRepository.class);
        subcategoriaRepository = mock(SubcategoriaRepository.class);

        entidadService = new EntidadBeneficiariaServiceImpl(entidadRepository, subcategoriaRepository);
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
                "extraordinaria", null, "Sillas para el aula", 30L);

        assertThrows(ResourceNotFoundException.class, () -> entidadService.registrarNecesidad(1L, request));
    }

    @Test
    @DisplayName("Debe fallar al registrar una necesidad recurrente sin periodo")
    void registrarNecesidadRecurrenteSinPeriodo() {
        when(entidadRepository.findById(1L)).thenReturn(Optional.of(new EntidadBeneficiaria()));
        when(subcategoriaRepository.findByNombre("Fideos"))
                .thenReturn(Optional.of(new Subcategoria("Fideos", new Categoria("Alimentos", false, true))));

        NecesidadRequest request = new NecesidadRequest(new SubcategoriaRequest("Fideos"),
                "recurrente", null, "Fideos para el comedor", 100L);

        Exception ex = assertThrows(BusinessException.class, () -> entidadService.registrarNecesidad(1L, request));
        assertTrue(ex.getMessage().contains("periodo"));
    }
}
