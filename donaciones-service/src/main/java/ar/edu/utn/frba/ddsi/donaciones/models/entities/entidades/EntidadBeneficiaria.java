package ar.edu.utn.frba.ddsi.donaciones.models.entities.entidades;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.common.models.enums.TipoContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.donaciones.Donacion;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "entidad_beneficiaria")
@Getter
@Setter
@NoArgsConstructor
public class EntidadBeneficiaria {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "razon_social")
  private String razonSocial;

  private String direccion;

  @Embedded
  @AttributeOverrides({
      @AttributeOverride(name = "valor", column = @Column(name = "telefono_valor")),
      @AttributeOverride(name = "tipoContacto", column = @Column(name = "telefono_tipo_contacto", length = 20))
  })
  private MedioContacto telefono;

  @ElementCollection
  @CollectionTable(name = "entidad_beneficiaria_correo_representantes",
      joinColumns = @JoinColumn(name = "entidad_beneficiaria_id"))
  private List<MedioContacto> correoRepresentantes;

  // Las necesidades se gestionan siempre a traves de su entidad: al quitarlas de la lista se borran
  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "entidad_beneficiaria_id")
  private List<Necesidad> necesidades;

  public EntidadBeneficiaria(String razonSocial, String direccion, String valorTelefono,
      List<MedioContacto> correoRepresentantes) {
    this.razonSocial = razonSocial;
    this.direccion = direccion;
    this.telefono = new MedioContacto(valorTelefono, TipoContacto.SMS);
    this.correoRepresentantes = correoRepresentantes;
    this.necesidades = new ArrayList<>();
  }

  public void registrarNecesidad(Necesidad necesidad) {
    this.necesidades.add(necesidad);
  }

  public void confirmarEntrega(Donacion donacion, String patenteCamion, LocalDateTime fechaEntrega) {
    donacion.confirmarEntrega(patenteCamion, fechaEntrega);
  }

  // las donaciones asignadas se conservan: solo se desvinculan de la necesidad eliminada
  public void eliminarNecesidad(Long necesidadId) {
    this.necesidades.stream()
        .filter(n -> n.getId().equals(necesidadId))
        .forEach(Necesidad::liberarDonaciones);
    this.necesidades.removeIf(n -> n.getId().equals(necesidadId));
  }
}
