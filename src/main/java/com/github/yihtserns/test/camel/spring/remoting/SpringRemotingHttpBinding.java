/*
 * Copyright 2015 yihtserns.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.yihtserns.test.camel.spring.remoting;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Header;
import org.apache.camel.Message;
import org.apache.camel.http.common.DefaultHttpBinding;
import org.apache.camel.http.common.HttpMessage;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * @author yihtserns
 */
public class SpringRemotingHttpBinding extends DefaultHttpBinding {

    private Map<MethodInvocation, MethodInvocation> methodInvocations = new HashMap<MethodInvocation, MethodInvocation>();

    protected SpringRemotingHttpBinding() {
    }

    @Override
    public void readRequest(HttpServletRequest request, HttpMessage message) {
        super.readRequest(request, message);

        unwrapRemoteInvocation(message);
    }

    void unwrapRemoteInvocation(Message message) {
        RemoteInvocation remoteInvocation = (RemoteInvocation) message.getBody();
        MethodInvocation methodInvocation = methodInvocations.get(MethodInvocation.from(remoteInvocation));
        Object[] arguments = remoteInvocation.getArguments();

        message.setBody(methodInvocation.getBody(arguments));
        message.getHeaders().putAll(methodInvocation.getHeaders(arguments));
    }

    @Override
    public void doWriteResponse(Message message, HttpServletResponse response, Exchange exchange) throws IOException {
        wrapInRemoteInvocationResult(message);

        super.doWriteResponse(message, response, exchange);
    }

    void wrapInRemoteInvocationResult(Message message) {
        message.setBody(new RemoteInvocationResult(message.getBody()));
    }

    public static SpringRemotingHttpBinding forServiceInterface(Class<?> serviceInterface) {
        if (!serviceInterface.isInterface()) {
            throw new IllegalArgumentException("Class must be an interface, but was " + serviceInterface);
        }

        SpringRemotingHttpBinding binding = new SpringRemotingHttpBinding();
        for (Method method : serviceInterface.getMethods()) {
            MethodInvocation methodInvocation = MethodInvocation.from(method);
            binding.methodInvocations.put(methodInvocation, methodInvocation);

            if (method.getParameterTypes().length == 1) {
                continue;
            }

            if (!hasBodyAnnotation(method)) {
                String msg = String.format(
                        "One of the parameters of method '%s' must be annotated with @Body",
                        method);
                throw new IllegalArgumentException(msg);
            }

            Annotation[][] nParamAnnotations = method.getParameterAnnotations();
            for (int parameterIndex = 0; parameterIndex < nParamAnnotations.length; parameterIndex++) {
                Annotation[] paramAnnotations = nParamAnnotations[parameterIndex];
                for (Annotation paramAnnotation : paramAnnotations) {
                    Class<? extends Annotation> annotationType = paramAnnotation.annotationType();
                    if (annotationType == Body.class) {
                        methodInvocation.bodyParameterIndex = parameterIndex;
                        continue;
                    }
                    if (annotationType == Header.class) {
                        String headerName = Header.class.cast(paramAnnotation).value();
                        methodInvocation.addHeaderIndex(parameterIndex, headerName);
                    }
                }
            }
        }

        return binding;
    }

    private static boolean hasBodyAnnotation(Method method) {
        boolean foundBody = false;
        for (Annotation[] paramAnnotations : method.getParameterAnnotations()) {
            for (Annotation paramAnnotation : paramAnnotations) {
                if (paramAnnotation.annotationType() != Body.class) {
                    continue;
                }
                if (foundBody) {
                    String msg = String.format(
                            "Only one of the parameters of method '%s' can be annotated with @Body",
                            method);
                    throw new IllegalArgumentException(msg);
                }
                foundBody = true;
            }
        }
        return foundBody;
    }

    private static final class MethodInvocation {

        public int bodyParameterIndex = 0;
        private Map<Integer, String> parameterIndex2HeaderName = new HashMap<Integer, String>();
        private String methodName;
        private Class<?>[] parameterTypes;

        private MethodInvocation(String methodName, Class<?>[] parameterTypes) {
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        public void addHeaderIndex(int parameterIndex, String headerName) {
            parameterIndex2HeaderName.put(parameterIndex, headerName);
        }

        public Object getBody(Object[] arguments) {
            return arguments[bodyParameterIndex];
        }

        public Map<String, Object> getHeaders(Object[] arguments) {
            Map<String, Object> headers = new HashMap<String, Object>();

            for (Entry<Integer, String> entry : parameterIndex2HeaderName.entrySet()) {
                Integer parameterIndex = entry.getKey();
                String headerName = entry.getValue();
                headers.put(headerName, arguments[parameterIndex]);
            }

            return headers;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 79 * hash + (this.methodName != null ? this.methodName.hashCode() : 0);
            hash = 79 * hash + Arrays.deepHashCode(this.parameterTypes);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final MethodInvocation other = (MethodInvocation) obj;
            if ((this.methodName == null) ? (other.methodName != null) : !this.methodName.equals(other.methodName)) {
                return false;
            }
            return Arrays.deepEquals(this.parameterTypes, other.parameterTypes);
        }

        public static MethodInvocation from(Method method) {
            return new MethodInvocation(method.getName(), method.getParameterTypes());
        }

        public static MethodInvocation from(RemoteInvocation remoteInvocation) {
            return new MethodInvocation(remoteInvocation.getMethodName(), remoteInvocation.getParameterTypes());
        }
    }
}
