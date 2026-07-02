package ar.edu.utn.frba.ddsi.donaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "ar.edu.utn.frba.ddsi")
@EnableScheduling
public class DonacionesServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(DonacionesServiceApplication.class, args);
    }
}
