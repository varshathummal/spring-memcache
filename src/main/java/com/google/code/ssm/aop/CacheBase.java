/*
 * Copyright (c) 2008-2017 Nelson Carpentier, Jakub Białek
 * 
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the
 * Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE
 * WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 * 
 */

package com.google.code.ssm.aop;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.security.InvalidParameterException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.google.code.ssm.Cache;
import com.google.code.ssm.PrefixedCacheImpl;
import com.google.code.ssm.Settings;
import com.google.code.ssm.aop.support.AnnotationData;
import com.google.code.ssm.aop.support.BridgeMethodMappingStore;
import com.google.code.ssm.aop.support.BridgeMethodMappingStoreImpl;
import com.google.code.ssm.aop.support.CacheKeyBuilder;
import com.google.code.ssm.aop.support.CacheKeyBuilderImpl;
import com.google.code.ssm.aop.support.InvalidAnnotationException;
import com.google.code.ssm.aop.support.PertinentNegativeNull;
import com.google.code.ssm.api.format.Serialization;
import com.google.code.ssm.api.format.SerializationType;
import com.google.code.ssm.util.Utils;

/**
 * 
 * @author Nelson Carpentier
 * @author Jakub Białek
 * 
 */
public class CacheBase implements ApplicationContextAware, InitializingBean {

    public static final String DISABLE_CACHE_PROPERTY = "ssm.cache.disable";

    private static final Logger LOG = LoggerFactory.getLogger(CacheBase.class);

    private CacheKeyBuilder cacheKeyBuilder = new CacheKeyBuilderImpl();

    private BridgeMethodMappingStore bridgeMethodMappingStore = new BridgeMethodMappingStoreImpl();

    // mapping cache zone <-> cache
    private final Map<String, Cache> caches = new HashMap<String, Cache>();

    private Settings settings = new Settings();

    private ApplicationContext context;

    @Override
    public void afterPropertiesSet() throws Exception {
    	try {
            settings = context.getBean(Settings.class);
        } catch (NoSuchBeanDefinitionException ex) {
            LOG.info("Cannot obtain custom SSM settings, default is used");
        }
    	
        for (Cache cache : context.getBeansOfType(Cache.class).values()) {
            addCache(cache);
        }        
    }

    @Override
    public void setApplicationContext(final ApplicationContext applicationContext) {
        this.context = applicationContext;
    }

    public void setCacheKeyBuilder(final CacheKeyBuilder cacheKeyBuilder) {
        this.cacheKeyBuilder = cacheKeyBuilder;
    }

    public CacheKeyBuilder getCacheKeyBuilder() {
        return this.cacheKeyBuilder;
    }

    public BridgeMethodMappingStore getBridgeMethodMappingStore() {
        return bridgeMethodMappingStore;
    }

    public void setBridgeMethodMappingStore(final BridgeMethodMappingStore bridgeMethodMappingStore) {
        this.bridgeMethodMappingStore = bridgeMethodMappingStore;
    }

    public Cache getCache(final AnnotationData data) {
        Cache cache = caches.get(data.getCacheName());
        if (cache == null) {
            throw new UndefinedCacheException(data.getCacheName());
        }

        if (cache.getProperties().isUseNameAsKeyPrefix()) {
            return new PrefixedCacheImpl(cache, data.getCacheName(), cache.getProperties().getKeyPrefixSeparator());
        }

        return cache;
    }

    public boolean isCacheDisabled() {
        String disableProperty = System.getProperty(DISABLE_CACHE_PROPERTY);
        return Boolean.toString(true).equals(disableProperty) || !Boolean.toString(false).equals(disableProperty)
                && settings.isDisableCache();
    }
    
