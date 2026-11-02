package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.EstadoBien;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoEstadoDonacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Bien;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadRecurrente;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Tests de Entidad Beneficiaria")
class EntidadBeneficiariaTest {

    @Test
    @DisplayName("Debe poder crearse con sus correos y su teléfono correctamente")
    void creacionEntidadBeneficiaria() {
        List<MedioContacto> correos = new ArrayList<>();
        correos.add(new MedioContacto("entidad@org.com", TipoContacto.EMAIL));

        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Fundación UTN", "Medrano 951", "1122334455", correos);

        assertEquals("Fundación UTN", entidad.getRazonSocial());
        assertEquals("Medrano 951", entidad.getDireccion());
        assertEquals("1122334455", entidad.getTelefono().getValor());
        assertEquals(1, entidad.getCorreoRepresentantes().size());
        assertTrue(entidad.getNecesidades().isEmpty());
    }

    @Test
    @DisplayName("Debe poder registrar y eliminar necesidades")
    void gestionNecesidades() {
        List<MedioContacto> correos = new ArrayList<>();
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Fundación UTN", "Medrano 951", "1122334455", correos);

        Categoria catAlimentos = new Categoria("Alimentos", false, true);
        Subcategoria subFideos = new Subcategoria("Fideos", catAlimentos);

        Necesidad necesidad = new Necesidad(subFideos, new NecesidadRecurrente(), "Fideos para comedor", 50L);
        necesidad.setId(1L);

        entidad.registrarNecesidad(necesidad);
        assertEquals(1, entidad.getNecesidades().size());

        entidad.eliminarNecesidad(1L);
        assertTrue(entidad.getNecesidades().isEmpty());
    }

    @Test
    @DisplayName("No elimina una necesidad con donaciones asignadas que todavía no llegaron")
    void noEliminaNecesidadConDonacionesEnCurso() {
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Fundación UTN", "Medrano 951", "1122334455", new ArrayList<>());
        Subcategoria subFideos = new Subcategoria("Fideos", new Categoria("Alimentos", false, true));
        Necesidad necesidad = new Necesidad(subFideos, new NecesidadRecurrente(), "Fideos para comedor", 50L);
        necesidad.setId(1L);
        entidad.registrarNecesidad(necesidad);

        Donacion donacion = new Donacion(new Bien("Fideos moñito", 10L, 0.5, 0.5, subFideos, EstadoBien.NUEVO,
                LocalDateTime.now().plusDays(10).toLocalDate()), LocalDateTime.now());
        donacion.asignarA(entidad, necesidad);
        donacion.cambiarEstado(TipoEstadoDonacion.EN_TRASLADO, "Sale el camión");
        assertEquals(1, necesidad.donacionesEnCurso());

        BusinessException error = assertThrows(BusinessException.class, () -> entidad.eliminarNecesidad(1L));
        assertTrue(error.getMessage().contains("1 donación asignada"));
        assertEquals(1, entidad.getNecesidades().size());
        assertSame(necesidad, donacion.getNecesidad());

        // una vez entregada, se puede eliminar y la donacion queda asignada a la entidad
        entidad.confirmarEntrega(donacion, "AB123CD", LocalDateTime.now());
        assertEquals(0, necesidad.donacionesEnCurso());
        entidad.eliminarNecesidad(1L);
        assertTrue(entidad.getNecesidades().isEmpty());
        assertNull(donacion.getNecesidad());
        assertSame(entidad, donacion.getEntidadBeneficiariaAsignada());
    }

    @Test
    @DisplayName("Debe cambiar el estado de la donación a ENTREGADA al confirmar entrega")
    void confirmarEntregaDonacion() {
        List<MedioContacto> correos = new ArrayList<>();
        EntidadBeneficiaria entidad = new EntidadBeneficiaria("Fundación UTN", "Medrano 951", "1122334455", correos);

        Categoria catAlimentos = new Categoria("Alimentos", false, true);
        Subcategoria subFideos = new Subcategoria("Fideos", catAlimentos);
        Bien bienFideos = new Bien("Fideos moñito", 1L, 0.5, 0.5, subFideos, EstadoBien.NUEVO,
                LocalDateTime.now().plusDays(10).toLocalDate());

        Donacion donacion = new Donacion(bienFideos, LocalDateTime.now());

        // Estado inicial de la donación es EN_DEPOSITO
        assertEquals(TipoEstadoDonacion.EN_DEPOSITO, donacion.estadoActual());

        // La entidad confirma entrega
        entidad.confirmarEntrega(donacion, "AB123CD", LocalDateTime.now());

        assertEquals(TipoEstadoDonacion.ENTREGADA, donacion.estadoActual());
        assertEquals("Entregado", donacion.getHistorialEstados().getLast().getJustificacion());
        // TPA2: queda registrado que camion realizo la entrega
        assertEquals("AB123CD", donacion.patenteCamionDeLaEntrega());
    }
}
