package ar.edu.utn.frba.ddsi.donaciones.controllers;

import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.controllers.JavalinController;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.CategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.dto.donacion.SubcategoriaRequest;
import ar.edu.utn.frba.ddsi.donaciones.services.CategoriaService;
import io.javalin.Javalin;

@Component
public class CategoriaController implements JavalinController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @Override
    public void registerRoutes(Javalin app) {
        app.get("/api/categorias", ctx -> {
            ctx.json(categoriaService.obtenerTodas());
        });

        app.post("/api/categorias", ctx -> {
            CategoriaRequest request = ctx.bodyAsClass(CategoriaRequest.class);
            ctx.status(201).json(categoriaService.crear(request));
        });

        app.get("/api/categorias/{categoriaId}/subcategorias", ctx -> {
            Long categoriaId = Long.parseLong(ctx.pathParam("categoriaId"));
            ctx.json(categoriaService.obtenerSubcategorias(categoriaId));
        });

        app.post("/api/categorias/{categoriaId}/subcategorias", ctx -> {
            Long categoriaId = Long.parseLong(ctx.pathParam("categoriaId"));
            SubcategoriaRequest request = ctx.bodyAsClass(SubcategoriaRequest.class);
            ctx.status(201).json(categoriaService.crearSubcategoria(categoriaId, request));
        });
    }
}
