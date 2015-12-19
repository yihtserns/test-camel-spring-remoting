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
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

    @Override
    public void readRequest(HttpServletRequest request, HttpMessage message) {
        super.readRequest(request, message);

        RemoteInvocation remoteInvocation = (RemoteInvocation) message.getBody();
        message.setBody(remoteInvocation.getArguments()[0]);
    }

    @Override
    public void doWriteResponse(Message message, HttpServletResponse response, Exchange exchange) throws IOException {
        message.setBody(new RemoteInvocationResult(message.getBody()));

        super.doWriteResponse(message, response, exchange);
    }
}
