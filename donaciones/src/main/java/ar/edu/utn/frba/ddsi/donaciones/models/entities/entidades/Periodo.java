package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.time.LocalDateTime;
import java.time.temporal.IsoFields;

public enum Periodo {
  DIARIO, SEMANAL, MENSUAL, ANUAL;

  public boolean incluye(LocalDateTime fechaReferencia) {
    if (fechaReferencia == null)
      return false;

    LocalDateTime ahora = LocalDateTime.now();

    return switch (this) {
      case DIARIO -> fechaReferencia.toLocalDate().isEqual(ahora.toLocalDate());

      case SEMANAL -> {
        int semanaRef = fechaReferencia.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int semanaAhora = ahora.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        int anioRef = fechaReferencia.get(IsoFields.WEEK_BASED_YEAR);
        int anioAhora = ahora.get(IsoFields.WEEK_BASED_YEAR);
        yield (semanaRef == semanaAhora) && (anioRef == anioAhora);
      }

      case MENSUAL -> (fechaReferencia.getMonth() == ahora.getMonth()) &&
          (fechaReferencia.getYear() == ahora.getYear());

      case ANUAL -> fechaReferencia.getYear() == ahora.getYear();
    };
  }
}