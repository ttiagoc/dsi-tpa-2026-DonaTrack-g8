package ar.edu.utn.frba.ddsi.donaciones.models.entities;

import ar.edu.utn.frba.ddsi.common.MedioContacto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.time.LocalDate;
import java.util.List;

@Getter @Setter @NoArgsConstructor
public class PersonaHumana {
  private Donante donante;
  private String nombre;
  private String apellido;
  private LocalDate fechaNacimiento;
  private String tipoDocumento;
  private String nroDocumento;
  private String genero;
  private String direccion;
  private List<MedioContacto> contactos;
  private MedioContacto contactoPredeterminado;
}
