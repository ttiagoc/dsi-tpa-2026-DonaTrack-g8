package ar.edu.utn.frba.ddsi.logistica.models.entities.logistica;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Camion {
    private Long id;
    private String patente;
    private Double capacidadVolumen;
    private Double altura;
    private Double capacidadCarga;
    private Chofer chofer;
    private Ubicacion ubicacion;

    public Camion(String patente, Double capacidadVolumen, Double altura, Double capacidadCarga, Chofer chofer) {
        this.id = null;
        this.patente = patente;
        this.capacidadVolumen = capacidadVolumen;
        this.altura = altura;
        this.capacidadCarga = capacidadCarga;
        this.chofer = chofer;
    }

    public void actualizarUbicacion(Ubicacion nuevaUbicacion) {
        this.ubicacion = nuevaUbicacion;
    }

}
