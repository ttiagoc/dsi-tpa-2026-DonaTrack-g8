package ar.edu.utn.frba.ddsi.donaciones.services;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@Service
public class DonanteService {

    private final DonanteRepository donanteRepository;
    private final EventoService eventoService;

    public DonanteService(DonanteRepository donanteRepository, EventoService eventoService) {
        this.donanteRepository = donanteRepository;
        this.eventoService = eventoService;
    }

    /**
     * Tarea programada nocturna diaria (1:00 AM) que revisa la inactividad de los donantes.
     * Si no registran interacciones (donaciones) por más de 20 días, se emite una notificación.
     */
    @Scheduled(cron = "0 0 1 * * *")
    public void verificarInactividadDonantes() {
        System.out.println("SCHEDULER (DonanteService): Iniciando verificación de inactividad de donantes...");
        List<Donante> donantes = donanteRepository.findAll();
        LocalDate hoy = LocalDate.now();

        for (Donante donante : donantes) {
            try {
                LocalDate fechaUltima = donante.getFechaUltimaDonacion();
                if (fechaUltima != null) {
                    long diasInactivo = ChronoUnit.DAYS.between(fechaUltima, hoy);
                    if (diasInactivo > 20) {
                        System.out.println("SCHEDULER (DonanteService): Donante ID #" + donante.getId() + " lleva " + diasInactivo + " días inactivo. Emitiendo notificación de ausencia.");
                        eventoService.notificarAusenciaDonante(donante);
                    }
                }
            } catch (RuntimeException e) {
                // Captura el caso donde donante.getFechaUltimaDonacion() lanza excepción si no registra donaciones previas
                System.out.println("SCHEDULER (DonanteService): El Donante ID #" + donante.getId() + " no registra donaciones anteriores. Emitiendo notificación de incentivo.");
                eventoService.notificarAusenciaDonante(donante);
            }
        }
    }
}
