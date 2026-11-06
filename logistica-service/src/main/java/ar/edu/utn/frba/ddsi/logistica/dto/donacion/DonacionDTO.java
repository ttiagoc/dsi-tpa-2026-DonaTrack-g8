package ar.edu.utn.frba.ddsi.logistica.dto.donacion;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DonacionDTO {
    private Long id;
    private Double peso;
    private Double volumen;
    private String direccion;
    private Long entidadId;
    private String entidadNombre;
    // ubicacion de la sede de la entidad (Georef). Nulas si donaciones todavia no la pudo geocodificar
    private Double latitud;
    private Double longitud;

    public DonacionDTO(Long id, Double peso, Double volumen, String direccion) {
        this(id, peso, volumen, direccion, null, null);
    }

    public DonacionDTO(Long id, Double peso, Double volumen, String direccion, Long entidadId, String entidadNombre) {
        this(id, peso, volumen, direccion, entidadId, entidadNombre, null, null);
    }
}
