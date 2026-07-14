package ar.edu.utn.frba.ddsi.common.controllers;

import io.javalin.Javalin;

public interface JavalinController {
    void registerRoutes(Javalin app);
}
