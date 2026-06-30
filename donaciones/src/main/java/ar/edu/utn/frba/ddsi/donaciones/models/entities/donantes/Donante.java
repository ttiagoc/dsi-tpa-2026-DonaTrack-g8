package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public abstract class Donante {
  private Long id;
  private List<RegistroDonacion> donaciones = new ArrayList<>();
  private List<MedioContacto> contactos = new ArrayList<>();
  private MedioContacto contactoPredeterminado;

  public void agregarDonacion(RegistroDonacion donacion) {
    this.donaciones.add(donacion);
  }

  public LocalDate getFechaUltimaDonacion() {
    LocalDate fechaUltimaDonacion = this.donaciones.stream()
        .map(RegistroDonacion::getFecha)
        .max(LocalDateTime::compareTo)
        .orElse(null)
        .toLocalDate();

    if (fechaUltimaDonacion == null) {
      throw new RuntimeException("El donante no tiene donaciones registradas.");
    }
    return fechaUltimaDonacion;
  }
}
