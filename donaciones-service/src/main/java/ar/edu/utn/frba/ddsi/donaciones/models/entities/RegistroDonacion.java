package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor
public class RegistroDonacion {
  private String descripcion;
  private LocalDateTime fecha;
  private List<Bien> bienes;
  private List<Donacion> donacionesSegmentadas = new ArrayList<>();

  public void segmentarDonacion() {
    if (this.bienes == null || this.bienes.isEmpty()) {
      return;
    }

    Map<String, Donacion> maps = new HashMap<>();

    for (Bien bien : this.bienes) {
      // Le pedimos al Bien que nos dé su key
      String key = bien.generarKey();

      if (!maps.containsKey(key)) {
        // Creamos la Donacion pasándole la responsabilidad de inicializarse sola
        maps.put(key, new Donacion(bien, this.fecha));
      } else {
        // Le decimos a la Donacion que agregue el bien
        maps.get(key).agregarBien(bien);
      }
    }

    if (this.donacionesSegmentadas == null) {
      this.donacionesSegmentadas = new ArrayList<>();
    }
    this.donacionesSegmentadas.addAll(maps.values());
  }
}