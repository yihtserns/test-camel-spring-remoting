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
package com.github.yihtserns.test.camel.spring.remoting.testutil;

import org.apache.camel.Body;
import org.apache.camel.Header;

/**
 * @author yihtserns
 */
public interface Service {

    Response service(Request req);

    Response service(
            @Header("timeout") long timeout,
            @Body Request req);

    void send(Request req);
}
