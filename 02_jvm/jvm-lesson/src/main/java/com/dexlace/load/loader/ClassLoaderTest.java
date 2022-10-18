package com.dexlace.load.loader;

public class ClassLoaderTest {
    public static void main(String[] args) {
        ClassLoader systemClassLoader=ClassLoader.getSystemClassLoader();
        System.out.println(systemClassLoader);

        ClassLoader extClassLoader=systemClassLoader.getParent();
        System.out.println(extClassLoader);

        // 获取不到 为null 获取不到引导类加载器
        ClassLoader bootStrapClassLoader = extClassLoader.getParent();
        System.out.println(bootStrapClassLoader);

        // 对于用户自定义类来说 打印出来  AppClassLoader
        ClassLoader classLoader = ClassLoaderTest.class.getClassLoader();
        System.out.println(classLoader);

        // java的核心类加载器都是用bootstrapclassloader加载的
        ClassLoader classLoader1 = String.class.getClassLoader();
        System.out.println(classLoader1);

    }
}
