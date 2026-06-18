package ar.edu.utn.frba.ddsi.donaciones.controllers;

import ar.edu.utn.frba.ddsi.donaciones.dto.PersonaHumanaDTO;
import ar.edu.utn.frba.ddsi.donaciones.dto.PersonaJuridicaDTO;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.PersonaHumana;
import ar.edu.utn.frba.ddsi.donaciones.models.entities.PersonaJuridica;
import ar.edu.utn.frba.ddsi.donaciones.models.repositories.DonanteRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/donantes")
public class DonanteController {

    private final DonanteRepository donanteRepository;

    public DonanteController(DonanteRepository donanteRepository) {
        this.donanteRepository = donanteRepository;
    }

    @PostMapping("/humanas")
    public ResponseEntity<PersonaHumanaDTO> crearHumana(@RequestBody PersonaHumanaDTO dto) {
        PersonaHumana humana = new PersonaHumana();
        humana.setNombre(dto.getNombre());
        humana.setApellido(dto.getApellido());
        humana.setDni(dto.getDni());
        // Simulación: Faltaría el mapeo de emails de contacto
        
        donanteRepository.guardarHumana(humana);
        dto.setId(humana.getId());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping("/humanas")
    public ResponseEntity<List<PersonaHumanaDTO>> obtenerHumanas() {
        List<PersonaHumanaDTO> dtos = donanteRepository.obtenerTodasLasHumanas().stream().map(h -> {
            PersonaHumanaDTO dto = new PersonaHumanaDTO();
            dto.setId(h.getId());
            dto.setNombre(h.getNombre());
            dto.setApellido(h.getApellido());
            dto.setDni(h.getDni());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/juridicas")
    public ResponseEntity<PersonaJuridicaDTO> crearJuridica(@RequestBody PersonaJuridicaDTO dto) {
        PersonaJuridica juridica = new PersonaJuridica();
        juridica.setRazonSocial(dto.getRazonSocial());
        juridica.setCuit(dto.getCuit());
        juridica.setRubro(dto.getRubro());
        
        donanteRepository.guardarJuridica(juridica);
        dto.setId(juridica.getId());
        return new ResponseEntity<>(dto, HttpStatus.CREATED);
    }

    @GetMapping("/juridicas")
    public ResponseEntity<List<PersonaJuridicaDTO>> obtenerJuridicas() {
        List<PersonaJuridicaDTO> dtos = donanteRepository.obtenerTodasLasJuridicas().stream().map(j -> {
            PersonaJuridicaDTO dto = new PersonaJuridicaDTO();
            dto.setId(j.getId());
            dto.setRazonSocial(j.getRazonSocial());
            dto.setCuit(j.getCuit());
            dto.setRubro(j.getRubro());
            return dto;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }
}
