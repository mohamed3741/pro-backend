package com.pro.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;


@Configuration
public class JacksonConfiguration {

    @Bean(name = "javaTimeModule")
    public Module javaTimeModule() {
        return new JavaTimeModule();
    }

    @Bean(name = "hibernate5Module")
    public Module hibernate5Module() {
        return new Hibernate5Module();
    }

    @Bean(name = "jdk8TimeModule")
    public Module jdk8TimeModule() {
        return new Jdk8Module();
    }

    @Bean(name = "default")
    @Primary
    public ObjectMapper mapper() {
        return new ObjectMapper()
                .findAndRegisterModules()
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


}


