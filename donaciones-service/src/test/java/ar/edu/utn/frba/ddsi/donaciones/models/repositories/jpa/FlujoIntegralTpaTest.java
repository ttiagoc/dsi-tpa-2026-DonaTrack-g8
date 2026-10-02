package ar.edu.utn.frba.ddsi.donaciones.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.CambioEstado;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.ResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.SegmentadorDeDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Representante;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoPropuesta;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoOrganizacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaCategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaDonante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaEntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaRegistroDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaResultadoMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaSubcategoria;

@DisplayName("Validacion Integral de Requerimientos: TPA 1, TPA 2 y TPA 3")
class FlujoIntegralTpaTest extends PersistenciaTest {

    private final JpaCategoria categorias = new JpaCategoria();
    private final JpaSubcategoria subcategorias = new JpaSubcategoria();
    private final JpaDonante donantes = new JpaDonante();
    private final JpaRegistroDonacion registros = new JpaRegistroDonacion();
    private final JpaDonacion donaciones = new JpaDonacion();
    private final JpaEntidadBeneficiaria entidades = new JpaEntidadBeneficiaria();
    private final JpaResultadoMatchmaking propuestas = new JpaResultadoMatchmaking();

    @Test
    @DisplayName("TPA 1, 2 y 3: Flujo de Donantes, Segmentacion, Necesidades, Matchmaking y Trazabilidad de Estados")
    void testFlujoCompletoConsignas() {
        // =========================================================================
        // 1. TPA 1: Catálogo y Categorías (Alimentos perecederos vs Mobiliario)
        // =========================================================================
        Categoria alimentos = categorias.save(new Categoria("Alimentos", false, true));
        Subcategoria fideos = subcategorias.save(new Subcategoria("Fideos secos", alimentos));
        Subcategoria tomate = subcategorias.save(new Subcategoria("Tomate tetra-pack", alimentos));

        Categoria mobiliario = categorias.save(new Categoria("Mobiliario", true, false));
        Subcategoria sillas = subcategorias.save(new Subcategoria("Sillas de oficina", mobiliario));

        // =========================================================================
        // 2. TPA 1: Donante Humano (contactos obligatorios, opcionales, predeterminado)
        // =========================================================================
        MedioContacto emailAna = new MedioContacto("ana.perez@mail.com", TipoContacto.EMAIL);
        MedioContacto smsAna = new MedioContacto("+541155555555", TipoContacto.SMS);
        MedioContacto wsAna = new MedioContacto("+541155555555", TipoContacto.WHATSAPP);

        PersonaHumana ana = new PersonaHumana(
                List.of(emailAna, smsAna, wsAna),
                emailAna,
                "Ana",
                "Perez",
                LocalDate.of(1992, 4, 15),
                "36123456",
                "F",
                "Av. Medrano 951"
        );
        Donante anaGuardada = donantes.save(ana);
        assertNotNull(anaGuardada.getId());

        // TPA 1: Donante Jurídico (Razón social, CUIT, rubro, tipo y representantes)
        MedioContacto emailEmpresa = new MedioContacto("contacto@arcosplateados.com", TipoContacto.EMAIL);
        Representante rep = new Representante("Carlos", "Gomez", new MedioContacto("carlos@arcos.com", TipoContacto.EMAIL));
        PersonaJuridica arcos = new PersonaJuridica(
                List.of(emailEmpresa),
                emailEmpresa,
                "Arcos Plateados S.A.",
                "Gastronomia",
                TipoOrganizacion.EMPRESA,
                "30-12345678-9",
                List.of(rep)
        );
        Donante arcosGuardado = donantes.save(arcos);
        assertNotNull(arcosGuardado.getId());

        // =========================================================================
        // 3. TPA 1: Carga única heterogénea y Segmentación Automática
        // Consigna: Fideos y tomate con fecha de vencimiento + sillas usadas
        // =========================================================================
        RegistroDonacion registro = registros.save(new RegistroDonacion(anaGuardada, "Donacion comunitaria"));

        Bien bienFideos = new Bien("Fideos secos tallarines", 100L, 0.5, 0.001, fideos, EstadoBien.NUEVO, LocalDate.of(2027, 1, 1));
        Bien bienTomate = new Bien("Tomate frito tetra-pack", 50L, 0.52, 0.0008, tomate, EstadoBien.NUEVO, LocalDate.of(2027, 1, 1));
        Bien bienSillas = new Bien("Sillas ergonomicas", 6L, 7.5, 0.35, sillas, EstadoBien.USADO, null);

        SegmentadorDeDonacion segmentador = new SegmentadorDeDonacion();
        List<Donacion> donacionesSegmentadas = segmentador.segmentarDonacion(registro, List.of(bienFideos, bienTomate, bienSillas));

        // Debe generar exactamente 3 donaciones independientes, una por cada subcategoría
        assertEquals(3, donacionesSegmentadas.size(), "La segmentacion debe generar 3 donaciones separadas");
        List<Donacion> donacionesGuardadas = donaciones.saveAll(donacionesSegmentadas);
        assertEquals(3, donacionesGuardadas.size());

        Donacion donacionFideos = donacionesGuardadas.stream()
                .filter(d -> d.getSubcategoria().getNombre().equals("Fideos secos"))
                .findFirst()
                .orElseThrow();
        assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacionFideos.estadoActual(), "La donacion inicia EN_DEPOSITO");

