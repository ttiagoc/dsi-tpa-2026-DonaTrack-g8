package ar.edu.utn.frba.ddsi.donaciones.models.entities.donantes;

import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.JoinColumn;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.models.enums.TipoOrganizacion;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("PERSONA_JURIDICA")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonaJuridica extends Donante {
  @Column(name = "razon_social")
  private String razonSocial;

  private String rubro;

  @Enumerated(EnumType.STRING)
  @Column(name = "tipo_organizacion", length = 50)
  private TipoOrganizacion tipo;

  @Column(length = 20)
  private String cuit;

  @ElementCollection
  @CollectionTable(name = "donante_representantes", joinColumns = @JoinColumn(name = "donante_id"))
  private List<Representante> representantes;

  public PersonaJuridica(List<MedioContacto> contactos, MedioContacto contactoPredeterminado,
      String razonSocial, String rubro, TipoOrganizacion tipo, String cuit, List<Representante> representantes) {
    super(contactos, contactoPredeterminado);
    validarObligatorio(razonSocial, "La razon social de la persona juridica no puede ser nula ni estar vacia");
    validarObligatorio(cuit, "El CUIT de la persona juridica no puede ser nulo ni estar vacio");
    this.razonSocial = razonSocial;
    this.rubro = rubro;
    this.tipo = tipo;
    this.cuit = cuit;
    this.representantes = representantes;
  }
}
