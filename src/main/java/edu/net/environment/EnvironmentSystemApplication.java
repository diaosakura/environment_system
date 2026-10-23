package edu.net.environment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("edu.net.environment.mapper")
public class EnvironmentSystemApplication {
    public static void main(String[] args) {
        SpringApplication.run(EnvironmentSystemApplication.class, args);
    }
}
