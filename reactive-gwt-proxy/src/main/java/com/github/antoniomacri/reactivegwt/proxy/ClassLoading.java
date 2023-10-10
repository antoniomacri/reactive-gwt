package com.github.antoniomacri.reactivegwt.proxy;

public class ClassLoading {
    public static Class<?> loadClass(String name) throws ClassNotFoundException {
        try {
            return Thread.currentThread().getContextClassLoader().loadClass(name);
        } catch (ClassNotFoundException e) {
            return Class.forName(name);
        }
    }
}
