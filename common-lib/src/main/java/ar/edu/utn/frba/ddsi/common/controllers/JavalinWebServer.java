package ar.edu.utn.frba.ddsi.common.controllers;

import java.time.Instant;
import java.util.List;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import ar.edu.utn.frba.ddsi.common.dtos.error.ErrorResponse;
import ar.edu.utn.frba.ddsi.common.exceptions.BusinessException;
import ar.edu.utn.frba.ddsi.common.exceptions.ResourceNotFoundException;

import io.javalin.Javalin;
import io.javalin.json.JavalinJackson;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.databind.SerializationFeature;

@Component
@Profile("!test")
public class JavalinWebServer {

    private final List<JavalinController> controllers;
    private Javalin app;

    @Value("${server.port:8080}")
    private int port;

    public JavalinWebServer(List<JavalinController> controllers) {
        this.controllers = controllers;
    }

    @PostConstruct
    public void start() {
        this.app = Javalin.create(config -> {
            config.showJavalinBanner = false;
            config.jsonMapper(new JavalinJackson().updateMapper(mapper -> {
                mapper.registerModule(new JavaTimeModule());
                mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            }));
        });

        app.exception(ResourceNotFoundException.class, (e, ctx) -> {
            ctx.status(404).json(new ErrorResponse("not_found", e.getMessage(), Instant.now()));
        });

        app.exception(BusinessException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("bad_request", e.getMessage(), Instant.now()));
        });

        app.exception(IllegalArgumentException.class, (e, ctx) -> {
            ctx.status(400).json(new ErrorResponse("bad_request", e.getMessage(), Instant.now()));
        });

        app.exception(Exception.class, (e, ctx) -> {
            e.printStackTrace();
            ctx.status(500).json(new ErrorResponse("internal_error", "Ocurrió un error interno", Instant.now()));
        });

        controllers.forEach(controller -> controller.registerRoutes(app));

        this.app.start(port);
        System.out.println("Javalin web server started on port " + port);
    }

    @PreDestroy
    public void stop() {
        if (this.app != null) {
            this.app.stop();
        }
    }
}
