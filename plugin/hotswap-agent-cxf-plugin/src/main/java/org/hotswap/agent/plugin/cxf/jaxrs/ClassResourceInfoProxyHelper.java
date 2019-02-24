/*
 * Copyright 2013-2019 the HotswapAgent authors.
 *
 * This file is part of HotswapAgent.
 *
 * HotswapAgent is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 2 of the License, or (at your
 * option) any later version.
 *
 * HotswapAgent is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with HotswapAgent. If not, see http://www.gnu.org/licenses/.
 */
package org.hotswap.agent.plugin.cxf.jaxrs;

import java.lang.reflect.Method;

import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.utils.ResourceUtils;
import org.hotswap.agent.javassist.util.proxy.MethodFilter;
import org.hotswap.agent.javassist.util.proxy.MethodHandler;
import org.hotswap.agent.javassist.util.proxy.Proxy;
import org.hotswap.agent.javassist.util.proxy.ProxyFactory;
import org.hotswap.agent.javassist.util.proxy.ProxyObject;
import org.hotswap.agent.logging.AgentLogger;
import org.hotswap.agent.util.ReflectionHelper;

/**
 * The Class ClassResourceInfoProxyHelper.
 */
public class ClassResourceInfoProxyHelper {

    private static AgentLogger LOGGER = AgentLogger.getLogger(ClassResourceInfoProxyHelper.class);

    private static Class<?> classResourceInfoProxyClass = null;

    private static final ThreadLocal<Boolean> DISABLE_PROXY_GENERATION = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return false;
        }
    };

    private static synchronized void createProxyClass(ClassResourceInfo cri) {
        if (classResourceInfoProxyClass == null) {
            ProxyFactory f = new ProxyFactory();
            f.setSuperclass(cri.getClass());
            f.setFilter(new MethodFilter() {
                public boolean isHandled(Method m) {
                    return !m.getName().equals("finalize");
                }
            });
            classResourceInfoProxyClass = f.createClass();
        }
    }

    /**
     * Creates the class resource info proxy
     *
     * @param classResourceInfo the class resource info
     * @param generatorParams the generator params
     * @return the class resource info
     */
    public static ClassResourceInfo createProxy(ClassResourceInfo classResourceInfo, Class<?> generatorTypes[], Object generatorParams[]) {

        if (!DISABLE_PROXY_GENERATION.get()) {
            try {
                createProxyClass(classResourceInfo);
                ClassResourceInfo result = (ClassResourceInfo) classResourceInfoProxyClass.newInstance();
                CriProxyMethodHandler methodHandler = new CriProxyMethodHandler(result, generatorTypes, generatorParams);
                ((Proxy)result).setHandler(methodHandler);
                methodHandler.delegate = classResourceInfo;
                methodHandler.generatorTypes = generatorTypes;
                methodHandler.generatorParams = generatorParams;
                return result;
            } catch (Exception e) {
                LOGGER.error("Unable to create ClassResourceInfo proxy for {}", e, classResourceInfo);
            }
        }

        return classResourceInfo;
    }

    public static void reloadClassResourceInfo(ClassResourceInfo classResourceInfoProxy) {
        try {
            DISABLE_PROXY_GENERATION.set(true);
            CriProxyMethodHandler criMethodHandler = (CriProxyMethodHandler) ((ProxyObject)classResourceInfoProxy).getHandler();
            ClassResourceInfo newClassResourceInfo = (ClassResourceInfo) ReflectionHelper.invoke(null, ResourceUtils.class, "createClassResourceInfo",
                    criMethodHandler.generatorTypes, criMethodHandler.generatorParams);
            ClassResourceInfo oldClassResourceInfo = criMethodHandler.delegate;
            newClassResourceInfo.setResourceProvider(oldClassResourceInfo.getResourceProvider());
            criMethodHandler.delegate = newClassResourceInfo;
        } catch (Exception e) {
            LOGGER.error("reloadClassResourceInfo() exception {}", e.getMessage());
        } finally {
            DISABLE_PROXY_GENERATION.remove();
        }
    }

    public static class CriProxyMethodHandler implements MethodHandler {

        ClassResourceInfo delegate;
        Object[] generatorParams;
        Class<?>[] generatorTypes;

        public CriProxyMethodHandler(ClassResourceInfo delegate, Class<?> generatorTypes[], Object[] generatorParams) {
            this.generatorTypes = generatorTypes; }

        public Object invoke(Object self, Method method, Method proceed, Object[] args) throws Throwable {
            // simple delegate to delegate object
            return method.invoke(delegate, args);
        }
    }
}
