/* Copyright (c) 2012-2019 Jakub Białek
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
 */

package com.google.code.ssm.aop;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static com.google.code.ssm.test.Matcher.any;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.google.code.ssm.api.ParameterValueKeyProvider;
import com.google.code.ssm.api.ReadThroughAssignCache;
import com.google.code.ssm.api.format.SerializationType;

/**
 * 
 * @author Jakub Białek
 * 
 */
public class ReadThroughAssignCacheAdviceTest extends AbstractCacheTest<ReadThroughAssignCacheAdvice> {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
                { true, "method1", new Class[] { int.class }, new Object[] { 22 }, 11, NS + ":1" }, //
                        { true, "method2", new Class[] { int.class }, new Object[] { 22 }, "33", NS + ":2" }, //
                        { true, "method3", new Class[] { int.class, int.class }, new Object[] { 33, 44 }, 33, NS + ":3" }, //

                        { false, "method51", new Class[] { int.class }, new Object[] { 51 }, null, null }, //
                });
    }

    private static final String NS = "TEST-NS";

    private static final int EXPIRATION = 220;

    private final Object expectedValue;

    public ReadThroughAssignCacheAdviceTest(final boolean isValid, final String methodName, final Class<?>[] paramTypes,
            final Object[] params, final Object expectedValue, final String cacheKey) {
        super(isValid, methodName, paramTypes, params, cacheKey);
        this.expectedValue = expectedValue;
    }

    @Before
    public void setUp() {
        super.setUp(new TestService());
    }

    @Test
    public void validCacheMiss() throws Throwable {
        Assume.assumeTrue(isValid);

        when(pjp.proceed()).thenReturn(expectedValue);

        assertEquals(expectedValue, advice.cacheSingleAssign(pjp));

        verify(cache).get(eq(cacheKey), any(SerializationType.class));
        verify(cache).set(eq(cacheKey), eq(EXPIRATION), eq(expectedValue), any(SerializationType.class));
        verify(pjp).proceed();
    }

    @Test
    public void validCacheHit() throws Throwable {
        Assume.assumeTrue(isValid);

        when(cache.get(eq(cacheKey), any(SerializationType.class))).thenReturn(expectedValue);

        assertEquals(expectedValue, advice.cacheSingleAssign(pjp));

        verify(cache).get(eq(cacheKey), any(SerializationType.class));
        verify(cache, never()).set(anyString(), anyInt(), any(), any(SerializationType.class));
        verify(pjp, never()).proceed();
    }

    @Test
    public void invalid() throws Throwable {
        Assume.assumeThat(isValid, CoreMatchers.is(false));

        when(pjp.proceed()).thenReturn(expectedValue);

        assertEquals(expectedValue, advice.cacheSingleAssign(pjp));

        verify(cache, never()).get(anyString(), any(SerializationType.class));
        verify(cache, never()).set(anyString(), anyInt(), any(), any(SerializationType.class));
        verify(pjp).proceed();
    }

    @Override
    protected ReadThroughAssignCacheAdvice createAdvice() {
        return new ReadThroughAssignCacheAdvice();
    }

    @Override
    protected String getNamespace() {
        return NS;
    }

    private static class TestService {

        @ReadThroughAssignCache(namespace = NS, assignedKey = "1", expiration = EXPIRATION)
        public int method1(final int id1) {
            return 1;
        }

        @ReadThroughAssignCache(namespace = NS, assignedKey = "2", expiration = EXPIRATION)
        public String method2(@ParameterValueKeyProvider final int id1) {
            return "2";
        }

        @ReadThroughAssignCache(namespace = NS, assignedKey = "3", expiration = EXPIRATION)
        public int method3(@ParameterValueKeyProvider(order = 1) final int id1, @ParameterValueKeyProvider(order = 2) final int id2) {
            return 3;
        }

        // void method
        @ReadThroughAssignCache(namespace = NS, assignedKey = "51", expiration = EXPIRATION)
        public void method51(@ParameterValueKeyProvider final int id1) {

        }

    }

}
