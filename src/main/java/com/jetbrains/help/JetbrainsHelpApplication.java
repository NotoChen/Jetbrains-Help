package com.jetbrains.help;

import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.jetbrains.help.context.*;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.*;

import java.net.InetAddress;

@Slf4j(topic = "源项目入口")
@EnableScheduling
@Import(SpringUtil.class)
@SpringBootApplication
public class JetbrainsHelpApplication {

    public static void main(String[] args) {
        SpringApplication.run(JetbrainsHelpApplication.class, args);
    }

    @SneakyThrows
    @EventListener(ApplicationReadyEvent.class)
    public void ready() {
        ProductsContextHolder.init();
        PluginsContextHolder.init();
        CertificateContextHolder.init();
        AgentContextHolder.init();

        InetAddress localHost = InetAddress.getLocalHost();
        String address = CharSequenceUtil.format("http://{}:{}", localHost.getHostAddress(), SpringUtil.getProperty("server.port"));
        String runSuccessWarn = "\n====================================================================================\n" +
                "=                        Jetbrains-Help 启动成功~                                   =\n" +
                "=                        访问地址:" + address + "                            =\n" +
                "====================================================================================\n";
        log.info(runSuccessWarn);
    }

    @Scheduled(cron = "0 0 12 * * ?")
    public void refresh() {
        ThreadUtil.execute(PluginsContextHolder::refreshJsonFile);
    }

}
