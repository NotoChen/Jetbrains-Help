package com.jetbrains.help.context;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
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
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginsContextHolder {

    private static final String PLUGIN_BASIC_URL = "https://plugins.jetbrains.com";

    private static final String PLUGIN_LIST_URL = PLUGIN_BASIC_URL + "/api/searchPlugins?max=10000&offset=0&orderBy=name";

    private static final String PLUGIN_INFO_URL = PLUGIN_BASIC_URL + "/api/plugins/";

    private static final String PLUGIN_JSON_FILE_NAME = "external/data/plugin.json";

    private static List<PluginCache> pluginCacheList;

    private static File pluginsJsonFile;

    public static void init() {
        log.info("Plugin context init loading...");
        pluginsJsonFile = FileTools.getFileOrCreat(PLUGIN_JSON_FILE_NAME);

        String pluginJsonArray;
        try {
            pluginJsonArray = IoUtil.readUtf8(FileUtil.getInputStream(pluginsJsonFile));
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(CharSequenceUtil.format("{} File read failed", PLUGIN_JSON_FILE_NAME), e);
        }
        if (CharSequenceUtil.isBlank(pluginJsonArray) || !JSONUtil.isTypeJSON(pluginJsonArray)) {
            refreshJsonFile();
        } else {
            pluginCacheList = JSONUtil.toList(pluginJsonArray, PluginCache.class);
            log.info("Plugin context init success !");
        }
    }

    public static List<PluginCache> pluginCacheList() {
        return PluginsContextHolder.pluginCacheList;
    }

    public static void refreshJsonFile() {
        log.info("Init or Refresh plugin context from 'JetBrains.com' loading...");
        CompletableFuture
                .supplyAsync(PluginsContextHolder::pluginList)
                .thenApply(PluginsContextHolder::pluginListFilter)
                .thenApply(PluginsContextHolder::pluginConversion)
                .thenAccept(PluginsContextHolder::overrideJsonFile)
                .exceptionally(throwable -> {
                    log.error("Plugin context init or refresh failed", throwable);
                    return null;
                });
        log.info("Init or Refresh plugin context success !");
    }

    public static void overrideJsonFile(List<PluginCache> pluginCaches) {
        pluginCacheList.addAll(pluginCaches);
        String jsonStr = JSONUtil.toJsonStr(pluginCacheList);
        try {
            FileUtil.writeString(jsonStr, pluginsJsonFile, StandardCharsets.UTF_8);
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(CharSequenceUtil.format("{} File write failed", PLUGIN_JSON_FILE_NAME), e);
        }

    }

    public static PluginList pluginList() {
        return HttpUtil.createGet(PLUGIN_LIST_URL)
                .thenFunction(response -> {
                    try (InputStream is = response.bodyStream()) {
                        if (!response.isOk()) {
                            throw new IllegalArgumentException(CharSequenceUtil.format("{} The request failed = {}", PLUGIN_LIST_URL, response));
                        }
                        return IoUtil.readObj(is, PluginList.class);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(CharSequenceUtil.format("{} The request io read failed", PLUGIN_LIST_URL), e);
                    }
                });
    }

    public static List<PluginList.Plugin> pluginListFilter(PluginList pluginList) {
        return pluginList.getPlugins()
                .stream()
                .filter(plugin -> !PluginsContextHolder.pluginCacheList.contains(new PluginCache().setId(plugin.getId())))
                .filter(plugin -> !CharSequenceUtil.equals(plugin.getPricingModel(), "FREE"))
                .toList();
    }

    public static List<PluginCache> pluginConversion(List<PluginList.Plugin> pluginList) {
        return pluginList
                .stream()
                .map(plugin -> {
                    String productCode = pluginInfo(plugin.getId()).getPurchaseInfo().getProductCode();
                    return new PluginCache()
                            .setId(plugin.getId())
                            .setProductCode(productCode)
                            .setName(plugin.getName())
                            .setPricingModel(plugin.getPricingModel())
                            .setIcon(PLUGIN_BASIC_URL + plugin.getIcon())
                            ;
                })
                .toList();
    }

    public static PluginInfo pluginInfo(Long pluginId) {
        return HttpUtil.createGet(PLUGIN_INFO_URL + pluginId)
                .thenFunction(response -> {
                    try (InputStream is = response.bodyStream()) {
                        if (!response.isOk()) {
                            throw new IllegalArgumentException(CharSequenceUtil.format("{} The request failed = {}", PLUGIN_INFO_URL, response));
                        }
                        return IoUtil.readObj(is, PluginInfo.class);
                    } catch (IOException e) {
                        throw new IllegalArgumentException(CharSequenceUtil.format("{} The request io read failed", PLUGIN_LIST_URL), e);
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
