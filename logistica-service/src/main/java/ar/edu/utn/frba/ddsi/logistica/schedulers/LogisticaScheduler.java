package ar.edu.utn.frba.ddsi.logistica.schedulers;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.logistica.services.PlanificacionRutasService;

@Component
public class LogisticaScheduler {

    private final PlanificacionRutasService planificacionRutasService;

    public LogisticaScheduler(PlanificacionRutasService planificacionRutasService) {
        this.planificacionRutasService = planificacionRutasService;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void planificarRutas() {
        planificacionRutasService.planificarRutas();
    }
}
