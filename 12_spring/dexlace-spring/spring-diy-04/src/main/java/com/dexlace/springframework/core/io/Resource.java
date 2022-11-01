package com.dexlace.springframework.core.io;

import java.io.IOException;
import java.io.InputStream;

/**
 * 资源加载处理流
 */
public interface Resource {

    InputStream getInputStream() throws IOException;

}
