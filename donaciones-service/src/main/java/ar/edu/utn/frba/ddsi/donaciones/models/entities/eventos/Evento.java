package ar.edu.utn.frba.ddsi.donaciones.models.entities.eventos;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;

public interface Evento {
    MedioContacto getContacto();

    String getMensaje();
}