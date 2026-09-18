package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Table;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "parada")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Parada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orden")
    private Integer orden;

    @Column(name = "destino")
    private String destino;

    @Column(name = "entidad_id")
    private Long entidadId;

    @ElementCollection
    @CollectionTable(name = "parada_donaciones", joinColumns = @JoinColumn(name = "parada_id"))
    @Column(name = "donacion_id")
    private List<Long> donacionIds = new ArrayList<>();
}
