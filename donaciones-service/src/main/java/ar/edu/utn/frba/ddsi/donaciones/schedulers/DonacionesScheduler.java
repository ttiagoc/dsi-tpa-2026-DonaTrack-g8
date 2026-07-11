package ar.edu.utn.frba.ddsi.donaciones.schedulers;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.MotorDeMatchmaking;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.GestorDeEventos;

@Component
public class DonacionesScheduler {

    private final GestorDeEventos gestorDeEventos;
    private final MotorDeMatchmaking motorDeMatchmaking;

    public DonacionesScheduler(GestorDeEventos gestorDeEventos, MotorDeMatchmaking motorDeMatchmaking) {
        this.gestorDeEventos = gestorDeEventos;
        this.motorDeMatchmaking = motorDeMatchmaking;
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void verificarInactividadDonantes() {
        gestorDeEventos.verificarInactividadDonantes();
    }

    @Scheduled(cron = "0 30 2 * * *")
    public void ejecutarProcesoNocturno() {
        motorDeMatchmaking.procesarMatchmaking();
    }
}
