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

import org.apache.camel.Body;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

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
        SpringRemotingHttpBinding.forServiceInterface(OneParamService.class);
    }

    @Test
    public void shouldThrowWhenMoreThanOneParamButNoneAnnotatedAsBody() throws Exception {
        thrown.expect(IllegalArgumentException.class);
        thrown.expectMessage("One of the parameters of method 'public abstract java.lang.Object com.github.yihtserns.test.camel.spring.remoting.SpringRemotingHttpBindingTest$UnannotatedTwoParamService.service(java.lang.Object,java.lang.Object)' must be annotated with @Body");
        SpringRemotingHttpBinding.forServiceInterface(UnannotatedTwoParamService.class);
    }

    @Test
    public void canCreateBindingWhenOneOfParamsIsAnnotatedAsBody() throws Exception {
        SpringRemotingHttpBinding.forServiceInterface(OneAnnotatedMultiParamService.class);
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
}
