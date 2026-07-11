package ar.edu.utn.frba.ddsi.logistica.schedulers;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.logistica.models.entities.logistica.PlanificadorDeRutas;

@Component
public class LogisticaScheduler {

    private final PlanificadorDeRutas planificadorDeRutas;

    public LogisticaScheduler(PlanificadorDeRutas planificadorDeRutas) {
        this.planificadorDeRutas = planificadorDeRutas;
    }

    @Scheduled(cron = "0 0 3 * * *")
    public void planificarRutas() {
        planificadorDeRutas.planificarRutas();
    }
}
