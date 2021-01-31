package com.megait.myhome.config;

import lombok.Data;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Data
@Component
@Configuration("app")
public class AppProperties {
    private String host;
}
