package ar.edu.utn.frba.ddsi.donaciones.services;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes.Donante;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;

@Service
public class ControlAusenciaDonantesService {

    private final DonanteRepository donanteRepository;
    private final EventoService eventoService;

    public ControlAusenciaDonantesService(DonanteRepository donanteRepository,
            EventoService eventoService) {
        this.donanteRepository = donanteRepository;
        this.eventoService = eventoService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void verificarInactividadDonantes() {
        System.out.println("Iniciando escaneo diario de inactividad de donantes...");

        List<Donante> donantes = donanteRepository.findAll();
        LocalDate limiteInactividad = LocalDate.now().minusDays(20);

        for (Donante donante : donantes) {
            if (donante.getFechaUltimaDonacion().isBefore(limiteInactividad)) {

                System.out.println("Se detectó inactividad prolongada en Donante ID #" + donante.getId());
                eventoService.notificarAusenciaDonante(donante);
            }
        }
        System.out.println("Escaneo de inactividad finalizado.");
    }
}