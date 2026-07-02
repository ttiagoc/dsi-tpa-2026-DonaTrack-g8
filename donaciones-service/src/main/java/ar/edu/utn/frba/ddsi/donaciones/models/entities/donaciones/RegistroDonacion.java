package ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones;

import java.time.LocalDateTime;
import java.util.List;

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
}