    public <T extends Annotation> Method getMethodToCache(final JoinPoint jp, final Class<T> annotationClass) throws NoSuchMethodException {
        final Signature sig = jp.getSignature();
        if (!(sig instanceof MethodSignature)) {
            throw new InvalidAnnotationException("This annotation is only valid on a method.");
        }

        final MethodSignature msig = (MethodSignature) sig;
        final Object target = jp.getTarget();

        // cannot use msig.getMethod() because it can return the method where annotation was declared i.e. method in
        // interface
        String name = msig.getName();
        Class<?>[] parameters = msig.getParameterTypes();

        Method method = findMethodFromTargetGivenNameAndParams(target, name, parameters);

        if (method.isBridge()) {
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Method is bridge. Name {}, params: {}", name, parameters);
            }

            parameters = bridgeMethodMappingStore.getTargetParamsTypes(target.getClass(), name, parameters);
            method = findMethodFromTargetGivenNameAndParams(target, name, parameters);
        }

        if (method.getAnnotation(annotationClass) == null && settings.isEnableAnnotationsInInterface()) {
            // look for annotation on implemented interfaces
             method = Arrays.stream(jp.getTarget().getClass().getInterfaces())
            .map(i -> {
                try {
                    return i.getMethod(msig.getName(), msig.getParameterTypes());
                } catch (NoSuchMethodException ex) {
                    // return null as this interface doesn't contain interrupted method
                    return null;
                } catch (Exception ex) {
                    getLogger().debug("Problem getting method from interface {}", i.getName(), ex);
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .filter(m -> m.getAnnotation(annotationClass) != null)
            .findFirst()
            .orElse(null);
            
        }

        return method;
    }

    @SuppressWarnings("unchecked")
    public <T> T getUpdateData(final AnnotationData data, final Method method, final Object[] args, final Object returnValue)
            throws Exception {
        return data.isReturnDataIndex() ? (T) returnValue : (T) Utils.getMethodArg(data.getDataIndex(), args, method.toString());
    }

    protected Settings getSettings() {
        return settings;
    }

    protected Object getSubmission(final Object o) {
        return (o == null) ? PertinentNegativeNull.NULL : o;
    }

    protected Object getResult(final Object result) {
        return (result instanceof PertinentNegativeNull) ? null : result;
    }

    protected void verifyReturnTypeIsList(final Method method, final Class<?> annotationClass) {
        if (!verifyTypeIsList(method.getReturnType())) {
            throw new InvalidAnnotationException(
                    String.format("The annotation [%s] is only valid on a method that returns a [%s] or its subclass. "
                            + "[%s] does not fulfill this requirement.", annotationClass.getName(), List.class.getName(), method.toString()));
        }
    }

    protected boolean verifyTypeIsList(final Class<?> clazz) {
        return List.class.isAssignableFrom(clazz);
    }

    protected void verifyReturnTypeIsNoVoid(final Method method, final Class<?> annotationClass) {
        if (method.getReturnType().equals(void.class)) {
            throw new InvalidParameterException(String.format("Annotation [%s] is defined on void method  [%s]", annotationClass,
                    method.getName()));
        }
    }

    protected SerializationType getSerializationType(final Method method) {
        Serialization serialization = method.getAnnotation(Serialization.class);
        if (serialization != null) {
            return serialization.value();
        }

        serialization = method.getDeclaringClass().getAnnotation(Serialization.class);
        if (serialization != null) {
            return serialization.value();
        }

        return null;
    }

    protected Logger getLogger() {
        return LOG;
    }

    protected void addCache(final Cache cache) {
        if (cache == null) {
            getLogger().warn("One of the cache is null");
            return;
        }

        if (caches.put(cache.getName(), cache) != null) {
            String errorMsg = "There are two or more caches with the same name '" + cache.getName() + "'";
            getLogger().error(errorMsg);
            throw new IllegalStateException(errorMsg);
        }

        for (String alias : cache.getAliases()) {
            if (caches.containsKey(alias)) {
                String errorMsg = String.format("The cache with name '%s' uses alias '%s' already defined by cache with name '%s'",
                        cache.getName(), alias, caches.get(alias).getName());
                getLogger().error(errorMsg);
                throw new IllegalStateException(errorMsg);
            } else {
                caches.put(alias, cache);
            }
        }
    }

    private Method findMethodFromTargetGivenNameAndParams(final Object target, final String name, final Class<?>[] parameters)
            throws NoSuchMethodException {
        Method method = target.getClass().getMethod(name, parameters);
        getLogger().debug("Method to cache: {}", method);

        return method;
    }

}
