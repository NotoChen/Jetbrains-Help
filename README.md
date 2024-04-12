
![2024年04月11日22-04-58.gif](https://img2.imgtp.com/2024/04/11/msZXv3CL.gif)


# 接着,介绍技术栈:

* **Java21**               (本地暂仅支持该版本)
* **SpringBoot**         (Web容器DDDD)
* **Maven**                (打包YYDS)
* **Lombok**              (简化代码没啥好说的)
* **Thymeleaf**          (模板引擎)
* **Hutool**                (敬礼)
* **Bouncycastle**     (证书生成必备)

# 然后,介绍一下功能

1. 公钥/私钥/证书,自动生成
2. 支持IDE+PLUGIN激活(类似括号/Req等有手段插件暂不支持)
3. 插件列表按名称排序,并且支持每天自动更新(每天12点自动刷新)
4. power.conf文件自动生成配置
5. ja-netfilter.jar文件自动生成打包
6. 支持自定义License Show
7. 页面展示IDE+PLUGIN列表,支持实时搜索
8. 支持生成全局通用激活码(一个激活码可激活所有)
9. 支持打包Jar运行
10. 支持Docker运行,内置Dockerfile
11. ...

# 最后,讲一下心路历程

>冗余的内容就不多赘述, 直接讲都碰到了哪些常见问题,怎么解决.
>首先还是引用了**Jetbrains-Key** + **Jenbrains-Go** 这两个项目.
>用Jetbrains-Key生成证书,用Jetbrains-Go展开页面.
>服务起多了,自然也难受,所以想用本专业Java来实现完整全套的服务.


> 首先是插件列表,得益于前有热佬展开了实现,知晓了Jetbrains官方的插件接口,从而能够获取插件列表,获取插件详情.

## 第一个插件维护类

https://github.com/NotoChen/Jetbrains-Help/blob/main/src/main/java/com/jetbrains/help/context/PluginsContextHolder.java

```
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
```
> 核心代码就是服务启动后,读取plugin.json文件, 如果不存在则创建, 如果内容为空或者内容格式非Json格式,则说明plugin列表未加载,此时则进入refreshJsonFile()

```
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
```
> 如代码片段所示,首先调用Jetbrains官方API获取插件列表,然后过滤掉免费插件,以及本地插件库已存在的插件,再进行数据转换便于最后的Json文件写入

> **所以本地库的plugin.json文件是非必须文件,程序是可以自动生成的**

> 最初热佬是由Go开发的plugin.go, 但是总觉得插件列表很混乱,习惯性的想加上排序,自然而然想到官方是否支持,经过官网爬取实验,找到了OrderBy参数字段.

> 然后,发现Index.html维护写死了一套IDE列表,觉得很冗长,不如类似插件列表的展示,于是.
## 第二个IDE维护类

https://github.com/NotoChen/Jetbrains-Help/blob/main/src/main/java/com/jetbrains/help/context/ProductsContextHolder.java

```
public static void init() {
        log.info("Product context init loading...");
        File productJsonFile = FileTools.getFileOrCreat(PRODUCT_JSON_FILE_NAME);

        String productJsonArray;
        try {
            productJsonArray = IoUtil.readUtf8(FileUtil.getInputStream(productJsonFile));
        } catch (IORuntimeException e) {
            throw new IllegalArgumentException(CharSequenceUtil.format("{} File read failed !", PRODUCT_JSON_FILE_NAME), e);
        }
        if (CharSequenceUtil.isBlank(productJsonArray) || !JSONUtil.isTypeJSON(productJsonArray)) {
            log.error("Jetbrains Product data does not exist !");
        } else {
            productCacheList = JSONUtil.toList(productJsonArray, ProductCache.class);
            log.info("Product context init success !");
        }
    }
```
> 大致初始化的风格雷同,此处如果未检测到IDE产品库的存在,则仅进行错误日志打印处理

> **所以本地库的product.json文件也是非必须文件,目前阶段还是在于自己维护**

> 预期是能够如插件列表一般自动更新,但是找了一圈没有找到相关可以爬取的接口,所以暂时放弃了

> 然后是公钥/私钥/证书的生成, 于是

## 第三个证书维护类

https://github.com/NotoChen/Jetbrains-Help/blob/main/src/main/java/com/jetbrains/help/context/CertificateContextHolder.java

```
public static void init() {
        log.info("certificate context init loading...");
        rootKeyFile = FileTools.getFileOrCreat(ROOT_KEY_FILE_NAME);
        if (!FileTools.fileExists(PRIVATE_KEY_FILE_NAME)
                || !FileTools.fileExists(PUBLIC_KEY_FILE_NAME)
                || !FileTools.fileExists(CET_FILE_NAME)) {
            log.info("certificate context generate loading...");
            generateCertificate();
        } else {
            privateKeyFile = FileTools.getFileOrCreat(PRIVATE_KEY_FILE_NAME);
            publicKeyFile = FileTools.getFileOrCreat(PUBLIC_KEY_FILE_NAME);
            crtFile = FileTools.getFileOrCreat(CET_FILE_NAME);
        }
        log.info("certificate context init success !");
    }
```
> root.key文件也就是官方发布的公钥证书,项目中应当自带的文件
然后分别读取私钥,公钥,证书文件,但凡有一个不存在,则进行初始化证书生成generateCertificate()

> 因为引入了Hutool,所以想把公私钥和证书生成相关的代码经过一番简化,能更直观的了解学习其中的过程

> 于是翻遍了Hutool的源码, 将公私钥的生成简化成了如下

```
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 4096);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
```

原代码是

```
        Security.addProvider(new BouncyCastleProvider());
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(4096, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
```

> 代码的精简引起了我更深入的兴趣
>
> 于是乎,我陷入了证书生成代码简化的漩涡

> 因为引入了bouncycastle依赖库
>
> Hutool也对该依赖库进行了一定程度的封装,但是类似证书的生成,证书文件的转储,证书文件的读取等等,都需要相当程度的底蕴才能够自如改写.
>
> 中途出现了很多小问题,例如对type参数的未知,Algorithm参数,signatureAlgorithm参数,加密算法,证书格式,BC Procider等等
>
> 但是终归还是完成了代码的梳理

```
private static void generateCertificate() {
        KeyPair keyPair = SecureUtil.generateKeyPair("RSA", 4096);
        PrivateKey privateKey = keyPair.getPrivate();
        PublicKey publicKey = keyPair.getPublic();
        privateKeyFile = FileTools.getFileOrCreat(PRIVATE_KEY_FILE_NAME);
        PemUtil.writePemObject("PRIVATE KEY", privateKey.getEncoded(), FileUtil.getWriter(privateKeyFile, StandardCharsets.UTF_8, false));
        publicKeyFile = FileTools.getFileOrCreat(PUBLIC_KEY_FILE_NAME);
        PemUtil.writePemObject("PUBLIC KEY", publicKey.getEncoded(), FileUtil.getWriter(publicKeyFile, StandardCharsets.UTF_8, false));
        JcaX509v3CertificateBuilder certificateBuilder = new JcaX509v3CertificateBuilder(
                new X500Name("CN=JetProfile CA"),
                BigInteger.valueOf(System.currentTimeMillis()),
                DateUtil.yesterday(),
                DateUtil.date().offset(DateField.YEAR, 100),
                new X500Name("CN=Jetbrains-Help"),
                SubjectPublicKeyInfo.getInstance(publicKey.getEncoded()));
        try {
            ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(privateKey);
            Certificate certificate = new JcaX509CertificateConverter().setProvider("BC").getCertificate(certificateBuilder.build(signer));
            crtFile = FileTools.getFileOrCreat(CET_FILE_NAME);
            PemUtil.writePemObject("CERTIFICATE", certificate.getEncoded(), FileUtil.getWriter(crtFile, StandardCharsets.UTF_8, false));
        } catch (OperatorCreationException e) {
            throw new IllegalArgumentException("Certificate operator creation exception", e);
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("The certificate encoding exception", e);
        } catch (CertificateException e) {
            throw new IllegalArgumentException("The certificate read exception", e);
        }
    }
```
> 生成过程几乎大同小异,我只是尽可能做了精简处理, 比如公私钥的生成,公私钥文件的转储,证书的生成依托于bouncycastle,Hutool暂时没有在这方面进行更深度的封装,所以流程几乎一样, 证书文件的生成也同样进行了精简处理.

> **这也就意味着,项目本身的ca.crt/private.key/public.key文件都是非必须要的, 程序是可以自动生成的**

> 证书生成成功了,接下来就是power.conf的配置生成了, 于是

## 第四个Agent维护类

https://github.com/NotoChen/Jetbrains-Help/blob/main/src/main/java/com/jetbrains/help/context/AgentContextHolder.java

> 预期实现的效果自然也是全自动生成power.conf的配置,并自动写入文件,打包ja-netfilter.zip,经由index.html下载,剩下则由使用者自行配置IDE's Vmoptions即可

```
public static void init() {
        log.info("Agent context init loading...");
        jaNetfilterFile = FileTools.getFileOrCreat(JA_NETFILTER_FILE_PATH);
        if (!powerConfHasInit()) {
            log.info("Agent config init loading...");
            loadPowerConf();
            zipJaNetfilter();
            log.info("Agent config init success !");
        }
        log.info("Agent context init success !");
    }
```
```
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
```

> 代码的实现也同样大同小异,我只是精简了公私钥和证书文件的读取等

> **因为我对项目本身的ja-netfilter/conf/power.conf文件进行了检测,需要同时具备Result头和Equal内容,才算进行了配置,所以Agent文件的生成也是自动化的**

> ### 这里有几个问题

> #### 首先是ja-netfilter文件的来源
>
> 其一是始皇的公开仓库,ja-netfilter
> 其二是热佬https://3.jetbra.in/ 提供的下载
>
> 他们之间的区别就在于,始皇的plugin中有一个native.jar,而热佬没有
>
> 导致我生成的power.conf配置一样的情况下,前者激活失败,后者激活成功
>
> 后来发现把native.jar删除即可
> 此处可能要手动艾特一下始皇,这个native的作用是什么,为什么会影响,或者有其他热佬了解的话也可以解释一下为什么,如果不删除的话要如何处理才可以

> #### 然后是关于ja-netfilter中conf/plugin目录的命名问题
> 始皇仓库的readme.md有详细描述,为了隔离配置和插件可以添加appName
>
> 所以热佬提供的ja-netfilter解压出来,conf/plugin目录实际都是conf-jetbrains/plugin-jetbrains
>
> 要使用热佬的ja-netfilter就必须在vmoptions中配置agent时,加上=jetbrains
>
> 也就是-javaagent:/(Your Path)/ja-netfilter/ja-netfilter.jar=jetbrains
>
> 只有这样才会生效

> 如果是始皇仓库的原生ja-netfilter则不需要加=jetbrains

> 然而在上述致谢的几篇教程当中,关于这一点的解释其实比较混乱,所以导致很多人试验不成功

**在此声明一下**

> **施教者务必先自顺**

当然,没有其他意思,不要见怪,只是一个呼吁和提倡

> 然后就是激活码的生成了, 于是

## 第五个License维护类

https://github.com/NotoChen/Jetbrains-Help/blob/main/src/main/java/com/jetbrains/help/context/LicenseContextHolder.java

```
public static String generateLicense(String licensesName, String assigneeName, String expiryDate, Set<String> productCodeSet) {
        String licenseId = IdUtil.fastSimpleUUID();
        List<Product> products = productCodeSet.stream()
                .map(productCode -> new Product()
                        .setCode(productCode)
                        .setFallbackDate(expiryDate)
                        .setPaidUpTo(expiryDate))
                .toList();
        LicensePart licensePart = new LicensePart()
                .setLicenseId(licenseId)
                .setLicenseeName(licensesName)
                .setAssigneeName(assigneeName)
                .setProducts(products);
        String licensePartJson = JSONUtil.toJsonStr(licensePart);
        String licensePartBase64 = Base64.encode(licensePartJson);
        PrivateKey privateKey = PemUtil.readPemPrivateKey(IoUtil.toStream(CertificateContextHolder.privateKeyFile()));
        PublicKey publicKey = PemUtil.readPemPublicKey(IoUtil.toStream(CertificateContextHolder.publicKeyFile()));
        Certificate certificate = SecureUtil.readX509Certificate(IoUtil.toStream(CertificateContextHolder.crtFile()));
        Sign sign = SignUtil.sign(SHA1withRSA, privateKey.getEncoded(), publicKey.getEncoded());
        String signatureBase64 = Base64.encode(sign.sign(licensePartJson));
        String certBase64;
        try {
            certBase64 = Base64.encode(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            throw new IllegalArgumentException("Certificate extraction failed", e);
        }
        return CharSequenceUtil.format("{}-{}-{}-{}", licenseId, licensePartBase64, signatureBase64, certBase64);
    }
```

> 我精简了各类公私钥/证书的读取和使用代码,精简了算法加密代码,尽可能通过hutool减少造的轮子

> 同时也经过极多轮的试验,验证了LicensePart的必要字段,把多余字段全部抹除

```
@Data
    public static class LicensePart {

        private String licenseId;
        private String licenseeName;
        private String assigneeName;
        private List<Product> products;
        private String metadata = "0120230914PSAX000005";
    }

    @Data
    public static class Product {
        private String code;
        private String fallbackDate;
        private String paidUpTo;
    }
```

**此处留一个问题**

> 那就是关于LicensePart中的metadata字段,似乎是必要条件,而根据致谢热佬的解答只能通过正版激活码反解析获得,所以这个字段值目前是写死的
>
> 我个人还是比较好奇生成规则的,如果有热佬研究出来可以自定义那就更好了
>
> 我会持续关注的

> 以上代码调用流程图如下

![JetbrainsHelpApplication_ready](https://cdn.linux.do/uploads/default/original/3X/f/8/f8fea80e47597659762fb273cc352b1ae6782c2f.png)



> OKOK
>
> 核心core相关的代码都已经整理好了
>
> 接下来就是页面的展示了
>
> index.html还是套用的现成的热佬文件

> **但是我加入了一些东西**

https://github.com/NotoChen/Jetbrains-Help/blob/main/src/main/resources/templates/index.html

> 首先是ja-netfilter.zip文件的下载功能

```
    <p>
        🇨🇳 Download <a href="/agent/ja-netfilter.zip" title="Download jetbra first">ja-netfilter.zip</a>
    </p>
```

> 然后是vmoptions的配置拷贝 (点击即可alter展示可拷贝配置项)

```
    <p>
        configure your JetBrains's <strong onclick="showVmoptins()">vmoptions!</strong> <br>
    </p>
```

> 接着是自定义LicenseInfo,可以自定义激活信息和时间等

```
    <p>
        🇨🇳 Also you can <a onclick="showLicenseForm()">Refill license information</a> to customizer your license!</br>
    </p>
```

当然这些都是热佬的肩膀

> **我另外额外添加了**

> 搜索功能, 可以实时输入并检索IDE和PLUGIN,实时展示

```
    <form class="parent">
        <input type="text" class="search" id="search" placeholder="Search Product or Plugin">
    </form>
```

```
document.getElementById('search').oninput = function (e) {
    $("#product-list").load('/search?search=' + e.target.value)
}
```

就是简单的oninput监听,以及结合thymeleaf的局部刷新实现的,源码很简单

```
@GetMapping
    public String index(Model model) {
        List<ProductsContextHolder.ProductCache> productCacheList = ProductsContextHolder.productCacheList();
        List<PluginsContextHolder.PluginCache> pluginCacheList = PluginsContextHolder.pluginCacheList();
        model.addAttribute("products", productCacheList);
        model.addAttribute("plugins", pluginCacheList);
        model.addAttribute("defaults", jetbrainsHelpProperties);
        return "index";
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
```

## 重磅

![2024年04月11日22-33-38.gif](https://img2.imgtp.com/2024/04/11/nPkaf21M.gif)

### 我加入了一个新的Card

![image](https://cdn.linux.do/uploads/default/original/3X/2/e/2ebb9d57e9bd8930d37840120fff6edc1716ca64.png)

> 也就是前面提到的,单码全家桶激活
>
> 本质上就是直接拿所有已知的code去生成LicenseCode

> 因为不善于前端
>
> 所以对于html的改动不多
>
> UI也暂时没有想法优化
>
> 还是老老实实站在热佬的肩膀上舒服

> 另外就是把CSS/JS都单独拎出来,来简化HTML的代码,看起来更舒服了
>
> 以上差不多就是全部
>
> 更多的小改动,可配置化的东西,还是看代码实现吧

> 服务端口号定义的是10768
>
> 因为我觉得768是Jetbrains的谐音,hhhh


# FAQ (实时记录)

# UPDATE HISTORY

## 2024年04月11日 22:15

> **修复licenseName错误传参问题**

## 2024年04月12日 00:40

> **修复瞬间出现"we could not validate your license"的问题**

## 2024年04月12日 15:39

> **解决打包成Jar不能运行的问题**
> **增加Dockerfile文件**
> **等等**
