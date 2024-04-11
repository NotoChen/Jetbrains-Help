package com.jetbrains.help.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties("help")
public class JetbrainsHelpProperties {

    private String defaultLicenseName;

    private String defaultAssigneeName;

    private String defaultExpiryDate;
}
