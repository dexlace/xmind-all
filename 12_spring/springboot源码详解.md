# springboot源码详解

## 一、自定义系统初始化器

`ApplicationContextInitializer`

sprin容器刷新之前执行的一个回调函数

向springboot容器中注册属性

### 1.1 方法一:使用spring.factories

```java
/**
 * 系统初始化器
 */
@Order(1)
public class FirstInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
//        environment.setRequiredProperties("3sss");
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        // 设置了环境变量
        MapPropertySource mapPropertySource = new MapPropertySource("firstInitializer", map);
        environment.getPropertySources().addLast(mapPropertySource);
        System.out.println("run firstInitializer");
    }
}
```

META-INF下的spring.factories

```properties
org.springframework.context.ApplicationContextInitializer=com.dexlace.boot.initializer.FirstInitializer
```

以上完成注册

使用如下

```java
/**
 * 可以拿到applicaitoncontext
 */
@Component
public class TestService implements ApplicationContextAware {


    private ApplicationContext applicationContext;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public String test() {
        return applicationContext.getEnvironment().getProperty("key1");
    }

}
```

```java
    
@Controller
@RequestMapping("/demo")
public class DemoController {
   @Autowired
    private TestService testService;
   @RequestMapping("test")
    @ResponseBody
    public String test() {
        return testService.test();
    }
}
```

### 1.2 方法二：使用SpringApplication添加初始化器

```java
@Order(2)
public class SecondInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        Map<String, Object> map = new HashMap<>();
        map.put("key2", "value2");
        MapPropertySource mapPropertySource = new MapPropertySource("secondInitializer", map);
        environment.getPropertySources().addLast(mapPropertySource);
        System.out.println("run secondInitializer");
    }
}
```

```java
@SpringBootApplication
public class MainApplication {

	public static void main(String[] args) {
		SpringApplication springApplication=new SpringApplication(MainApplication.class);
		springApplication.addInitializers(new SecondInitializer());
		springApplication.run(args);
//		SpringApplication.run(MainApplication.class, args);
	}

}
```

使用还是一样的

### 1.3 方法三：使用application.properties

```java
@Order(3)
public class ThirdInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        Map<String, Object> map = new HashMap<>();
        map.put("key3", "value3");
        MapPropertySource mapPropertySource = new MapPropertySource("thirdInitializer", map);
        environment.getPropertySources().addLast(mapPropertySource);
        System.out.println("run thirdInitializer");
    }
}
```

Application.properties

```java

context.initializer.classes=com.dexlace.boot.initializer.ThirdInitializer
```

### 1.4 系统初始化器的作用

- 上下文刷新即refresh方法前调用
- 用来编码设置一些属性变量
- 可以通过order进行排序



## 二、SpringFactoriesLoader介绍

框架内部使用的通用工厂加载机制

从classpath下的多个jar包特定位置读取文件并初始化类

文件内容必须是kv形式，即properties形式

key是全限定名、value是实现，多个用逗号分隔

<img src="/Users/dexlace/private-github-repository/xmind-all/12_spring/springboot源码详解.assets/截屏2022-10-22 22.08.01-6447729.png" alt="截屏2022-10-22 22.08.01" style="zoom: 25%;" />

<img src="/Users/dexlace/private-github-repository/xmind-all/12_spring/springboot源码详解.assets/image-20221022221024056.png" alt="image-20221022221024056" style="zoom: 25%;" />







