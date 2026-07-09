package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.DonanteResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donante.PersonaJuridicaRequest;

public interface DonanteService {

    List<DonanteResponse> obtenerTodos();

    DonanteResponse obtenerPorId(Long id);

    DonanteResponse crearPersonaHumana(PersonaHumanaRequest request);

    DonanteResponse crearPersonaJuridica(PersonaJuridicaRequest request);

    DonanteResponse actualizarPersonaHumana(Long id, PersonaHumanaRequest request);

    DonanteResponse actualizarPersonaJuridica(Long id, PersonaJuridicaRequest request);

    boolean eliminar(Long id);

    MedioContacto obtenerContactoPredeterminado(Long id);
}
