package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistroDonacion {
  private String descripcion;
  private LocalDateTime fecha;
  private List<Bien> bienes;
  private List<Donacion> donacionesSegmentadas;

  public void segmentarDonacion() {
    donacionesSegmentadas = new ArrayList<>();

    if (this.bienes == null || this.bienes.isEmpty()) {
      return;
    }

    List<Donacion> nuevasDonaciones = this.bienes.stream()
        .collect(Collectors.groupingBy(Bien::generarKey))
        .values().stream()
        .map(bienesAgrupados -> {
          Donacion donacion = new Donacion(bienesAgrupados.getFirst(), this.fecha);
          bienesAgrupados.stream().skip(1).forEach(donacion::agregarBien);
          return donacion;
        })
        .toList();

    this.donacionesSegmentadas.addAll(nuevasDonaciones);
  }
}