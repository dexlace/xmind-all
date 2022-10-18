package com.dexlace.load.time;


/**
 * 非主动使用类字段
 * 注意配置jvm选项  -XX： +TraceClassLoading参
 */
public class NotInitialization {

    public static void main(String[] args) {
//        Super Class init
//        123
        // 以上运行结果表明通过子类来引用父类中定义的静态字段  只会触发父类的初始化而不会触发子类的初始化
        // 例子一：通过子类引用父类的静态字段，不会导致子类初始化
        // 但是会触发子类的加载和验证阶段
//        System.out.println(SubClass.value);


        // 例子二：通过数组定义来引用类，不会触发此类的初始化，所以不会打印该类的静态方法
//        SuperClass [] sca=new SuperClass[10];

        // 例子三：同样没有输出const class init
//        是因为虽然在Java源码中确实引用了
//        ConstClass类的常量HELLOWORLD，但其实在编译阶段通过常量传播优化，
//        已经将此常量的值“hello world”直接存储在NotInitialization类的常量池中，
//        以后NotInitialization对常量 ConstClass.HELLOWORLD的引用，
//        实际都被转化为NotInitialization类对自身常量池的引用了
        System.out.println(ConstClass.HELLOWORLD);
    }
}
