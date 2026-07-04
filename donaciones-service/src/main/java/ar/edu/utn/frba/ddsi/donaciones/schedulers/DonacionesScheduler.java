package ar.edu.utn.frba.ddsi.donaciones.schedulers;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.donaciones.services.EventoService;
import ar.edu.utn.frba.ddsi.donaciones.services.MatchmakingService;

@Component
public class DonacionesScheduler {

    private final EventoService eventoService;
    private final MatchmakingService matchmakingService;

    public DonacionesScheduler(EventoService eventoService, MatchmakingService matchmakingService) {
        this.eventoService = eventoService;
        this.matchmakingService = matchmakingService;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void verificarInactividadDonantes() {
        eventoService.verificarInactividadDonantes();
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void ejecutarProcesoNocturno() {
        matchmakingService.ejecutarProcesoNocturno();
    }
}
