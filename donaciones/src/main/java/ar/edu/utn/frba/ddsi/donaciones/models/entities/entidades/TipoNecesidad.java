package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;

public interface TipoNecesidad {
  Boolean estaSatisfecha(List<Donacion> donaciones, Long cantidadRequerida);
}