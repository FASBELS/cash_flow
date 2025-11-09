package com.dsi.spring.flujoreal.spring_cashflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class SpringCashflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringCashflowApplication.class, args);
        System.out.println("Aplicación iniciada correctamente con DBConnection Singleton");
    }
}
