package ar.edu.utn.frba.ddsi.donaciones;

import ar.edu.utn.frba.ddsi.common.Email;
import ar.edu.utn.frba.ddsi.common.MedioContacto;
import ar.edu.utn.frba.ddsi.common.Telefono;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        // 1. Preparar un archivo CSV temporal
        Path tempFile = Files.createTempFile("donantes_test", ".csv");
        
        List<String> lineas = List.of(
            "TipoPersona,TipoDoc,Documento,Nombre/Razón Social,Email,Teléfono",
            "HUMANA,DNI,12345678,Ana Perez,ana@mail.com,+54 11 5555-5555",
            "JURIDICA,CUIT,30-12345678-9,Arcos Plateados S.A.,contacto@empresa.com,+54 11 4444-4444",
            // Esta línea actualizará a Ana Perez (mismo email) cambiando su DNI y nombre completo
            "HUMANA,DNI,87654321,Ana Gomez Perez,ana@mail.com,+54 11 9999-9999"
        );
        Files.write(tempFile, lineas);

        // 2. Ejecutar la importación
        importadorDonantesService.importarDonantes(tempFile.toAbsolutePath().toString());

        // 3. Verificar los resultados
        List<PersonaHumana> humanas = donanteRepository.obtenerTodasLasHumanas();
        List<PersonaJuridica> juridicas = donanteRepository.obtenerTodasLasJuridicas();

        // Validar que se creó y actualizó el donante humano
        Assertions.assertEquals(1, humanas.size(), "Debería haber solo una persona humana por la actualización");
        PersonaHumana humana = humanas.getFirst();
        Assertions.assertEquals("Ana", humana.getNombre());
        Assertions.assertEquals("Gomez Perez", humana.getApellido());
        Assertions.assertEquals("87654321", humana.getNroDocumento());
        
        // Verificar contactos de la humana
        Assertions.assertEquals(2, humana.getContactos().size());
        Optional<Email> emailOpt = humana.getContactos().stream()
            .filter(c -> c instanceof Email).map(c -> (Email) c).findFirst();
        Assertions.assertTrue(emailOpt.isPresent());
        Assertions.assertEquals("ana@mail.com", emailOpt.get().getValor());

        Optional<Telefono> telOpt = humana.getContactos().stream()
            .filter(c -> c instanceof Telefono).map(c -> (Telefono) c).findFirst();
        Assertions.assertTrue(telOpt.isPresent());
        Assertions.assertEquals("+54 11 9999-9999", telOpt.get().getValor());
        Assertions.assertSame(humana.getContactoPredeterminado(), emailOpt.get());

        // Validar que se creó la persona jurídica
        Assertions.assertEquals(1, juridicas.size());
        PersonaJuridica juridica = juridicas.getFirst();
        Assertions.assertEquals("Arcos Plateados S.A.", juridica.getRazonSocial());
        Assertions.assertEquals("30-12345678-9", juridica.getNroDocumento());
        Assertions.assertEquals(TipoOrganizacion.EMPRESA, juridica.getTipo());

        // Borrar el archivo temporal
        Files.deleteIfExists(tempFile);
    }

    @Test
    void testNecesidadExtraordinariaSatisfecha() {
        // Preparar modelo
        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setNombre("Sillas");

        NecesidadExtraordinaria tipo = new NecesidadExtraordinaria();
        Necesidad necesidad = new Necesidad(subcategoria, tipo, "Necesidad de sillas para aula", 10.0);

        Assertions.assertFalse(necesidad.getSatisfecha(), "Inicialmente no debería estar satisfecha");

        // Crear una donación con 4 sillas
        Bien bien1 = new Bien();
        bien1.setSubcategoria(subcategoria);
        bien1.setCantidad(4.0);
        Donacion donacion1 = new Donacion(bien1, LocalDateTime.now());

        necesidad.recibirDonacion(donacion1);
        Assertions.assertFalse(necesidad.getSatisfecha(), "Con 4 de 10 no debería estar satisfecha");

        // Crear otra donación con 6 sillas
        Bien bien2 = new Bien();
        bien2.setSubcategoria(subcategoria);
        bien2.setCantidad(6.0);
        Donacion donacion2 = new Donacion(bien2, LocalDateTime.now());

        necesidad.recibirDonacion(donacion2);
        Assertions.assertTrue(necesidad.getSatisfecha(), "Con 10 de 10 debería estar satisfecha");
    }

    @Test
    void testNecesidadRecurrenteSatisfecha() {
        Subcategoria subcategoria = new Subcategoria();
        subcategoria.setNombre("Fideos");

        NecesidadRecurrente tipo = new NecesidadRecurrente();
        tipo.setPeriodo(Periodo.SEMANAL);
        Necesidad necesidad = new Necesidad(subcategoria, tipo, "Fideos semanales", 50.0);

        // Donación dentro de la semana actual
        Bien bien1 = new Bien();
        bien1.setSubcategoria(subcategoria);
        bien1.setCantidad(30.0);
        Donacion donacionActual = new Donacion(bien1, LocalDateTime.now());

        // Donación fuera de la semana actual (hace 3 semanas)
        Bien bien2 = new Bien();
        bien2.setSubcategoria(subcategoria);
        bien2.setCantidad(40.0);
        Donacion donacionPasada = new Donacion(bien2, LocalDateTime.now().minusWeeks(3));

        necesidad.recibirDonacion(donacionActual);
        necesidad.recibirDonacion(donacionPasada);

        // La donación de hace 3 semanas no debería contar para el período semanal actual
        Assertions.assertFalse(necesidad.getSatisfecha(), "Solo se deben contar las donaciones de la semana actual");

        // Agregar otra donación en la semana actual que complete la meta
        Bien bien3 = new Bien();
        bien3.setSubcategoria(subcategoria);
        bien3.setCantidad(25.0);
        Donacion donacionActual2 = new Donacion(bien3, LocalDateTime.now());

        necesidad.recibirDonacion(donacionActual2);
        Assertions.assertTrue(necesidad.getSatisfecha(), "La suma de las donaciones semanales actuales (55.0) supera la meta (50.0)");
    }
}
