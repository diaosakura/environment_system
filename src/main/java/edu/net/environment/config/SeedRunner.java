package edu.net.environment.config;

import edu.net.environment.service.DemoSeeder;
import org.springframework.boot.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name="app.seed",havingValue="true")
public class SeedRunner implements CommandLineRunner {
    private final DemoSeeder seeder;
    private final ConfigurableApplicationContext context;
    public SeedRunner(DemoSeeder seeder,ConfigurableApplicationContext context) { this.seeder=seeder; this.context=context; }
    @Override public void run(String... args) {
        seeder.seed();
        System.out.println("Demo initialized: 1 admin, 2 supervisors, 4 grid members, 40 feedbacks, 20 measurements.");
        SpringApplication.exit(context,()->0);
    }
}