        // =========================================================================
        // 4. TPA 1: Entidades Beneficiarias y Necesidades (Recurrentes y Extraordinarias)
        // =========================================================================
        EntidadBeneficiaria comedor = new EntidadBeneficiaria(
                "Comedor Escobar Sonrisas",
                "Calle Falsa 123, Escobar",
                "1122334455",
                List.of(new MedioContacto("comedor@org.com", TipoContacto.EMAIL))
        );
        // Necesidad Recurrente: 80 paquetes de fideos semanales
        Necesidad necFideos = new Necesidad(fideos, new NecesidadRecurrente(Periodo.SEMANAL), "Fideos semanales para viandas", 80L);
        comedor.registrarNecesidad(necFideos);
        entidades.save(comedor);

        EntidadBeneficiaria escuelaRural = new EntidadBeneficiaria(
                "Escuela Rural N°10",
                "Ruta Provincial 3 Km 45",
                "1199887766",
                List.of(new MedioContacto("escuela10@educacion.gob.ar", TipoContacto.EMAIL))
        );
        // Necesidad Extraordinaria por inundacion: 6 sillas (cubrible por donaciones)
        Necesidad necSillas = new Necesidad(sillas, new NecesidadExtraordinaria(), "Reposicion de sillas por inundacion", 6L);
        escuelaRural.registrarNecesidad(necSillas);
        entidades.save(escuelaRural);

        // Validamos la satisfaccion de la necesidad con la donacion de sillas (Consigna TPA 1)
        assertFalse(necSillas.estaSatisfecha());
        Donacion donacionSillas = donacionesGuardadas.stream()
                .filter(d -> d.getSubcategoria().getNombre().equals("Sillas de oficina"))
                .findFirst()
                .orElseThrow();
        necSillas.asignarDonacion(donacionSillas);
        assertTrue(necSillas.estaSatisfecha(), "Con los 6 bienes de la donacion de sillas queda satisfecha");

        // =========================================================================
        // 5. TPA 2 y TPA 3: Matchmaking y Asignación de Donaciones
        // =========================================================================
        nuevoRequest(); // Forzamos nueva sesion de EntityManager para verificar persistencia real

        Donacion donacionRecargada = donaciones.findById(donacionFideos.getId()).orElseThrow();
        EntidadBeneficiaria comedorRecargado = entidades.findById(comedor.getId()).orElseThrow();

        // Creamos propuesta de matchmaking
        ResultadoMatchmaking propuesta = new ResultadoMatchmaking(donacionRecargada, List.of(comedorRecargado));
        ResultadoMatchmaking propuestaGuardada = propuestas.save(propuesta);
        assertNotNull(propuestaGuardada.getId());
        assertEquals(EstadoPropuesta.PENDIENTE, propuestaGuardada.getEstado());

        // Aceptamos la propuesta: cambia el estado de la propuesta y asigna la donacion
        propuestaGuardada.setEstado(EstadoPropuesta.ACEPTADO);
        propuestas.save(propuestaGuardada);

        donacionRecargada.asignarA(comedorRecargado, comedorRecargado.getNecesidades().get(0));
        donaciones.save(donacionRecargada);

        nuevoRequest();

        // =========================================================================
        // 6. TPA 1 y TPA 2: Trazabilidad y Auditoría de Estados de la Donación
        // =========================================================================
        Donacion donacionVerificada = donaciones.findById(donacionFideos.getId()).orElseThrow();
        assertEquals(TipoEstadoDonacion.ASIGNACION_REALIZADA, donacionVerificada.estadoActual());
        assertEquals("Calle Falsa 123, Escobar", donacionVerificada.obtenerDireccion());

        // Transicion: Lista para entregar (planificada en ruta)
        donacionVerificada.cambiarEstado(TipoEstadoDonacion.LISTA_PARA_ENTREGAR, "Asignada a ruta de camion RT123AB");
        donaciones.save(donacionVerificada);

        // Transicion: En traslado (chofer inicio recorrido)
        donacionVerificada.cambiarEstado(TipoEstadoDonacion.EN_TRASLADO, "Camion salio del deposito central");
        donaciones.save(donacionVerificada);

        // Transicion: Entregada con fotos de recepcion
        donacionVerificada.cambiarEstado(TipoEstadoDonacion.ENTREGADA, "Recepcion confirmada con exito por la entidad");
        donacionVerificada.setFotosRecepcion(List.of("https://s3.amazonaws.com/donatrack/fotos/remito_comedor_1.jpg"));
        donaciones.save(donacionVerificada);

        nuevoRequest();

        Donacion donacionFinal = donaciones.findById(donacionFideos.getId()).orElseThrow();
        assertEquals(TipoEstadoDonacion.ENTREGADA, donacionFinal.estadoActual());
        assertEquals(1, donacionFinal.getFotosRecepcion().size());

        // Verificamos auditoría completa de cambios de estado (TPA 1 Requerimiento 4)
        List<CambioEstado> historial = donacionFinal.getHistorialEstados();
        assertTrue(historial.size() >= 4, "Debe registrar cada cambio de estado en la auditoria");
        assertEquals(TipoEstadoDonacion.EN_DEPOSITO, historial.get(0).getEstado());
        assertEquals(TipoEstadoDonacion.ASIGNACION_REALIZADA, historial.get(1).getEstado());
        assertEquals(TipoEstadoDonacion.LISTA_PARA_ENTREGAR, historial.get(2).getEstado());
        assertEquals(TipoEstadoDonacion.EN_TRASLADO, historial.get(3).getEstado());
    }
}
