package com.spring;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 自定义的spring上下文容器
 *
 * @author 张贝易
 */
public class ZbyApplicationContext {
    private static final String CLASS_SUFFIX = ".class";
    private String contextClassPath;
    private List<String> classNames;
    /**
     * 单例池
     */
    private ConcurrentHashMap<String, Object> singletonBeanPool = new ConcurrentHashMap<>();
    /**
     * bean定义池
     */
    private ConcurrentHashMap<String, BeanDefinition> definitionPool = new ConcurrentHashMap<>();

    public ZbyApplicationContext(Class configClass) {
        init();
        resolveClasses(configClass);
        loadClasses(this.classNames);
    }

    public Object getBean(String beanName) {
        return createBean(beanName);
    }

    /**
     * 根据beanName获取beanDefinition创建一个bean
     * 根据单例还是原型模式选择创建模式
     * 暂时不支持type，还是使用class
     *
     * @param beanName bean的名称
     * @return bean对象
     */
    private Object createBean(String beanName) {
        BeanDefinition beanDefinition = definitionPool.get(beanName);
        Object bean = null;
        Type type = beanDefinition.getType();
        try {
            if (beanDefinition.isPrototype()) {
                bean = ((Class) type).newInstance();
                singletonBeanPool.put(beanName, bean);
            } else {
                bean = singletonBeanPool.get(beanName);
                if (bean == null) {
                    bean = ((Class) type).newInstance();
                    singletonBeanPool.put(beanName, bean);
                }
            }
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return bean;
    }

    /**
     * 初始化。
     * 获取ClassPath信息、获取ClassLoader
     */
    private void init() {
        this.contextClassPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        try {
            this.contextClassPath = URLDecoder.decode(this.contextClassPath, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取配置中ComponentScan对应包下所有类的全类名
     *
     * @param configClass 配置类
     */
    private void resolveClasses(Class configClass) {
        ComponentScan componentScan = (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        String packageName = componentScan.value();
        String packagePath = packageName.replace('.', '/');
        packagePath = this.contextClassPath + packagePath;
        this.classNames = doResolveClasses(packagePath);
    }

    /**
     * 将某一个包下的所有类的全类名提取出来
     *
     * @param packagePath 包的绝对路径
     * @return 包下的所有类的全类名
     */
    private List<String> doResolveClasses(String packagePath) {
        List<String> classNames = new LinkedList<>();
        File file = new File(packagePath);
        File[] subFiles = file.listFiles();
        if (subFiles != null) {
            for (File subFile : subFiles) {
                String subFilePath = subFile.getPath();
                if (subFilePath.endsWith(CLASS_SUFFIX)) {
                    String className = classPathToName(subFilePath);
                    classNames.add(className);
                } else {
                    classNames.addAll(doResolveClasses(subFile.getPath()));
                }
            }
        }
        return classNames;
    }

    /**
     * 将类的绝对路径转换为全类名
     *
     * @param classAbsolutePath 类的绝对路径
     * @return 类的全类名
     */
    private String classPathToName(String classAbsolutePath) {
        String className = classAbsolutePath.substring(this.contextClassPath.length() - 1,
                classAbsolutePath.length() - CLASS_SUFFIX.length());
        className = className.replace('/', '.');
        className = className.replace('\\', '.');
        return className;
    }

    /**
     * 完成类加载
     *
     * @param classNames 全类名的链表
     */
    private void loadClasses(List<String> classNames) {
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                Component component = clazz.getDeclaredAnnotation(Component.class);
                Scope scope = clazz.getDeclaredAnnotation(Scope.class);
                if (component != null) {
                    String beanName = component.value();
                    SimpleBeanDefinition beanDefinition = new SimpleBeanDefinition();
                    beanDefinition.setType(clazz).setScope(scope == null ? null : scope.value());
                    definitionPool.put(beanName, beanDefinition);
                }
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
