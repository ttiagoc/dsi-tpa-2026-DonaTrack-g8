package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.RestTemplate;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.NotificacionRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@DisplayName("Tests de ImportadorDeDonantes")
class ImportadorDeDonantesTest {

    private DonanteRepository donanteRepository;
    private RestTemplate restTemplate;
    private RestDonacionesConfig properties;
    private ImportadorDeDonantes importadorService;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        donanteRepository = mock(DonanteRepository.class);
        restTemplate = mock(RestTemplate.class);
        properties = mock(RestDonacionesConfig.class);

        when(properties.getNotificacionesUrl()).thenReturn("http://localhost:8082/api/notificaciones-service");

        importadorService = new ImportadorDeDonantes(donanteRepository, restTemplate, properties);
    }

    @Test
    @DisplayName("Debe importar y guardar nuevos donantes notificándolos exitosamente")
    void importarNuevosDonantesYNotificar() throws IOException {
        Path tempFile = tempDir.resolve("donantes_nuevos.csv");
        String content = "TipoPersona,Documento,Nombre,Email,Telefono\n" +
                "HUMANA,,11222333,Ana Lopez,humana@test.com,\n" +
                "JURIDICA,,30-11222333-4,Empresa Test,juridica@test.com,";
        Files.writeString(tempFile, content);

        when(donanteRepository.buscarPorEmail(anyString())).thenReturn(Optional.empty());

        importadorService.importarDonantes(tempFile.toString());

        ArgumentCaptor<Donante> captor = ArgumentCaptor.forClass(Donante.class);
        verify(donanteRepository, times(2)).save(captor.capture());
        
        List<Donante> saved = captor.getAllValues();
        PersonaHumana humana = (PersonaHumana) saved.get(0);
        PersonaJuridica juridica = (PersonaJuridica) saved.get(1);

        assertEquals("Ana", humana.getNombre());
        assertEquals("Lopez", humana.getApellido());
        assertEquals("11222333", humana.getDni());
        assertEquals("humana@test.com", humana.getContactos().get(0).getValor());

        assertEquals("Empresa Test", juridica.getRazonSocial());
        assertEquals("30-11222333-4", juridica.getCuit());
        assertEquals("juridica@test.com", juridica.getContactos().get(0).getValor());

        verify(restTemplate, times(2)).postForObject(
                eq("http://localhost:8082/api/notificaciones-service/notificar"),
                any(NotificacionRequest.class),
                eq(Void.class)
        );
    }

    @Test
    @DisplayName("Debe actualizar los datos de donantes si ya existen y no notificar")
    void actualizarDonantesExistentes() throws IOException {
        Path tempFile = tempDir.resolve("donantes_existentes.csv");
        String content = "TipoPersona,Documento,Nombre,Email,Telefono\n" +
                "HUMANA,,11111111,Ana Lopez,humana@test.com,";
        Files.writeString(tempFile, content);

        MedioContacto emailHumana = new MedioContacto("humana@test.com", TipoContacto.EMAIL);
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

        importadorService.importarDonantes(tempFile.toString());

        verify(donanteRepository, times(1)).save(humanaExistente);
        assertEquals("Ana", humanaExistente.getNombre());
        assertEquals("Lopez", humanaExistente.getApellido());
        assertEquals("11111111", humanaExistente.getDni());

        verify(restTemplate, times(0)).postForObject(
                anyString(),
                any(NotificacionRequest.class),
                eq(Void.class)
        );
    }
}
