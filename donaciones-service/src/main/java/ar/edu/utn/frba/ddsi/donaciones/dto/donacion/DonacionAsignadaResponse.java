package ar.edu.utn.frba.ddsi.donaciones.dto.donacion;

public record DonacionAsignadaResponse(
                Long id,
                Double peso,
                Double volumen,
                String direccion,
                Long entidadId,
                String entidadNombre,
                Double latitud,
                Double longitud) {

        public DonacionAsignadaResponse(Long id, Double peso, Double volumen, String direccion) {
                this(id, peso, volumen, direccion, null, null);
        }

        public DonacionAsignadaResponse(Long id, Double peso, Double volumen, String direccion,
                        Long entidadId, String entidadNombre) {
                this(id, peso, volumen, direccion, entidadId, entidadNombre, null, null);
        }
}
