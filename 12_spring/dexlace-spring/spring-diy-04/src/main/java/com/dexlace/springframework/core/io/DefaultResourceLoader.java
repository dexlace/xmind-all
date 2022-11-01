package com.dexlace.springframework.core.io;

import cn.hutool.core.lang.Assert;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * 默认的资源加载器实现
 */
public class DefaultResourceLoader implements ResourceLoader {

    @Override
    public Resource getResource(String location) {
        Assert.notNull(location, "Location must not be null");
        // 判断是不是classpath的加载方式
        if (location.startsWith(CLASSPATH_URL_PREFIX)) {
            return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()));
        }
        else {
            try {
                // 是否是url
                URL url = new URL(location);
                return new UrlResource(url);
            } catch (MalformedURLException e) {
                // 文件
                return new FileSystemResource(location);
            }
        }
    }

}
