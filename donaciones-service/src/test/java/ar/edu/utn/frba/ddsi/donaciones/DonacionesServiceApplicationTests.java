package ar.edu.utn.frba.ddsi.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.*;
import ar.edu.utn.frba.ddsi.donaciones.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.ImportadorDonantesService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@SpringBootTest
public class DonacionesServiceApplicationTests {

    @Autowired
    private DonanteRepository donanteRepository;

    @Autowired
    private ImportadorDonantesService importadorDonantesService;

    @BeforeEach
    void setUp() {
        donanteRepository.limpiar();
    }

    @Test
    void contextLoads() {
        // Verifica que levante el contexto de Spring correctamente
    }

    @Test
    void testImportadorDonantesCSV() throws IOException {
        Path tempFile = Files.createTempFile("donantes_test", ".csv");

        List<String> lineas = List.of(
            "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono",
            "HUMANA,DNI,12345678,Ana Perez,ana@mail.com,+54 11 5555-5555",
            "JURIDICA,CUIT,30-12345678-9,Arcos Plateados S.A.,contacto@empresa.com,+54 11 4444-4444",
            "HUMANA,DNI,87654321,Ana Gomez Perez,ana@mail.com,+54 11 9999-9999"
        );
        Files.write(tempFile, lineas);

        importadorDonantesService.importarDonantes(tempFile.toAbsolutePath().toString());

        List<PersonaHumana> humanas = donanteRepository.obtenerTodasLasHumanas();
        List<PersonaJuridica> juridicas = donanteRepository.obtenerTodasLasJuridicas();

        Assertions.assertEquals(1, humanas.size(), "Debería haber solo una persona humana por la actualización");
        PersonaHumana humana = humanas.getFirst();
        Assertions.assertEquals("Ana Gomez Perez", humana.getNombre() + " " + humana.getApellido());

        Assertions.assertEquals(1, juridicas.size());
        Assertions.assertEquals("Arcos Plateados S.A.", juridicas.getFirst().getRazonSocial());

        Files.deleteIfExists(tempFile);
    }
}