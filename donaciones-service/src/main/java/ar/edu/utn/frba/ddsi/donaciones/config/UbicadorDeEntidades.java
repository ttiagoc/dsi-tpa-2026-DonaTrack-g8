package ar.edu.utn.frba.ddsi.donaciones.config;

import java.util.logging.Logger;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.donaciones.services.EntidadBeneficiariaService;
import io.github.flbulgarelli.jpa.extras.simple.WithSimplePersistenceUnit;

/**
 * Al arrancar, busca en Georef las entidades que no tienen ubicacion: las cargadas antes de que se
 * geocodificaran las direcciones y las que se registraron mientras Georef no respondia.
 * Corre en otro hilo para no demorar el arranque si no hay red.
 */
@Component
public class UbicadorDeEntidades implements ApplicationRunner {

    private static final Logger LOG = Logger.getLogger(UbicadorDeEntidades.class.getName());

    private final EntidadBeneficiariaService entidades;

    public UbicadorDeEntidades(EntidadBeneficiariaService entidades) {
        this.entidades = entidades;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread.ofVirtual().name("ubicador-de-entidades").start(() -> {
            try {
                int ubicadas = entidades.ubicarPendientes();
                if (ubicadas > 0) {
                    LOG.info("Se ubicaron en el mapa " + ubicadas + " entidades beneficiarias");
                }
            } catch (RuntimeException e) {
                LOG.warning("No se pudieron ubicar las entidades pendientes: " + e.getMessage());
            } finally {
                WithSimplePersistenceUnit.dispose(); // el EntityManager es por hilo
            }
        });
    }
}
