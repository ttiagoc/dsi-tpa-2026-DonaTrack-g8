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

    public DonacionDTO(Long id, Double peso, Double volumen, String direccion) {
        this(id, peso, volumen, direccion, null, null);
    }
}
