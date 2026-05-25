package ar.edu.utn.frba.ddsi.donaciones;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.*;
import ar.edu.utn.frba.ddsi.donaciones.repositories.DonanteRepository;
import ar.edu.utn.frba.ddsi.donaciones.services.ImportadorDonantesService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
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

    // --- TESTS DE IMPORTACIÓN ---

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

    // --- TESTS DE SEGMENTACIÓN ---

    @Test
    @DisplayName("Segmentación Arcos Plateados: Agrupar por subcategoría (Sillas y Mesas)")
    void testSegmentacionMuebles() {
        Categoria mobiliario = crearCategoria("Mobiliario", true, false);
        Subcategoria sillas = crearSubcategoria("Sillas", mobiliario);
        Subcategoria mesas = crearSubcategoria("Mesas", mobiliario);

        Bien b1 = crearBien("Silla oficina", 6.0, sillas, EstadoBien.USADO, null);
        Bien b2 = crearBien("Mesa rectangular", 1.0, mesas, EstadoBien.USADO, null);

        RegistroDonacion registro = new RegistroDonacion();
        registro.setFecha(LocalDateTime.now());
        registro.setBienes(Arrays.asList(b1, b2));

        registro.segmentarDonacion();

        Assertions.assertEquals(2, registro.getDonacionesSegmentadas().size());

        Donacion donacionSillas = registro.getDonacionesSegmentadas().stream()
            .filter(d -> d.getSubcategoria().getNombre().equals("Sillas"))
            .findFirst()
            .orElseThrow(() -> new AssertionError("No se encontró una donación segmentada para la subcategoría 'Sillas'"));

        Assertions.assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacionSillas.estadoActual());
    }

    @Test
    @DisplayName("Segmentación Alimentos: Diferenciar por fecha de vencimiento")
    void testSegmentacionVencimientos() {
        Categoria alimentos = crearCategoria("Alimentos", false, true);
        Subcategoria fideos = crearSubcategoria("Fideos", alimentos);
        LocalDate venc2027 = LocalDate.of(2027, 1, 1);
        LocalDate vencPronto = LocalDate.now().plusDays(5);

        Bien f1 = crearBien("Fideos Lote A", 100.0, fideos, null, venc2027);
        Bien f2 = crearBien("Fideos Lote B", 20.0, fideos, null, vencPronto);

        RegistroDonacion registro = new RegistroDonacion();
        registro.setFecha(LocalDateTime.now());
        registro.setBienes(Arrays.asList(f1, f2));

        registro.segmentarDonacion();

        Assertions.assertEquals(2, registro.getDonacionesSegmentadas().size(), "Deben separarse por fecha de vencimiento aunque sean la misma subcategoría");
    }

    // --- TESTS DE NECESIDADES ---

    @Test
    void testNecesidadExtraordinariaSatisfecha() {
        Subcategoria subcategoria = crearSubcategoria("Sillas", crearCategoria("Muebles", true, false));
        NecesidadExtraordinaria tipo = new NecesidadExtraordinaria();
        Necesidad necesidad = new Necesidad(subcategoria, tipo, "Sillas para aula", 10.0);

        Bien bien1 = crearBien("Sillas", 10.0, subcategoria, EstadoBien.NUEVO, null);
        Donacion donacion = new Donacion(bien1, LocalDateTime.now());

        necesidad.recibirDonacion(donacion);
        Assertions.assertTrue(necesidad.getSatisfecha());
    }

    @Test
    void testNecesidadRecurrenteSatisfecha() {
        Subcategoria subcategoria = crearSubcategoria("Fideos", crearCategoria("Alimentos", false, true));
        NecesidadRecurrente tipo = new NecesidadRecurrente();
        tipo.setPeriodo(Periodo.SEMANAL);
        Necesidad necesidad = new Necesidad(subcategoria, tipo, "Fideos semanales", 50.0);

        Bien bienActual = crearBien("Fideos", 55.0, subcategoria, null, null);
        Donacion donacionActual = new Donacion(bienActual, LocalDateTime.now());

        necesidad.recibirDonacion(donacionActual);
        Assertions.assertTrue(necesidad.getSatisfecha());
    }

    // --- MÉTODOS AUXILIARES ---

    private Bien crearBien(String desc, Double cant, Subcategoria sub, EstadoBien estado, LocalDate venc) {
        Bien b = new Bien();
        b.setDescripcion(desc);
        b.setCantidad(cant);
        b.setSubcategoria(sub);
        b.setEstadoBien(estado);
        b.setFechaVencimiento(venc);
        return b;
    }

    private Categoria crearCategoria(String nombre, boolean pideEstado, boolean perecedero) {
        Categoria c = new Categoria();
        c.setNombre(nombre);
        c.setPideEstado(pideEstado);
        c.setEsPerecedero(perecedero);
        return c;
    }

    private Subcategoria crearSubcategoria(String nombre, Categoria cat) {
        Subcategoria s = new Subcategoria();
        s.setNombre(nombre);
        s.setCategoria(cat);
        return s;
    }
}