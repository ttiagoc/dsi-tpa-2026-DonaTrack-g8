package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/matchmaking")
public class MatchmakingController {

    @PostMapping("/ejecutar")
    public ResponseEntity<String> ejecutarAlgoritmo() {
        // Mock de la ejecución a demanda del algoritmo de asignación de necesidades
        return ResponseEntity.ok("Algoritmo de asignacion de necesidades ejecutado con exito");
    }
}
