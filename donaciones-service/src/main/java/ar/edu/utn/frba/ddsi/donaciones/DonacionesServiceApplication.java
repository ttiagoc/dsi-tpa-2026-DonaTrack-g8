package ar.edu.utn.frba.ddsi.donaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import ar.edu.utn.frba.ddsi.donaciones.config.RestDonacionesConfig;

@SpringBootApplication(scanBasePackages = "ar.edu.utn.frba.ddsi")
@EnableConfigurationProperties(RestDonacionesConfig.class)
public class DonacionesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DonacionesServiceApplication.class, args);
    }
}

