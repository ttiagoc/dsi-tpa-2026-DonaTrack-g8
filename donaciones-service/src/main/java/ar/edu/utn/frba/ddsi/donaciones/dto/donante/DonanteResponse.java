package ar.edu.utn.frba.ddsi.donaciones.dto.donante;

public record DonanteResponse(
        Long id,
        String tipo,
        String nombre,
        String documento,
        String contactoPredeterminado) {

    public DonanteResponse(Long id, String tipo, String contactoPredeterminado) {
        this(id, tipo, null, null, contactoPredeterminado);
    }
}
