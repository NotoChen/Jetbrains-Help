package com.jetbrains.help.util;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.CharSequenceUtil;
import org.springframework.core.io.ClassPathResource;

import java.io.File;
import java.io.IOException;

public interface FileTools {

    static boolean fileExists(String pathOrFile) {
        return FileUtil.file(new ClassPathResource(pathOrFile).getPath()).exists();
    }

    static File getFileOrCreat(String pathOrFile) {
        File file = FileUtil.file(new ClassPathResource(pathOrFile).getPath());
        if (!file.exists()) {
            try {
                File parentFile = file.getParentFile();
                if (!parentFile.exists() && !parentFile.mkdir()) {
                    throw new IllegalArgumentException(CharSequenceUtil.format("{} File directory create Failed", pathOrFile));
                }
                if (!file.createNewFile()) {
                    throw new IllegalArgumentException(CharSequenceUtil.format("{} File create failed", pathOrFile));
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(CharSequenceUtil.format("{} File create failed", pathOrFile), e);
            }
        }
        return file;
    }
}
