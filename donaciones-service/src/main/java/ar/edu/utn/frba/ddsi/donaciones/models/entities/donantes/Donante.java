package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.RegistroDonacion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "donante")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_donante", length = 20)
@Getter
@Setter
@NoArgsConstructor
public abstract class Donante {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToMany(mappedBy = "donante")
  private List<RegistroDonacion> donaciones = new ArrayList<>();

  @ElementCollection
  @CollectionTable(name = "donante_contactos", joinColumns = @JoinColumn(name = "donante_id"))
  private List<MedioContacto> contactos = new ArrayList<>();

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "valor", column = @Column(name = "contacto_predeterminado_valor")),
      @AttributeOverride(name = "tipoContacto",
          column = @Column(name = "contacto_predeterminado_tipo_contacto", length = 20))
  })
  private MedioContacto contactoPredeterminado;

  public Donante(Long id, List<MedioContacto> contactos, MedioContacto contactoPredeterminado) {
    this.id = id;
    this.contactos = contactos;
    this.contactoPredeterminado = contactoPredeterminado;
  }

  public void agregarDonacion(RegistroDonacion donacion) {
    this.donaciones.add(donacion);
  }

  public LocalDate getFechaUltimaDonacion() {
    LocalDateTime maxFecha = this.donaciones.stream()
        .map(RegistroDonacion::getFecha)
        .max(LocalDateTime::compareTo)
        .orElse(null);

    if (maxFecha == null) {
      throw new RuntimeException("El donante no tiene donaciones registradas.");
    }
    return maxFecha.toLocalDate();
  }
}
