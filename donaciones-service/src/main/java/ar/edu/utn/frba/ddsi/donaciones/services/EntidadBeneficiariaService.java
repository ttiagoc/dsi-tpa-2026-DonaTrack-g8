package ar.edu.utn.frba.ddsi.donaciones.services;

import java.util.List;

import ar.edu.utn.frba.ddsi.common.models.entities.MedioContacto;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.EntidadBeneficiariaResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.NecesidadResponse;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.ReportarNoRecibidaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.entidadbeneficiaria.SubirFotosRecepcionRequest;

public interface EntidadBeneficiariaService {

    List<EntidadBeneficiariaResponse> obtenerTodas();

    EntidadBeneficiariaResponse obtenerPorId(Long id);

    EntidadBeneficiariaResponse crear(EntidadBeneficiariaRequest request);

    boolean eliminar(Long id);

    EntidadBeneficiariaResponse actualizar(Long id, EntidadBeneficiariaRequest request);

    List<NecesidadResponse> obtenerNecesidades(Long id);

    NecesidadResponse registrarNecesidad(Long id, NecesidadRequest request);

    void eliminarNecesidad(Long entidadId, Long necesidadId);

    void confirmarEntrega(Long entidadId, Long donacionId);

    void reportarNoRecibida(Long entidadId, Long donacionId, ReportarNoRecibidaRequest request);

    void subirFotosRecepcion(Long entidadId, Long donacionId, SubirFotosRecepcionRequest request);

    List<MedioContacto> obtenerContactos(Long id);
}
