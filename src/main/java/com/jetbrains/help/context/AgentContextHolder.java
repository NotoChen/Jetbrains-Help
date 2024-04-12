package com.jetbrains.help.context;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ZipUtil;
import cn.hutool.crypto.KeyUtil;
import cn.hutool.crypto.PemUtil;
import com.jetbrains.help.util.FileTools;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.CompletableFuture;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentContextHolder {

    private static final String JA_NETFILTER_FILE_PATH = "external/agent/ja-netfilter";

    private static final String POWER_CONF_FILE_NAME = JA_NETFILTER_FILE_PATH + "/config/power.conf";

    private static File jaNetfilterFile;

    private static File jaNetfilterZipFile;

    public static void init() {
        log.info("Agent context init loading...");
        jaNetfilterZipFile = FileTools.getFileOrCreat(JA_NETFILTER_FILE_PATH + ".zip");
        if (!FileTools.fileExists(JA_NETFILTER_FILE_PATH)) {
            unzipJaNetfilter();
            if (!powerConfHasInit()) {
                log.info("Agent config init loading...");
                loadPowerConf();
                zipJaNetfilter();
                log.info("Agent config init success !");
            }
        }
        log.info("Agent context init success !");
    }

    public static File jaNetfilterZipFile() {
        return AgentContextHolder.jaNetfilterZipFile;
    }

    private static boolean powerConfHasInit() {
        File powerConfFile = FileTools.getFileOrCreat(POWER_CONF_FILE_NAME);
        String powerConfStr;
        try {
            powerConfStr = IoUtil.readUtf8(FileUtil.getInputStream(powerConfFile));
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(CharSequenceUtil.format("{} File read failed", POWER_CONF_FILE_NAME), e);
        }
        return CharSequenceUtil.containsAll(powerConfStr, "[Result]", "EQUAL,");
    }

    private static void loadPowerConf() {
        CompletableFuture
                .supplyAsync(AgentContextHolder::generatePowerConfigRule)
                .thenApply(AgentContextHolder::generatePowerConfigStr)
                .thenAccept(AgentContextHolder::overridePowerConfFileContent)
                .exceptionally(throwable -> {
                    log.error("agent context init or refresh failed", throwable);
                    return null;
                }).join();
    }

    @SneakyThrows
    private static String generatePowerConfigRule() {
        X509Certificate crt = (X509Certificate) KeyUtil.readX509Certificate(IoUtil.toStream(CertificateContextHolder.crtFile()));
        RSAPublicKey publicKey = (RSAPublicKey) PemUtil.readPemPublicKey(IoUtil.toStream(CertificateContextHolder.publicKeyFile()));
        RSAPublicKey rootPublicKey = (RSAPublicKey) PemUtil.readPemPublicKey(IoUtil.toStream(CertificateContextHolder.rootKeyFile()));
        BigInteger x = new BigInteger(1, crt.getSignature());
        BigInteger y = BigInteger.valueOf(65537L);
        BigInteger z = rootPublicKey.getModulus();
        BigInteger r = x.modPow(publicKey.getPublicExponent(), publicKey.getModulus());
        return CharSequenceUtil.format("EQUAL,{},{},{}->{}", x, y, z, r);
    }

    private static String generatePowerConfigStr(String ruleValue) {
        return CharSequenceUtil.builder("[Result]", "\n", ruleValue).toString();
    }

    private static void overridePowerConfFileContent(String configStr) {
        File powerConfFile = FileTools.getFileOrCreat(POWER_CONF_FILE_NAME);
        try {
            FileUtil.writeString(configStr, powerConfFile, StandardCharsets.UTF_8);
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(CharSequenceUtil.format("{} File write failed", POWER_CONF_FILE_NAME), e);
        }
    }

    private static void unzipJaNetfilter() {
        jaNetfilterFile = ZipUtil.unzip(jaNetfilterZipFile);
    }

    private static void zipJaNetfilter() {
        jaNetfilterZipFile = ZipUtil.zip(jaNetfilterFile);
    }
}
