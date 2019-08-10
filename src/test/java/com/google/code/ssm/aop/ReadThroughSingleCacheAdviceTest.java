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
import com.google.code.ssm.api.ReadThroughSingleCache;
import com.google.code.ssm.api.ReturnValueKeyProvider;
import com.google.code.ssm.api.format.SerializationType;

/**
 * 
 * @author Jakub Białek
 * 
 */
public class ReadThroughSingleCacheAdviceTest extends AbstractCacheTest<ReadThroughSingleCacheAdvice> {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
                { true, "method1", new Class[] { int.class }, new Object[] { 1 }, 1 }, //
                        { true, "method2", new Class[] { int.class }, new Object[] { 2 }, "2" }, //
                        { true, "method3", new Class[] { int.class, int.class }, new Object[] { 3, 44 }, 3 }, //

                        { false, "method50", new Class[] { int.class }, new Object[] { 50 }, 50 }, //
                        { false, "method51", new Class[] { int.class }, new Object[] { 51 }, null }, //
                        { false, "method52", new Class[] { int.class }, new Object[] { 1 }, 1 }, //
                });
    }

    private static final String NS = "TEST_NS";

    private static final int EXPIRATION = 110;

    private final Object expectedValue;

    public ReadThroughSingleCacheAdviceTest(final boolean isValid, final String methodName, final Class<?>[] paramTypes,
            final Object[] params, final Object expectedValue) {
        super(isValid, methodName, paramTypes, params, null);
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

        assertEquals(expectedValue, advice.cacheGetSingle(pjp));

        verify(cache).get(eq(cacheKey), any(SerializationType.class));
        verify(cache).set(eq(cacheKey), eq(EXPIRATION), eq(expectedValue), any(SerializationType.class));
        verify(pjp).proceed();
    }

    @Test
    public void validCacheHit() throws Throwable {
        Assume.assumeTrue(isValid);

        when(cache.get(eq(cacheKey), any(SerializationType.class))).thenReturn(expectedValue);

        assertEquals(expectedValue, advice.cacheGetSingle(pjp));

        verify(cache).get(eq(cacheKey), any(SerializationType.class));
        verify(cache, never()).set(anyString(), anyInt(), any(), any(SerializationType.class));
        verify(pjp, never()).proceed();
    }

    @Test
    public void invalid() throws Throwable {
        Assume.assumeThat(isValid, CoreMatchers.is(false));

        when(pjp.proceed()).thenReturn(expectedValue);

        assertEquals(expectedValue, advice.cacheGetSingle(pjp));

        verify(cache, never()).get(anyString(), any(SerializationType.class));
        verify(cache, never()).set(anyString(), anyInt(), any(), any(SerializationType.class));
        verify(pjp).proceed();
    }

    @Override
    protected ReadThroughSingleCacheAdvice createAdvice() {
        return new ReadThroughSingleCacheAdvice();
    }

    @Override
    protected String getNamespace() {
        return NS;
    }

    private static class TestService {

        @ReadThroughSingleCache(namespace = NS, expiration = EXPIRATION)
        public int method1(@ParameterValueKeyProvider final int id1) {
            return 1;
        }

        @ReadThroughSingleCache(namespace = NS, expiration = EXPIRATION)
        public String method2(@ParameterValueKeyProvider final int id1) {
            return "2";
        }

        @ReadThroughSingleCache(namespace = NS, expiration = EXPIRATION)
        public int method3(@ParameterValueKeyProvider(order = 1) final int id1, @ParameterValueKeyProvider(order = 2) final int id2) {
            return 3;
        }

        // no @ParameterValueKeyProvider
        @ReadThroughSingleCache(namespace = NS, expiration = EXPIRATION)
        public int method50(final int id1) {
            return 50;
        }

        // void method
        @ReadThroughSingleCache(namespace = NS, expiration = EXPIRATION)
        public void method51(@ParameterValueKeyProvider final int id1) {

        }

        // ReturnValueKeyProvider is not supported by ReadThroughSingleCache
        @ReturnValueKeyProvider
        @ReadThroughSingleCache(namespace = NS, expiration = EXPIRATION)
        public int method52(final int id1) {
            return 1;
        }

    }

}
