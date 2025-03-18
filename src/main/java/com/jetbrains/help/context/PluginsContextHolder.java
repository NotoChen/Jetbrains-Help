package com.jetbrains.help.context;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.jetbrains.help.util.FileTools;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Slf4j(topic = "插件上下文")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginsContextHolder {

    private static final String PLUGIN_BASIC_URL = "https://plugins.jetbrains.com";

    private static final String PLUGIN_LIST_URL = PLUGIN_BASIC_URL + "/api/searchPlugins?max=10000&offset=0&orderBy=name";

    private static final String PLUGIN_INFO_URL = PLUGIN_BASIC_URL + "/api/plugins/";

    private static final String PLUGIN_JSON_FILE_NAME = "external/data/plugin.json";

    private static List<PluginCache> pluginCacheList;

    private static File pluginsJsonFile;

    public static void init() {
        log.info("初始化中...");
        pluginsJsonFile = FileTools.getFileOrCreat(PLUGIN_JSON_FILE_NAME);

        String pluginJsonArray;
        try {
            pluginJsonArray = IoUtil.readUtf8(FileUtil.getInputStream(pluginsJsonFile));
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(CharSequenceUtil.format("{} 文件读取失败!", PLUGIN_JSON_FILE_NAME), e);
        }
        if (CharSequenceUtil.isBlank(pluginJsonArray) || !JSONUtil.isTypeJSON(pluginJsonArray)) {
            pluginCacheList = new ArrayList<>();
            refreshJsonFile();
        } else {
            pluginCacheList = JSONUtil.toList(pluginJsonArray, PluginCache.class);
            log.info("初始化成功!");
            refreshJsonFile();
        }
    }

    public static List<PluginCache> pluginCacheList() {
        return PluginsContextHolder.pluginCacheList;
    }

    public static void refreshJsonFile() {
        log.info("从'JetBrains.com'刷新中...");
        CompletableFuture
                .supplyAsync(PluginsContextHolder::pluginList)
                .thenApply(PluginsContextHolder::pluginListFilter)
                .thenApply(PluginsContextHolder::pluginConversion)
                .thenAccept(PluginsContextHolder::overrideJsonFile)
                .thenRun(() -> log.info("刷新成功!"))
                .exceptionally(throwable -> {
                    log.error("刷新失败!", throwable);
                    return null;
                });
    }

    public static void overrideJsonFile(List<PluginCache> pluginCaches) {
        log.info("源大小 => [{}], 新增大小 => [{}]", pluginCacheList.size(), pluginCaches.size());
        pluginCacheList.addAll(pluginCaches);
        String jsonStr = JSONUtil.toJsonStr(pluginCacheList);
        try {
            FileUtil.writeString(JSONUtil.formatJsonStr(jsonStr), pluginsJsonFile, StandardCharsets.UTF_8);
            log.info("Json文件已覆写!");
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(CharSequenceUtil.format("{} 文件写入失败!", PLUGIN_JSON_FILE_NAME), e);
        }

    }

    public static PluginList pluginList() {
        return HttpUtil.createGet(PLUGIN_LIST_URL)
                .thenFunction(response -> {
                    try (InputStream is = response.bodyStream()) {
                        if (!response.isOk()) {
                            throw new IllegalArgumentException(CharSequenceUtil.format("{} 请求失败! = {}", PLUGIN_LIST_URL, response));
                        }
                        PluginList pluginList = JSONUtil.toBean(IoUtil.readUtf8(is), PluginList.class);
                        log.info("获取大小 => [{}]", pluginList.getTotal());
                        return pluginList;
                    } catch (IOException e) {
                        throw new IllegalArgumentException(CharSequenceUtil.format("{} 请求IO读取失败!", PLUGIN_LIST_URL), e);
                    }
                });
    }

    public static List<PluginList.Plugin> pluginListFilter(PluginList pluginList) {
        List<PluginList.Plugin> plugins = pluginList.getPlugins()
                .stream()
                .filter(plugin -> !PluginsContextHolder.pluginCacheList.contains(new PluginCache().setId(plugin.getId())))
                .filter(plugin -> !CharSequenceUtil.equals(plugin.getPricingModel(), "FREE"))
                .toList();
        log.info("过滤后大小 => [{}]", plugins.size());
        return plugins;
    }

    public static List<PluginCache> pluginConversion(List<PluginList.Plugin> pluginList) {
        List<PluginCache> list = pluginList
                .stream()
                .parallel()
                .map(plugin -> {
                    String productCode = pluginInfo(plugin).getPurchaseInfo().getProductCode();
                    return new PluginCache()
                            .setId(plugin.getId())
                            .setProductCode(productCode)
                            .setName(plugin.getName())
                            .setPricingModel(plugin.getPricingModel())
                            .setIcon(StrUtil.isNotBlank(plugin.getIcon()) ? PLUGIN_BASIC_URL + plugin.getIcon() : null)
                            ;
                })
                .toList();
        log.info("转换后大小 => [{}]", list.size());
        return list;
    }

    public static PluginInfo pluginInfo(PluginList.Plugin plugin) {
        return HttpUtil.createGet(PLUGIN_INFO_URL + plugin.getId())
                .thenFunction(response -> {
                    try (InputStream is = response.bodyStream()) {
                        if (!response.isOk()) {
                            throw new IllegalArgumentException(CharSequenceUtil.format("{} 请求失败! = {}", PLUGIN_INFO_URL, response));
                        }
                        PluginInfo pluginInfo = JSONUtil.toBean(IoUtil.readUtf8(is), PluginInfo.class);
                        log.info("已抓取 => ID = [{}], 名称 = [{}], Code = [{}]", pluginInfo.getId(), plugin.getName(), pluginInfo.getPurchaseInfo().getProductCode());
                        return pluginInfo;
                    } catch (IOException e) {
                        throw new IllegalArgumentException(CharSequenceUtil.format("{} 请求IO读取失败!", PLUGIN_LIST_URL), e);
                    }
                });
    }


    @Data
    public static class PluginCache {

        private Long id;
        private String productCode;
        private String name;
        private String pricingModel;
        private String icon;

        @Override
        public final boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PluginCache that)) return false;

            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    @Data
    public static class PluginInfo {

        private Long id;

        private PurchaseInfo purchaseInfo;

        @Data
        public static class PurchaseInfo {

            private String productCode;
        }
    }

    @Data
    public static class PluginList {

        private List<Plugin> plugins;
        private Long total;


        @Data
        public static class Plugin {

            private Long id;
            private String name;
            private String preview;
            private Integer downloads;
            private String pricingModel;
            private String organization;
            private String icon;
            private String previewImage;
            private Double rating;
            private VendorInfo vendorInfo;
        }

        @Data
        public static class VendorInfo {
            private String name;
            private Boolean isVerified;
        }
    }
}
