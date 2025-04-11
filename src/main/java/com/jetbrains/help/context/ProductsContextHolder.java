package com.jetbrains.help.context;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IORuntimeException;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.json.JSONUtil;
import com.jetbrains.help.util.FileTools;
import lombok.AccessLevel;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j(topic = "产品上下文")
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ProductsContextHolder {

    private static final String PRODUCT_JSON_FILE_NAME = "external/data/product.json";

    private static List<ProductCache> productCacheList;

    // TODO 通过该接口可以获取付费IDE的CODE
    // TODO https://data.services.jetbrains.com/products?fields=name,salesCode

    public static void init() {
        log.info("初始化中...");
        File productJsonFile = FileTools.getFileOrCreat(PRODUCT_JSON_FILE_NAME);

        String productJsonArray;
        try {
            productJsonArray = IoUtil.readUtf8(FileUtil.getInputStream(productJsonFile));
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(CharSequenceUtil.format("{} 文件读取失败!", PRODUCT_JSON_FILE_NAME), e);
        }
        if (CharSequenceUtil.isBlank(productJsonArray) || !JSONUtil.isTypeJSON(productJsonArray)) {
            log.error("产品数据不存在!");
        } else {
            productCacheList = JSONUtil.toList(productJsonArray, ProductCache.class);
            log.info("初始化成功!");
        }
    }

    public static List<ProductCache> productCacheList() {
        return ProductsContextHolder.productCacheList;
    }

    @Data
    public static class ProductCache {

        private String name;
        private String productCode;
        private String iconClass;
    }
}
