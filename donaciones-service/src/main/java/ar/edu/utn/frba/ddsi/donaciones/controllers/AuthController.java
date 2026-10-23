package ar.edu.utn.frba.ddsi.donaciones.controllers;

import static ar.edu.utn.frba.ddsi.common.security.Rol.ADMINISTRADOR;
import static ar.edu.utn.frba.ddsi.common.security.Rol.DONANTE;
import static ar.edu.utn.frba.ddsi.common.security.Rol.ENTIDAD_BENEFICIARIA;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.common.security.Autorizacion;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.LoginRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RefreshRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroEntidadBeneficiariaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroPersonaHumanaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.auth.RegistroPersonaJuridicaRequest;
import ar.edu.utn.frba.ddsi.donaciones.services.AuthService;
import io.javalin.Javalin;

@Component
public class AuthController implements JavalinController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.post("/api/auth/login", ctx -> {
            ctx.json(authService.login(ctx.bodyAsClass(LoginRequest.class)));
        });

        app.post("/api/auth/refresh", ctx -> {
            ctx.json(authService.refresh(ctx.bodyAsClass(RefreshRequest.class)));
        });

        app.post("/api/auth/registro/persona-humana", ctx -> {
            RegistroPersonaHumanaRequest request = ctx.bodyAsClass(RegistroPersonaHumanaRequest.class);
            ctx.status(201).json(authService.registrarPersonaHumana(request));
        });

        app.post("/api/auth/registro/persona-juridica", ctx -> {
            RegistroPersonaJuridicaRequest request = ctx.bodyAsClass(RegistroPersonaJuridicaRequest.class);
            ctx.status(201).json(authService.registrarPersonaJuridica(request));
        });

        app.post("/api/auth/registro/entidad-beneficiaria", ctx -> {
            RegistroEntidadBeneficiariaRequest request = ctx.bodyAsClass(RegistroEntidadBeneficiariaRequest.class);
            ctx.status(201).json(authService.registrarEntidadBeneficiaria(request));
        });

        app.get("/api/auth/me", ctx -> {
            ctx.json(authService.obtenerUsuario(Autorizacion.usuario(ctx).usuarioId()));
        }, DONANTE, ENTIDAD_BENEFICIARIA, ADMINISTRADOR);
    }
}
