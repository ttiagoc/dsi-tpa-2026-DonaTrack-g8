package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.utn.frba.ddsi.donaciones.dto.evento.ConfirmacionEntregaExitosaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.evento.InicioRutaRequest;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos.GestorDeEventos;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/api/donaciones-service/evento")
@AllArgsConstructor
public class EventoController {

    private final GestorDeEventos gestorDeEventos;

    @PostMapping("/inicio-ruta")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void iniciarRuta(@RequestBody InicioRutaRequest request) {
        gestorDeEventos.iniciarRuta(request);
    }

    @PostMapping("/confirmacion-entrega-exitosa")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void confirmarEntregaExitosa(@RequestBody ConfirmacionEntregaExitosaRequest request) {
        gestorDeEventos.confirmarEntregaExitosa(request);
    }
}