```java


public abstract class AutoConfigurationPackages {
    private static final Log logger = LogFactory.getLog(AutoConfigurationPackages.class);
    // 拿到全限定名
    private static final String BEAN = AutoConfigurationPackages.class.getName();

    public AutoConfigurationPackages() {
    }

    public static boolean has(BeanFactory beanFactory) {
        // 存在bean
        return beanFactory.containsBean(BEAN) && !get(beanFactory).isEmpty();
    }

    public static List<String> get(BeanFactory beanFactory) {
        try {
            return ((AutoConfigurationPackages.BasePackages)beanFactory.getBean(BEAN, AutoConfigurationPackages.BasePackages.class)).get();
        } catch (NoSuchBeanDefinitionException var2) {
            throw new IllegalStateException("Unable to retrieve @EnableAutoConfiguration base packages");
        }
    }

    public static void register(BeanDefinitionRegistry registry, String... packageNames) {
        if (registry.containsBeanDefinition(BEAN)) {
            BeanDefinition beanDefinition = registry.getBeanDefinition(BEAN);
            ConstructorArgumentValues constructorArguments = beanDefinition.getConstructorArgumentValues();
            constructorArguments.addIndexedArgumentValue(0, addBasePackages(constructorArguments, packageNames));
        } else {
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(AutoConfigurationPackages.BasePackages.class);
            beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, packageNames);
            beanDefinition.setRole(2);
            registry.registerBeanDefinition(BEAN, beanDefinition);
        }

    }

    private static String[] addBasePackages(ConstructorArgumentValues constructorArguments, String[] packageNames) {
        String[] existing = (String[])((String[])constructorArguments.getIndexedArgumentValue(0, String[].class).getValue());
        Set<String> merged = new LinkedHashSet();
        merged.addAll(Arrays.asList(existing));
        merged.addAll(Arrays.asList(packageNames));
        return StringUtils.toStringArray(merged);
    }

    static final class BasePackages {
        private final List<String> packages;
        private boolean loggedBasePackageInfo;

        BasePackages(String... names) {
            List<String> packages = new ArrayList();
            String[] var3 = names;
            int var4 = names.length;

            for(int var5 = 0; var5 < var4; ++var5) {
                String name = var3[var5];
                if (StringUtils.hasText(name)) {
                    packages.add(name);
                }
            }

            this.packages = packages;
        }

      
        // get
        public List<String> get() {
            if (!this.loggedBasePackageInfo) {
                if (this.packages.isEmpty()) {
                    if (AutoConfigurationPackages.logger.isWarnEnabled()) {
                        AutoConfigurationPackages.logger.warn("@EnableAutoConfiguration was declared on a class in the default package. Automatic @Repository and @Entity scanning is not enabled.");
                    }
                } else if (AutoConfigurationPackages.logger.isDebugEnabled()) {
                    String packageNames = StringUtils.collectionToCommaDelimitedString(this.packages);
                    AutoConfigurationPackages.logger.debug("@EnableAutoConfiguration was declared on a class in the package '" + packageNames + "'. Automatic @Repository and @Entity scanning is enabled.");
                }

                this.loggedBasePackageInfo = true;
            }

            return this.packages;
        }
    }

    private static final class PackageImport {
        private final String packageName;

        PackageImport(AnnotationMetadata metadata) {
            this.packageName = ClassUtils.getPackageName(metadata.getClassName());
        }

        public String getPackageName() {
            return this.packageName;
        }

        public boolean equals(Object obj) {
            return obj != null && this.getClass() == obj.getClass() ? this.packageName.equals(((AutoConfigurationPackages.PackageImport)obj).packageName) : false;
        }

        public int hashCode() {
            return this.packageName.hashCode();
        }

        public String toString() {
            return "Package Import " + this.packageName;
        }
    }

  
  
    // 有一个内部类实现了
    static class Registrar implements ImportBeanDefinitionRegistrar, DeterminableImports {
        Registrar() {
        }

       // 注册beanDefinition
       // 由于AutoConfigurationPackage注解标注在主类上，得到了主类的包名然后把这个包名最终封装
       // (toArray(new String[0]))到我们的一个数组里面,然后给我们注册(register())进去,
       // 相当于我们这个Registrar就是把某一个包下的所有组件批量注册进容器
        public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
            AutoConfigurationPackages.register(registry, (new AutoConfigurationPackages.PackageImport(metadata)).getPackageName());
        }

        // 决定导入的
        public Set<Object> determineImports(AnnotationMetadata metadata) {
            return Collections.singleton(new AutoConfigurationPackages.PackageImport(metadata));
        }
    }
}

```







