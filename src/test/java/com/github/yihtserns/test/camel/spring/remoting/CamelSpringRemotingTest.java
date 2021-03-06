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

import com.github.yihtserns.test.camel.spring.remoting.testutil.Service;
import com.github.yihtserns.test.camel.spring.remoting.testutil.Request;
import com.github.yihtserns.test.camel.spring.remoting.testutil.Response;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.SimpleRegistry;
import org.apache.camel.support.ExpressionAdapter;
import org.junit.Test;
import org.springframework.remoting.httpinvoker.HttpInvokerProxyFactoryBean;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import org.junit.After;

/**
 * @author yihtserns
 */
public class CamelSpringRemotingTest {

    private SimpleRegistry registry = new SimpleRegistry();
    private DefaultCamelContext camelContext = new DefaultCamelContext(registry);

    @After
    public void stopCamelContext() throws Exception {
        if (camelContext.isStarted()) {
            camelContext.stop();
        }
    }

    @Test
    public void canServeSpringRemotingCallerUsingCamelJetty() throws Exception {
        final String url = "http://localhost:8088/trigger";

        registry.put("springRemotingBinding", SpringRemotingHttpBinding.forServiceInterface(Service.class));
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("jetty:" + url + "?httpBindingRef=#springRemotingBinding")
                        .transform(new ExpressionAdapter() {

                            @Override
                            public Object evaluate(Exchange exchange) {
                                Request request = exchange.getIn().getBody(Request.class);

                                return new Response(request.message + " Bye!");
                            }
                        });
            }
        });
        camelContext.start();

        HttpInvokerProxyFactoryBean factoryBean = new HttpInvokerProxyFactoryBean();
        factoryBean.setServiceInterface(Service.class);
        factoryBean.setServiceUrl(url);
        factoryBean.afterPropertiesSet();
        Service service = (Service) factoryBean.getObject();

        Response response = service.service(new Request("Hi!"));
        assertThat(response, is(notNullValue()));
        assertThat(response.message, is("Hi! Bye!"));
    }

    @Test
    public void canGetHeader() throws Exception {
        final String url = "http://localhost:8088/trigger";

        registry.put("springRemotingBinding", SpringRemotingHttpBinding.forServiceInterface(Service.class));
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("jetty:" + url + "?httpBindingRef=#springRemotingBinding")
                        .process(new Processor() {

                            @Override
                            public void process(Exchange exchange) throws Exception {
                                Request request = exchange.getIn().getMandatoryBody(Request.class);
                                Long timeout = exchange.getIn().getHeader("timeout", Long.class);

                                exchange.getIn().setBody(new Response(request.message + timeout));
                            }
                        });
            }
        });
        camelContext.start();

        HttpInvokerProxyFactoryBean factoryBean = new HttpInvokerProxyFactoryBean();
        factoryBean.setServiceInterface(Service.class);
        factoryBean.setServiceUrl(url);
        factoryBean.afterPropertiesSet();
        Service service = (Service) factoryBean.getObject();

        Response response = service.service(1000, new Request("Timeout: "));
        assertThat(response.message, is("Timeout: 1000"));
    }

    @Test
    public void noIssueWhenServiceReturnTypeIsVoid() throws Exception {
        final String url = "http://localhost:8088/trigger";

        registry.put("springRemotingBinding", SpringRemotingHttpBinding.forServiceInterface(Service.class));
        camelContext.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
                from("jetty:" + url + "?httpBindingRef=#springRemotingBinding")
                        .transform(new ExpressionAdapter() {

                            @Override
                            public Object evaluate(Exchange exchange) {
                                Request request = exchange.getIn().getBody(Request.class);

                                return request.message + " Bye!";
                            }
                        })
                        .to("mock:mock");
            }
        });
        camelContext.start();

        HttpInvokerProxyFactoryBean factoryBean = new HttpInvokerProxyFactoryBean();
        factoryBean.setServiceInterface(Service.class);
        factoryBean.setServiceUrl(url);
        factoryBean.afterPropertiesSet();
        Service service = (Service) factoryBean.getObject();

        MockEndpoint mock = camelContext.getEndpoint("mock:mock", MockEndpoint.class);

        mock.expectedBodiesReceived("Hi! Bye!");
        service.send(new Request("Hi!"));
        mock.assertIsSatisfied(1000);
    }
}
