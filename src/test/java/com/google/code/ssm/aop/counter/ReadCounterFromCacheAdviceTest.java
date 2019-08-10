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

package com.google.code.ssm.aop.counter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.google.code.ssm.api.ParameterValueKeyProvider;
import com.google.code.ssm.api.ReturnValueKeyProvider;
import com.google.code.ssm.api.counter.ReadCounterFromCache;
import com.google.code.ssm.api.format.SerializationType;
import com.google.code.ssm.test.Point;

/**
 * 
 * @author Jakub Białek
 * 
 */
public class ReadCounterFromCacheAdviceTest extends AbstractCounterTest<ReadCounterFromCacheAdvice> {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
                        { true, "readCounter1", new Class[] { int.class }, new Object[] { 1 }, new Integer(1), null }, //
                        { true, "readCounter2", new Class[] { int.class }, new Object[] { 2 }, new Long(2), null }, //
                        { true, "readCounter3", new Class[] { int.class }, new Object[] { 3 }, 3L, null }, //
                        { true, "readCounter4", new Class[] { int.class }, new Object[] { 4 }, 4, null }, //
                        { true, "readCounter5", new Class[] { int.class, String.class }, new Object[] { 5, "v1" }, 5, null }, //
                        { true, "readCounter6", new Class[] { int.class, String.class, Point.class, String.class },
                                new Object[] { 6, "v1", new Point(2, 3), "test" }, 5, NS + ":v1/(2,3)/6" }, //

                        { false, "readCounter20", new Class[] { int.class }, new Object[] { 20 }, "20xx", null }, //
                        { false, "readCounter21", new Class[] { int.class }, new Object[] { 21 }, new Object(), null }, //
                        { false, "readCounter22", new Class[] { int.class }, new Object[] { 4 }, 22, null }, //
                        { false, "readCounter23", new Class[] { int.class, int.class }, new Object[] { 144, 43 }, 23, null }, //
                        { false, "readCounter24", new Class[] { int.class }, new Object[] { 24 }, null, null }, //
                        { false, "readCounter25", new Class[] { int.class }, new Object[] { 25 }, new Integer(25), null }, //
                });
    }

    private static final int EXPIRATION = 100;

    private final Object expectedValue;

    public ReadCounterFromCacheAdviceTest(final boolean isValid, final String methodName, final Class<?>[] paramTypes,
            final Object[] params, final Object expectedValue, final String cacheKey) {
        super(isValid, methodName, paramTypes, params, cacheKey);
        this.expectedValue = expectedValue;
    }

    @Before
    public void setUp() {
        super.setUp(new TestService());
    }

    @Test
    public void validReadCounterCacheMiss() throws Throwable {
        Assume.assumeTrue(isValid);

        when(pjp.proceed()).thenReturn(expectedValue);
        when(cache.getCounter(eq(cacheKey))).thenReturn(null);

        assertEquals(expectedValue, advice.readCounter(pjp));

        verify(cache).getCounter(eq(cacheKey));
        verify(cache).incr(eq(cacheKey), eq(0), eq(((Number) expectedValue).longValue()), eq(EXPIRATION));
        verify(pjp).proceed();
    }

    @Test
    public void validReadCounterCacheHit() throws Throwable {
        Assume.assumeTrue(isValid);

        Long value = 100L;

        when(cache.getCounter(eq(cacheKey))).thenReturn(value);

        assertEquals(value.intValue(), ((Number) advice.readCounter(pjp)).intValue());

        verify(cache).getCounter(eq(cacheKey));
        verify(cache, never()).incr(anyString(), anyInt(), anyLong(), anyInt());
        verify(pjp, never()).proceed();
    }

    @Test
    public void invalidReadCounter() throws Throwable {
        Assume.assumeThat(isValid, CoreMatchers.is(false));

        when(pjp.proceed()).thenReturn(expectedValue);

        assertEquals(expectedValue, advice.readCounter(pjp));

        verify(cache, never()).getCounter(anyString());
        verify(cache, never()).get(anyString(), any(SerializationType.class));
        verify(cache, never()).incr(anyString(), anyInt(), anyLong(), anyInt());
        verify(pjp).proceed();
    }

    @Override
    protected ReadCounterFromCacheAdvice createAdvice() {
        return new ReadCounterFromCacheAdvice();
    }

    private static class TestService {

        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public Integer readCounter1(@ParameterValueKeyProvider final int id) {
            return 10;
        }

        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public Long readCounter2(@ParameterValueKeyProvider final int id) {
            return 11L;
        }

        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public long readCounter3(@ParameterValueKeyProvider final int id) {
            return 12L;
        }

        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public int readCounter4(@ParameterValueKeyProvider final int id) {
            return 13;
        }

        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public int readCounter5(@ParameterValueKeyProvider(order = 0) final int id, @ParameterValueKeyProvider(order = 1) final String id2) {
            return 14;
        }

        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public int readCounter6(@ParameterValueKeyProvider(order = 3) final int id, @ParameterValueKeyProvider(order = 1) final String id2,
                @ParameterValueKeyProvider(order = 2) final Point p, final String sth) {
            return 6;
        }

        // wrong return type
        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public String readCounter20(@ParameterValueKeyProvider final int id) {
            return null;
        }

        // wrong return type
        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public Object readCounter21(@ParameterValueKeyProvider final int id) {
            return null;
        }

        // no @ParameterValueKeyProvider
        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public int readCounter22(final int id) {
            return 13;
        }

        // the same order in both @ParameterValueKeyProvider
        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public int readCounter23(@ParameterValueKeyProvider(order = 0) final int id, @ParameterValueKeyProvider(order = 0) final int id2) {
            return 14;
        }

        // wrong void return type
        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public void readCounter24(@ParameterValueKeyProvider final int id) {

        }

        @ReturnValueKeyProvider
        @ReadCounterFromCache(namespace = NS, expiration = EXPIRATION)
        public Integer readCounter25(final int id) {
            return 25;
        }

    }

}
