package com.richard.paylite;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(
                title = "PayLite API",
                version = "1.0",
                description = "A lightweight RESTful API for processing payments.",
                contact = @Contact(
                        name = "Richard",
                        email = "richard@example.com"
                ),
                license = @License(
                        name = "Apache 2.0",
                        url = "http://www.apache.org/licenses/LICENSE-2.0.html"
                )
        )
)
public class PayLiteApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayLiteApplication.class, args);
    }

}
