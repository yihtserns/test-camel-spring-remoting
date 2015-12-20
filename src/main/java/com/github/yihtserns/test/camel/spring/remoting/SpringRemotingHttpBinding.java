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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.camel.Body;
import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.http.common.DefaultHttpBinding;
import org.apache.camel.http.common.HttpMessage;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * @author yihtserns
 */
public class SpringRemotingHttpBinding extends DefaultHttpBinding {

    private int bodyParameterIndex = 0;

    protected SpringRemotingHttpBinding() {
    }

    @Override
    public void readRequest(HttpServletRequest request, HttpMessage message) {
        super.readRequest(request, message);

        unwrapRemoteInvocation(message);
    }

    void unwrapRemoteInvocation(Message message) {
        RemoteInvocation remoteInvocation = (RemoteInvocation) message.getBody();
        message.setBody(remoteInvocation.getArguments()[bodyParameterIndex]);
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
        Method method = serviceInterface.getMethods()[0];
        if (method.getParameterTypes().length == 1) {
            return new SpringRemotingHttpBinding();
        }

        if (!hasBodyAnnotation(method)) {
            String msg = String.format(
                    "One of the parameters of method '%s' must be annotated with @Body",
                    method);
            throw new IllegalArgumentException(msg);
        }

        SpringRemotingHttpBinding binding = new SpringRemotingHttpBinding();

        Annotation[][] nParamAnnotations = method.getParameterAnnotations();
        for (int parameterIndex = 0; parameterIndex < nParamAnnotations.length; parameterIndex++) {
            Annotation[] paramAnnotations = nParamAnnotations[parameterIndex];
            for (Annotation paramAnnotation : paramAnnotations) {
                if (paramAnnotation.annotationType() == Body.class) {
                    binding.bodyParameterIndex = parameterIndex;
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
}
