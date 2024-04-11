package com.jetbrains.help;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jetbrains.help.context.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.util.Collection;
import java.util.List;

@EnableScheduling
@Import(SpringUtil.class)
@SpringBootApplication
public class JetbrainsHelpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetbrainsHelpApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        ProductsContextHolder.init();
        PluginsContextHolder.init();
        CertificateContextHolder.init();
        AgentContextHolder.init();
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void refresh() {
        PluginsContextHolder.refreshJsonFile();
    }

}
