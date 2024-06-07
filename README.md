# Jetbrains-Help

## 目录
- [项目说明](#项目说明)
  - [仓库简要](#仓库简要)
  - [仓库趋势](#仓库趋势)
  - [支持版本](#支持版本)
  - [项目版本](#项目版本)
  - [功能列表](#功能列表)
- [运行教程](#运行教程)
  - [拉取项目](#拉取项目)
  - [配置环境](#配置环境)
    - [本地运行](#本地运行)
    - [容器运行](#容器运行)
  - [运行服务](#运行服务)
    - [本地运行](#本地运行)
      - [有IDE](#有IDE)
      - [无IDE](#无IDE)
    - [容器运行](#容器运行)
- [使用教程](#使用教程)
  - [下载依赖](#下载依赖)
  - [依赖配置](#依赖配置)
    - [可打开IDE](#可打开IDE)
    - [不可打开IDE](#可打开IDE)

## 项目说明
### 仓库简要
<p align="left">
    <img src="https://img.shields.io/github/stars/NotoChen/Jetbrains-Help">
    <img src="https://img.shields.io/github/forks/NotoChen/Jetbrains-Help">
    <img src="https://img.shields.io/github/repo-size/notochen/jetbrains-help">
    <img src="https://img.shields.io/github/license/notochen/jetbrains-help">
</p>

### 仓库趋势
<p align="center">
    <img src="https://api.star-history.com/svg?repos=NotoChen/Jetbrains-Help&type=Date">
</p>


### 支持版本
<p align="left">
    <img src="https://img.shields.io/badge/Jetbrains_Version-All-%23000000?logo=jetbrains&labelColor=black&color=white">
</p>

### 项目版本
<p align="left">
    <img src="https://img.shields.io/badge/Java_Version-21-%23000000?logo=openjdk&&color=white">
    <img src="https://img.shields.io/badge/Maven_Version-Laster-%23000000?logo=apachemaven&&color=white">
    <img src="https://img.shields.io/badge/SpringBoot_Version-Laster-%23000000?logo=springboot&&color=white">
    <img src="https://img.shields.io/badge/Thymeleaf_Version-Laster-%23000000?logo=thymeleaf&&color=white">
</p>

### 功能列表

| 功能                       | DID |
|:-------------------------|:---:|
| Jetbrains全产品支持           |  ✅  |
| Jetbrains全插件支持           |  ✅  |
| 插件库全自动订阅官网更新             |  ✅  |
| 公私钥/证书, 自动生成管理           |  ✅  |
| power.conf文件自动配置         |  ✅  |
| ja-netfilter.zip自动打包     |  ✅  |
| 自定义License Show          |  ✅  |
| 支持实时搜索                   |  ✅  |
| 插件默认按名称排序                |  ✅  |
| 支持local/jar/dockerfile运行 |  ✅  |
| 单码全家桶激活支持                |  ✅  |
| ……                       | ☑️  |

## 运行教程

> 以下是该项目详细运行教程, 尽量争取可以在各个环境下运作

### 拉取项目

`clone` 本项目至本地

### 配置环境

#### 本地运行

1. 需要 `Java` 环境，并且版本要求 **21**
2. 需要 `Maven` 环境，版本无要求，但建议采用最新版

#### 容器运行
1. 需要 `Docker` 环境，版本无要求，但建议采用最新版
2. 如有 `Docker-Compos` 环境，更佳，但此环境**非必须**

### 运行服务

#### 本地运行

##### 有IDE

1. 通过 `IDE` `Open` 项目
2. 配置项目相关环境
3. 运行 [JetbrainsHelpApplication.java](src%2Fmain%2Fjava%2Fcom%2Fjetbrains%2Fhelp%2FJetbrainsHelpApplication.java)

##### 无IDE

1. 系统终端 `Cd` 进入项目根目录
2. 运行打包命令 `mvn clean package`
3. 运行启动命令 `java -jar target/Jetbrains-Help.jar`

#### 容器运行

1. 系统终端 `Cd` 进入项目根目录

##### 使用Docker
2. 运行 `Docker` 命令 `docker build -t jetbrains-help .`
3. **或者** 执行 [build-with-docker.sh](build-with-docker.sh)
4. 运行 `Docker` 命令 `docker run -d -p 10768:10768 --name jetbrains-help jetbrains-help`
5. **或者** 执行 [run-with-docker.sh](run-with-docker.sh)

##### 使用Docker-Compose

2. 运行 `Docker-Compose` 命令 `docker compose build && docker compose up -d`
3. **或者** 执行 [run-with-docker-compose.sh](run-with-docker-compose.sh)

### 使用教程

项目运行后, `Console` 会打印相关服务地址, 默认端口为 `10768`, 默认地址为 `127.0.0.1:10768`

可以点此直接访问 [Jetbrains-Help](http://127.0.0.1:10768)

#### 下载依赖

阅读 **页面头部**，根据头部指引下载 `ja-netfilter.zip`

移动本地 `ja-netfilter.zip` 到自定义目录，**解压**

#### 依赖配置

##### 可打开IDE

- `进入IDE`
- **点击** 菜单栏 `帮助（help）`
- **点击** `编辑自定义虚拟机选型`
- **键入** 如下配置
```
-javaagent:you-path/ja-netfilter.jar
--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED
--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED
```
- 将`you-path`替换为 [下载依赖](#下载依赖) 步骤中自定义目录
- **重启** `IDE`

##### 不可打开IDE

- **下载安装** [Toolbox](https://www.jetbrains.com/toolbox-app/)
- **启动** `Toolbox`
- **点击** `Toolbox` 找到对应 `IDE` 
- **点击** `IDE` 右侧的 `⋮`
- **点击** `设置`
- 找到 `配置` 选项
- **点击** `编辑JVM选项`
- **键入** 如下配置
```
-javaagent:you-path/ja-netfilter.jar
--add-opens=java.base/jdk.internal.org.objectweb.asm=ALL-UNNAMED
--add-opens=java.base/jdk.internal.org.objectweb.asm.tree=ALL-UNNAMED
```
- 将`you-path`替换为 [下载依赖](#下载依赖) 步骤中自定义目录
- **重启** `IDE`

