package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

public record DonacionAsignadaResponse(
                Long id,
                Double peso,
                Double volumen,
                String direccion,
                Long entidadId,
                String entidadNombre) {

        public DonacionAsignadaResponse(Long id, Double peso, Double volumen, String direccion) {
                this(id, peso, volumen, direccion, null, null);
        }
}
