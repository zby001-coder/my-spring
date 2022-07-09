package com.spring;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
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
     * 单例bean创建时使用的锁，通过synchronize一个公共对象来实现加锁
     */
    private final Object createSingletonBeanLock = new Object();
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
        try {
            if (beanDefinition.isPrototype()) {
                bean = createPrototypeBean(beanName);
            } else {
                bean = createSingletonBean(beanName);
            }
            injectFields(bean, beanDefinition);
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return bean;
    }

    /**
     * 注入bean的字段，通过反射实现
     *
     * @param o
     * @param beanDefinition
     * @throws IllegalAccessException
     */
    public void injectFields(Object o, BeanDefinition beanDefinition) throws IllegalAccessException {
        Class clazz = (Class) beanDefinition.getType();
        Field[] fields = clazz.getDeclaredFields();
        //变量bean的字段
        for (Field field : fields) {
            field.setAccessible(true);
            //如果是autowired，就注入这个bean
            if (field.isAnnotationPresent(Autowired.class)) {
                String name = field.getName();
                //调用createBean，如果是单例就会返回map中的值，如果是原型就会返回新建的值
                Object bean = createBean(name);
                field.set(o, bean);
            }
        }
    }

    /**
     * 创建单例bean，使用双重锁+synchronize来做线程同步
     *
     * @param beanName
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private Object createSingletonBean(String beanName) throws InstantiationException, IllegalAccessException {
        if (singletonBeanPool.get(beanName) != null) {
            return singletonBeanPool.get(beanName);
        } else {
            synchronized (createSingletonBeanLock) {
                if (singletonBeanPool.get(beanName) == null) {
                    BeanDefinition beanDefinition = definitionPool.get(beanName);
                    Type type = beanDefinition.getType();
                    Object bean = ((Class) type).newInstance();
                    singletonBeanPool.put(beanName, bean);
                    return bean;
                } else {
                    return singletonBeanPool.get(beanName);
                }
            }
        }
    }

    /**
     * 创建原型bean
     *
     * @param beanName
     * @return
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    private Object createPrototypeBean(String beanName) throws InstantiationException, IllegalAccessException {
        BeanDefinition beanDefinition = definitionPool.get(beanName);
        Type type = beanDefinition.getType();
        Object bean = ((Class) type).newInstance();
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
