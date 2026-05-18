package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import java.util.List;

public interface TipoNecesidad {
  Boolean estaSatisfecha(List<Donacion> donaciones);
}