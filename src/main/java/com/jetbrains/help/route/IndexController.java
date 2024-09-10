package com.jetbrains.help.route;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.jetbrains.help.JetbrainsHelpApplication;
import com.jetbrains.help.context.AgentContextHolder;
import com.jetbrains.help.context.PluginsContextHolder;
import com.jetbrains.help.context.ProductsContextHolder;
import com.jetbrains.help.properties.JetbrainsHelpProperties;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.cglib.core.Local;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.util.DateUtils;

import java.io.File;
import java.util.*;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.http.MediaType.APPLICATION_OCTET_STREAM;

@Controller
@RequiredArgsConstructor
public class IndexController {

    private final JetbrainsHelpProperties jetbrainsHelpProperties;

    @GetMapping
    public String index(Model model) {
        List<ProductsContextHolder.ProductCache> productCacheList = ProductsContextHolder.productCacheList();
        List<PluginsContextHolder.PluginCache> pluginCacheList = PluginsContextHolder.pluginCacheList();
        model.addAttribute("products", productCacheList);
        model.addAttribute("plugins", pluginCacheList);
        model.addAttribute("defaults", jetbrainsHelpProperties);
        return "index";
    }

    @GetMapping("/scoop/ja-netfilter")
    @ResponseBody
    public ResponseEntity<Map<String,Object>> scoopInstall(HttpServletRequest request) {
        String basePath = (request.getScheme() + "://" + request.getServerName() + ":" + request.getServerPort() + request.getContextPath());
        Map<String, Object> json = new HashMap<>();
        String version = DateUtils.format(new Date(),"yyyyMMdd", Locale.getDefault());
        json.put("version", version);
        json.put("description", "JetBrains' dragon slayer");
        json.put("homepage", "https://cikaros.top");
        json.put("license", "MIT");
        json.put("url", String.format("%s/ja-netfilter#dl.zip", basePath));
        json.put("extract_to", Arrays.asList("", "config-jetbrains", "plugins-jetbrains", "scripts", "vmoptions"));
        json.put("hash", DigestUtil.sha256Hex(AgentContextHolder.jaNetfilterZipFile()));
        json.put("post_install", "cscript $dir/scripts/install-current-user.vbs");
        json.put("pre_uninstall", "cscript $dir/scripts/uninstall-current-user.vbs");
        return ResponseEntity.ok()
                .contentType(APPLICATION_JSON)
                .body(json);
    }

    @GetMapping("search")
    public String index(@RequestParam(required = false) String search, Model model) {
        List<ProductsContextHolder.ProductCache> productCacheList = ProductsContextHolder.productCacheList();
        List<PluginsContextHolder.PluginCache> pluginCacheList = PluginsContextHolder.pluginCacheList();
        if (CharSequenceUtil.isNotBlank(search)) {
            productCacheList = productCacheList.stream()
                    .filter(productCache -> CharSequenceUtil.containsIgnoreCase(productCache.getName(), search))
                    .toList();
            pluginCacheList = pluginCacheList.stream()
                    .filter(pluginCache -> CharSequenceUtil.containsIgnoreCase(pluginCache.getName(), search))
                    .toList();
        }
        model.addAttribute("products", productCacheList);
        model.addAttribute("plugins", pluginCacheList);
        model.addAttribute("defaults", jetbrainsHelpProperties);
        return "index::product-list";
    }

    @GetMapping("ja-netfilter")
    @ResponseBody
    public ResponseEntity<Resource> downloadJaNetfilter() {
        File jaNetfilterZipFile = AgentContextHolder.jaNetfilterZipFile();
        return ResponseEntity.ok()
                .header(CONTENT_DISPOSITION, "attachment;filename=" + jaNetfilterZipFile.getName())
                .contentType(APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(FileUtil.getInputStream(jaNetfilterZipFile)));
    }

}
