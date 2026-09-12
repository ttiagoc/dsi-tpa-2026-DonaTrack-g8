package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.time.LocalDate;
import java.util.List;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("PERSONA_HUMANA")
@Getter
@Setter
@NoArgsConstructor
public class PersonaHumana extends Donante {
  private String nombre;
  private String apellido;

  @Column(name = "fecha_nacimiento")
  private LocalDate fechaNacimiento;

  @Column(length = 20)
  private String dni;

  @Column(length = 50)
  private String genero;

  private String direccion;

  public PersonaHumana(Long id, List<MedioContacto> contactos,
      MedioContacto contactoPredeterminado, String nombre, String apellido, LocalDate fechaNacimiento, String dni,
      String genero, String direccion) {
    super(id, contactos, contactoPredeterminado);
    this.nombre = nombre;
    this.apellido = apellido;
    this.fechaNacimiento = fechaNacimiento;
    this.dni = dni;
    this.genero = genero;
    this.direccion = direccion;
  }
}
