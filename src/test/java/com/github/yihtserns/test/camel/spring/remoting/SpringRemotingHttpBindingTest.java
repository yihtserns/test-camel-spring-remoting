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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.apache.camel.Body;
import org.apache.camel.Header;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultMessage;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.remoting.support.RemoteInvocation;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * @author yihtserns
 */
public class SpringRemotingHttpBindingTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldThrowIfProvidedWithNonInterfaceClass() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Class must be an interface, but was class java.lang.Object");
        SpringRemotingHttpBinding.forServiceInterface(Object.class);
    }

    /**
     * The single param is by default the body.
     */
    @Test
    public void canCreateBindingWhenSingleParamIsNotAnnotated() {
        Class<OneParamService> interfaceClass = OneParamService.class;
        SpringRemotingHttpBinding binding = SpringRemotingHttpBinding.forServiceInterface(interfaceClass);

        final DefaultMessage message = new DefaultMessage();
        Object payload = "Expected Payload";

        OneParamService service = proxyOf(interfaceClass, ConvertMethodCallToRemoteObject.setAsBodyOf(message));
        service.service(payload);

        binding.unwrapRemoteInvocation(message);
        assertThat(message.getBody(), is(sameInstance(payload)));

        binding.wrapInRemoteInvocationResult(message);
        assertThat(((RemoteInvocationResult) message.getBody()).getValue(), is(payload));
    }

    @Test
    public void shouldThrowWhenMoreThanOneParamButNoneAnnotatedAsBody() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("One of the parameters of method 'public abstract java.lang.Object com.github.yihtserns.test.camel.spring.remoting.SpringRemotingHttpBindingTest$UnannotatedTwoParamService.service(java.lang.Object,java.lang.Object)' must be annotated with @Body");
        SpringRemotingHttpBinding.forServiceInterface(UnannotatedTwoParamService.class);
    }

    @Test
    public void canCreateBindingWhenOneOfParamsIsAnnotatedAsBody() throws Exception {
        Class<OneAnnotatedMultiParamService> interfaceClass = OneAnnotatedMultiParamService.class;
        SpringRemotingHttpBinding binding = SpringRemotingHttpBinding.forServiceInterface(interfaceClass);

        final DefaultMessage message = new DefaultMessage();
        Object payload = "Expected Payload";

        OneAnnotatedMultiParamService service = proxyOf(interfaceClass, ConvertMethodCallToRemoteObject.setAsBodyOf(message));
        service.service("Not Payload 1", payload, "Not Payload 2");

        binding.unwrapRemoteInvocation(message);
        assertThat(message.getBody(), is(payload));
    }

    @Test
    public void shouldThrowWhenMoreThanOneParamIsAnnotatedAsBody() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("Only one of the parameters of method 'public abstract java.lang.Object com.github.yihtserns.test.camel.spring.remoting.SpringRemotingHttpBindingTest$MultiAnnotatedMultiParamService.service(java.lang.Object,java.lang.Object,java.lang.Object)' can be annotated with @Body");
        SpringRemotingHttpBinding.forServiceInterface(MultiAnnotatedMultiParamService.class);
    }

    @Test
    public void canHandleServiceInterfaceWithMultipleMethods() throws Exception {
        Class<MultiMethodService> interfaceClass = MultiMethodService.class;
        SpringRemotingHttpBinding binding = SpringRemotingHttpBinding.forServiceInterface(interfaceClass);
        Object payload = "Expected Payload";

        {
            DefaultMessage message = new DefaultMessage();

            MultiMethodService service = proxyOf(interfaceClass, ConvertMethodCallToRemoteObject.setAsBodyOf(message));
            service.service(payload);

            binding.unwrapRemoteInvocation(message);
            assertThat(message.getBody(), is(payload));
        }
        {
            DefaultMessage message = new DefaultMessage();

            MultiMethodService service = proxyOf(interfaceClass, ConvertMethodCallToRemoteObject.setAsBodyOf(message));
            service.service("Not Payload 1", payload);

            binding.unwrapRemoteInvocation(message);
            assertThat(message.getBody(), is(payload));
        }
        {
            DefaultMessage message = new DefaultMessage();

            MultiMethodService service = proxyOf(interfaceClass, ConvertMethodCallToRemoteObject.setAsBodyOf(message));
            service.service("Not Payload 1", "Not Payload 2", payload);

            binding.unwrapRemoteInvocation(message);
            assertThat(message.getBody(), is(payload));
        }
    }

    @Test
    public void canExtractHeader() throws Exception {
        Class<WithHeaderService> interfaceClass = WithHeaderService.class;
        SpringRemotingHttpBinding binding = SpringRemotingHttpBinding.forServiceInterface(interfaceClass);

        DefaultMessage message = new DefaultMessage();
        WithHeaderService service = proxyOf(interfaceClass, ConvertMethodCallToRemoteObject.setAsBodyOf(message));

        Object body = "Expected Body";
        Object header = "Expected Header";
        service.service(body, header);

        binding.unwrapRemoteInvocation(message);
        assertThat(message.getBody(), is(body));
        assertThat(message.getHeader("header"), is(header));
    }

    @Test
    public void canHandleWhenSameParamAnnotatedAsBothBodyAndHeader() throws Exception {
        Class<SameHeaderBodyService> interfaceClass = SameHeaderBodyService.class;
        SpringRemotingHttpBinding binding = SpringRemotingHttpBinding.forServiceInterface(interfaceClass);

        DefaultMessage message = new DefaultMessage();
        SameHeaderBodyService service = proxyOf(interfaceClass, ConvertMethodCallToRemoteObject.setAsBodyOf(message));

        Object payload = "Expected Payload";
        service.service(payload, "Unused");

        binding.unwrapRemoteInvocation(message);
        assertThat(message.getBody(), is(payload));
        assertThat(message.getHeader("payload"), is(payload));
    }

    @Test
    public void canExtractMultipleHeaders() throws Exception {
        Class<MultiHeaderService> interfaceClass = MultiHeaderService.class;
        SpringRemotingHttpBinding binding = SpringRemotingHttpBinding.forServiceInterface(interfaceClass);

        DefaultMessage message = new DefaultMessage();
        MultiHeaderService service = proxyOf(interfaceClass, ConvertMethodCallToRemoteObject.setAsBodyOf(message));

        Object timeout = 1000;
        Object id = "Expected ID";
        Object payload = "Expected Payload";
        service.service(timeout, id, payload);

        binding.unwrapRemoteInvocation(message);
        assertThat(message.getBody(), is(payload));
        assertThat(message.getHeader("timeout"), is(timeout));
        assertThat(message.getHeader("id"), is(id));
    }

    /**
     * Not an intentional feature - just documenting a discovery.
     */
    @Test
    public void canAcceptCallWithDifferentServiceInterfaceButSameMethodSignature() throws Exception {
        SpringRemotingHttpBinding binding = SpringRemotingHttpBinding.forServiceInterface(OneParamService.class);

        DefaultMessage message = new DefaultMessage();
        MultiMethodService service = proxyOf(MultiMethodService.class, ConvertMethodCallToRemoteObject.setAsBodyOf(message));

        Object payload = "Expected Payload";
        service.service(payload);

        binding.unwrapRemoteInvocation(message);
        assertThat(message.getBody(), is(payload));
    }

    private static abstract class ConvertMethodCallToRemoteObject implements InvocationHandler {

        @Override
        public Object invoke(Object o, Method method, Object[] args) throws Throwable {
            RemoteInvocation remoteInvocation = new RemoteInvocation(method.getName(), method.getParameterTypes(), args);
            handle(remoteInvocation);

            return null;
        }

        protected abstract void handle(RemoteInvocation remoteInvocation);

        public static ConvertMethodCallToRemoteObject setAsBodyOf(final Message message) {
            return new ConvertMethodCallToRemoteObject() {

                @Override
                protected void handle(RemoteInvocation remoteInvocation) {
                    message.setBody(remoteInvocation);
                }
            };
        }
    }

    private static <T> T proxyOf(Class<T> serviceInterface, InvocationHandler invocationHandler) {
        return serviceInterface.cast(Proxy.newProxyInstance(
                SpringRemotingHttpBindingTest.class.getClassLoader(),
                new Class[]{serviceInterface},
                invocationHandler));
    }

    interface OneParamService {

        Object service(Object param);
    }

    interface UnannotatedTwoParamService {

        Object service(Object param1, Object param2);
    }

    interface OneAnnotatedMultiParamService {

        Object service(Object param1, @Body Object param2, Object param3);
    }

    interface MultiAnnotatedMultiParamService {

        Object service(Object param1, @Body Object param2, @Body Object param3);
    }

    interface MultiMethodService {

        Object service(Object param);

        Object service(Object param1, @Body Object param2);

        Object service(Object param1, Object param2, @Body Object param3);
    }

    interface WithHeaderService {

        Object service(@Body Object body, @Header("header") Object header);
    }

    interface SameHeaderBodyService {

        Object service(@Body @Header("payload") Object payload, Object unused);
    }

    interface MultiHeaderService {

        Object service(@Header("timeout") Object timeout, @Header("id") Object id, @Body Object payload);
    }
}
