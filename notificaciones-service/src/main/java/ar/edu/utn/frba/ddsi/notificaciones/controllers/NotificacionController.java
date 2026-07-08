package ar.edu.utn.frba.ddsi.notificaciones.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.notificaciones.dto.NotificacionRequest;
import ar.edu.utn.frba.ddsi.notificaciones.models.entities.Notificacion;
import ar.edu.utn.frba.ddsi.notificaciones.services.NotificacionService;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/notificaciones-service/notificar")
@AllArgsConstructor
public class NotificacionController {

    private final NotificacionService notificacionService;

    @PostMapping
    @ResponseStatus(HttpStatus.OK)
    public void notificar(@RequestBody NotificacionRequest notificacionRequest) {
        notificacionService.enviarNotificacion(notificacionRequest);
    }
}

