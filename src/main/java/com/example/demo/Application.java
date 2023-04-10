package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.aop.Advisor;
import org.springframework.aop.SpringProxy;
import org.springframework.aop.framework.Advised;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.core.DecoratingProxy;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.invoker.HttpClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    record User(String firstName, String lastName) {
    }

    @Bean
    ApplicationListener<ApplicationReadyEvent> ready(UsersClient usersClient) {
        return event -> {
            var user = usersClient.findByUsername("Natasha");
            System.out.println(user);
        };
    }

    static class UsersClientRuntimeHintsRegistrar implements RuntimeHintsRegistrar {

        @Override
        public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
            hints.proxies().registerJdkProxy(
                    UsersClient.class,
                    SpringProxy.class,
                    Advised.class,
                    DecoratingProxy.class
            );
        }
    }

    @Bean
    @RegisterReflectionForBinding(User.class)
    @ImportRuntimeHints(UsersClientRuntimeHintsRegistrar.class)
    UsersClient userClient(WebClient webClient) {
        HttpServiceProxyFactory httpServiceProxyFactory =
                HttpServiceProxyFactory.builder(WebClientAdapter.forClient(webClient))
                        .build();
        return httpServiceProxyFactory.createClient(UsersClient.class);
    }

    @Bean
    WebClient webClient() {
        return WebClient.builder()
                .baseUrl("http://localhost:8080")
                .build();
    }


    interface UsersClient {
        @GetExchange("/users")
        User findByUsername(@RequestParam("firstName") String firstName);
    }
}
