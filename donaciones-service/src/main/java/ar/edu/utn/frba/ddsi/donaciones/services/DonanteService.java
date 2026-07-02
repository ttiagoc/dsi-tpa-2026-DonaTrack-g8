package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.donaciones.dto.donante.ActualizarPersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.ActualizarPersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.CrearPersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;

public interface DonanteService {

    List<DonanteResponse> obtenerTodos();

    DonanteResponse obtenerPorId(Long id);

    DonanteResponse crearPersonaHumana(PersonaHumanaRequest request);

    DonanteResponse crearPersonaJuridica(CrearPersonaJuridicaRequest request);

    DonanteResponse actualizarPersonaHumana(Long id, ActualizarPersonaHumanaRequest request);

    DonanteResponse actualizarPersonaJuridica(Long id,
            ActualizarPersonaJuridicaRequest request);

    boolean eliminar(Long id);
}
