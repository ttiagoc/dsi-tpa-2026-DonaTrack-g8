package ar.edu.utn.frba.ddsi.logistica.services;

import java.util.List;

import org.springframework.stereotype.Service;

import ar.edu.utn.frba.ddsi.logistica.models.entities.Chofer;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ActualizarCamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ActualizarCamionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ChoferInfo;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CrearCamionRequest;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.CrearCamionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ObtenerCamionResponse;
import ar.edu.utn.frba.ddsi.logistica.dto.camion.ObtenerTodosCamionesResponse;
import ar.edu.utn.frba.ddsi.logistica.models.entities.Camion;
import ar.edu.utn.frba.ddsi.logistica.models.repositories.CamionRepository;

@Service
public class CamionService {

    private final CamionRepository camionRepository;

    public CamionService(CamionRepository camionRepository) {
        this.camionRepository = camionRepository;
    }

    public ObtenerTodosCamionesResponse obtenerTodos() {
        List<ObtenerCamionResponse> camiones = camionRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
        return new ObtenerTodosCamionesResponse(camiones);
    }

    public ObtenerCamionResponse obtenerPorId(Long id) {
        Camion camion = camionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el camion con id: " + id));
        return mapToResponse(camion);
    }

    public CrearCamionResponse crear(CrearCamionRequest request) {
        Camion camion = new Camion();
        camion.setPatente(request.patente());
        camion.setCapacidadVolumen(request.capacidadVolumen());
        camion.setAltura(request.altura());
        camion.setCapacidadCarga(request.capacidadCarga());
        camion.setChofer(mapToChofer(request.chofer()));

        Camion guardado = camionRepository.save(camion);
        return new CrearCamionResponse(guardado.getId(), "Camión creado con éxito");
    }

    public ActualizarCamionResponse actualizar(Long id, ActualizarCamionRequest request) {
        Camion existente = camionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro el camion con id: " + id));

        existente.setPatente(request.patente());
        existente.setCapacidadVolumen(request.capacidadVolumen());
        existente.setAltura(request.altura());
        existente.setCapacidadCarga(request.capacidadCarga());
        existente.setChofer(mapToChofer(request.chofer()));

        camionRepository.save(existente);
        return new ActualizarCamionResponse("Camión actualizado con éxito");
    }

    public boolean eliminar(Long id) {
        return camionRepository.deleteById(id);
    }

    private ObtenerCamionResponse mapToResponse(Camion camion) {
        ChoferInfo choferInfo = camion.getChofer() != null
                ? new ChoferInfo(camion.getChofer().getNombre(), camion.getChofer().getApellido())
                : null;
        return new ObtenerCamionResponse(
                camion.getId(),
                camion.getPatente(),
                camion.getCapacidadVolumen(),
                camion.getAltura(),
                camion.getCapacidadCarga(),
                choferInfo);
    }

    private Chofer mapToChofer(ChoferInfo info) {
        if (info == null)
            return null;
        Chofer chofer = new Chofer();
        chofer.setNombre(info.nombre());
        chofer.setApellido(info.apellido());
        return chofer;
    }
}
