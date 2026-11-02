package ar.edu.utn.frba.ddsi.donaciones.models.enums;

import java.util.EnumSet;
import java.util.Set;

public enum TipoEstadoDonacion {
  EN_DEPOSITO,
  ASIGNACION_REALIZADA,
  LISTA_PARA_ENTREGAR,
  EN_TRASLADO,
  ENTREGADA,
  ENTREGA_FALLIDA,
  VENCIDA;

  // asignada a una entidad y todavia sin entregar (la entrega fallida se reprograma)
  private static final Set<TipoEstadoDonacion> EN_CURSO =
      EnumSet.of(ASIGNACION_REALIZADA, LISTA_PARA_ENTREGAR, EN_TRASLADO, ENTREGA_FALLIDA);

  public boolean estaEnCurso() {
    return EN_CURSO.contains(this);
  }
}
