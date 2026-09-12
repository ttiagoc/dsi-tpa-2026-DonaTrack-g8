package ar.edu.utn.frba.ddsi.donaciones.models.repositories.jpa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Categoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Subcategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.EntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.Necesidad;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadExtraordinaria;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades.NecesidadRecurrente;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.Periodo;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaCategoria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaEntidadBeneficiaria;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.impl.JpaSubcategoria;

@DisplayName("Persistencia de EntidadBeneficiaria y Necesidad")
class EntidadBeneficiariaRepositoryJpaTest extends PersistenciaTest {

    private final JpaCategoria categorias = new JpaCategoria();
    private final JpaSubcategoria subcategorias = new JpaSubcategoria();
    private final JpaEntidadBeneficiaria entidades = new JpaEntidadBeneficiaria();

    @Test
    @DisplayName("Reconstruye el tipo de necesidad (Strategy) a partir de las columnas aplanadas")
    void reconstruyeTipoNecesidad() {
        Subcategoria fideos = subcategorias.save(new Subcategoria("Fideos", categorias.save(
                new Categoria("Alimentos", false, true))));

        EntidadBeneficiaria comedor = new EntidadBeneficiaria("Comedor Escobar Sonrisas", "Escobar 123", "1122334455",
                List.of(new MedioContacto("comedor@org.com", TipoContacto.EMAIL)));
        comedor.registrarNecesidad(new Necesidad(fideos, new NecesidadRecurrente(Periodo.SEMANAL), "Fideos", 100L));
        comedor.registrarNecesidad(new Necesidad(fideos, new NecesidadExtraordinaria(), "Fideos extra", 30L));
        Long idComedor = entidades.save(comedor).getId();
        nuevoRequest();

        EntidadBeneficiaria leida = entidades.findById(idComedor).orElseThrow();

        assertEquals("1122334455", leida.getTelefono().getValor());
        assertEquals(1, leida.getCorreoRepresentantes().size());
        assertEquals(2, leida.getNecesidades().size());

        Necesidad recurrente = leida.getNecesidades().stream()
                .filter(n -> n.getDescripcion().equals("Fideos")).findFirst().orElseThrow();
        assertInstanceOf(NecesidadRecurrente.class, recurrente.getTipoNecesidad());
        assertEquals(Periodo.SEMANAL, ((NecesidadRecurrente) recurrente.getTipoNecesidad()).getPeriodo());

        Necesidad extraordinaria = leida.getNecesidades().stream()
                .filter(n -> n.getDescripcion().equals("Fideos extra")).findFirst().orElseThrow();
        assertInstanceOf(NecesidadExtraordinaria.class, extraordinaria.getTipoNecesidad());
    }

    @Test
    @DisplayName("Al quitar una necesidad de su entidad, se borra de la base")
    void eliminaNecesidadHuerfana() {
        Subcategoria sillas = subcategorias.save(new Subcategoria("Sillas", categorias.save(
                new Categoria("Mobiliario", true, false))));
        EntidadBeneficiaria escuela = new EntidadBeneficiaria("Escuela Rural 10", "Ruta 3 km 20", "1100000000",
                List.of());
        escuela.registrarNecesidad(new Necesidad(sillas, new NecesidadExtraordinaria(), "Sillas", 30L));
        Long idEscuela = entidades.save(escuela).getId();
        nuevoRequest();

        EntidadBeneficiaria leida = entidades.findById(idEscuela).orElseThrow();
        leida.eliminarNecesidad(leida.getNecesidades().get(0).getId());
        entidades.save(leida);
        nuevoRequest();

        assertTrue(entidades.findById(idEscuela).orElseThrow().getNecesidades().isEmpty());
        assertTrue(createQuery("from Necesidad", Necesidad.class).getResultList().isEmpty());
    }
}
