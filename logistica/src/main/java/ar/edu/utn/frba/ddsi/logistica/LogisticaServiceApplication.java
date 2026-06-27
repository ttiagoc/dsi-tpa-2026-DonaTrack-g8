package ar.edu.utn.frba.ddsi.logistica;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import ar.edu.utn.frba.ddsi.logistica.config.RestLogisticaConfig;

@SpringBootApplication
@EnableConfigurationProperties(RestLogisticaConfig.class)
public class LogisticaServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogisticaServiceApplication.class, args);
    }
}
