package ar.edu.utn.frba.ddsi.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

import ar.edu.utn.frba.ddsi.web.config.WebConfig;

@SpringBootApplication
@EnableConfigurationProperties(WebConfig.class)
public class WebServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebServiceApplication.class, args);
    }
}
