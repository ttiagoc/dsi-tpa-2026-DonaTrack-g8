package ar.edu.utn.frba.ddsi.donaciones.services;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.ImportarCsv;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.impl.ImportadorDonantesServiceImpl;

@DisplayName("Tests del ImportadorDonantesService")
class ImportadorDonantesServiceTest {

    private DonanteRepository donanteRepository;
    private ImportarCsv importarCsv;
    private RestTemplate restTemplate;
    private RestDonacionesConfig properties;
    private ImportadorDonantesServiceImpl importadorService;

    @BeforeEach
    void setUp() {
        donanteRepository = mock(DonanteRepository.class);
        importarCsv = mock(ImportarCsv.class);
        restTemplate = mock(RestTemplate.class);
        properties = mock(RestDonacionesConfig.class);

        when(properties.getNotificacionesUrl()).thenReturn("http://localhost:8082/api/notificaciones-service");

        importadorService = new ImportadorDonantesServiceImpl(donanteRepository, importarCsv, restTemplate, properties);
    }

    @Test
    @DisplayName("Debe importar y guardar nuevos donantes notificándolos exitosamente")
    void importarNuevosDonantesYNotificar() {
        MedioContacto emailHumana = new MedioContacto("humana@test.com", TipoContacto.EMAIL);
        PersonaHumana humana = new PersonaHumana(
                null,
                new ArrayList<>(List.of(emailHumana)),
                emailHumana,
                "Ana",
                "Lopez",
                null,
                "11222333",
                null,
                null);

        MedioContacto emailJuridica = new MedioContacto("juridica@test.com", TipoContacto.EMAIL);
        PersonaJuridica juridica = new PersonaJuridica(
                null,
                new ArrayList<>(List.of(emailJuridica)),
                emailJuridica,
                "Empresa Test",
                null,
                null,
                "30-11222333-4",
                new ArrayList<>());

        List<Donante> donantesCsv = List.of(humana, juridica);
        when(importarCsv.importar("test_path.csv")).thenReturn(donantesCsv);

        when(donanteRepository.buscarPorEmail(anyString())).thenReturn(Optional.empty());

        importadorService.importarDonantes("test_path.csv");

        verify(donanteRepository, times(1)).save(humana);
        verify(donanteRepository, times(1)).save(juridica);

        verify(restTemplate, times(2)).postForObject(
                eq("http://localhost:8082/api/notificaciones-service/notificar"),
                any(NotificacionRequest.class),
                eq(Void.class)
        );
    }

    @Test
    @DisplayName("Debe actualizar los datos de donantes si ya existen y no notificar")
    void actualizarDonantesExistentes() {
        MedioContacto emailHumana = new MedioContacto("humana@test.com", TipoContacto.EMAIL);
        PersonaHumana humana = new PersonaHumana(
                null,
                new ArrayList<>(List.of(emailHumana)),
                emailHumana,
                "Ana",
                null,
                null,
                null,
                null,
                null);

        List<Donante> donantesCsv = List.of(humana);
        when(importarCsv.importar("test_path.csv")).thenReturn(donantesCsv);

        PersonaHumana humanaExistente = new PersonaHumana(
                1L,
                new ArrayList<>(List.of(emailHumana)),
                emailHumana,
                "Ana",
                "Vieja",
                null,
                "11111111",
                null,
                null);
        when(donanteRepository.buscarPorEmail("humana@test.com")).thenReturn(Optional.of(humanaExistente));

        importadorService.importarDonantes("test_path.csv");

        verify(donanteRepository, times(1)).save(humanaExistente);

        verify(restTemplate, times(0)).postForObject(
                anyString(),
                any(NotificacionRequest.class),
                eq(Void.class)
        );
    }
}

