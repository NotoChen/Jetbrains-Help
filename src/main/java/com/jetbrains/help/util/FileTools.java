package com.jetbrains.help.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.ObjectUtil;
import org.springframework.boot.system.ApplicationHome;
import org.springframework.core.io.ClassPathResource;

import java.io.File;

public interface FileTools {

    ApplicationHome application = new ApplicationHome();


    static boolean fileExists(String path) {
        return getFile(path).exists();
    }

    static boolean fileNotExists(String path) {
        return !fileExists(path);
    }

    static File getFile(String path) {
        File homeDir = application.getDir();
        File source = application.getSource();
        ClassPathResource classPathResource = new ClassPathResource(path);
        return ObjectUtil.isNull(source) ? FileUtil.file(classPathResource.getPath()) : FileUtil.file(homeDir, path);
    }

    static File getFileOrCreat(String path) {
        File file = getFile(path);
        if (ObjectUtil.isNotNull(application.getSource())) {
            ClassPathResource classPathResource = new ClassPathResource(path);
            File classPathFile = FileUtil.file(classPathResource.getPath());
            if (classPathResource.exists() && !file.exists()) {
                try {
                    FileUtil.writeFromStream(classPathResource.getInputStream(), classPathFile);
                } catch (Exception e) {
                    throw new IllegalArgumentException(CharSequenceUtil.format("{} File read failed", classPathFile.getPath()), e);
                }
                FileUtil.copy(classPathFile, file, true);
            }
        }
        return file;

    }
}
