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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.Arrays;
import java.util.Collection;

import org.hamcrest.CoreMatchers;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized.Parameters;

import com.google.code.ssm.api.ParameterDataUpdateContent;
import com.google.code.ssm.api.ParameterValueKeyProvider;
import com.google.code.ssm.api.ReturnDataUpdateContent;
import com.google.code.ssm.api.counter.UpdateCounterInCache;
import com.google.code.ssm.api.format.SerializationType;
import com.google.code.ssm.test.Point;

/**
 * 
 * @author Jakub Białek
 * 
 */
public class UpdateCounterInCacheAdviceTest extends AbstractCounterTest<UpdateCounterInCacheAdvice> {

    @Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] { //
                        { true, "updateCounter1", new Class[] { int.class }, new Object[] { 1 }, 1, 1, null }, //
                        { true, "updateCounter2", new Class[] { int.class }, new Object[] { 2 }, new Integer(2), new Integer(2), null }, //
                        { true, "updateCounter3", new Class[] { int.class }, new Object[] { 3 }, 3L, 3L, null }, //
                        { true, "updateCounter4", new Class[] { int.class }, new Object[] { 4 }, new Long(4L), new Long(4L), null }, //
                        { true, "updateCounter5", new Class[] { int.class, int.class }, new Object[] { 5, 65 }, 5, 5, null }, //
                        { true, "updateCounter6", new Class[] { String.class, int.class }, new Object[] { "8", 6 }, null, 6, NS + ":8" }, //
                        { true, "updateCounter7", new Class[] { String.class, Integer.class }, new Object[] { "234", new Integer(7) },
                                null, new Integer(7), NS + ":234" }, //
                        { true, "updateCounter8", new Class[] { Point.class, long.class }, new Object[] { new Point(1, 4), 8L }, null, 8L,
                                NS + ":(1,4)" }, //
                        { true, "updateCounter9", new Class[] { Long.class, String.class, Long.class },
                                new Object[] { new Long(1), "2", new Long(9L) }, null, new Long(9L), NS + ":2/1" }, //

                        { false, "updateCounter20", new Class[] { int.class }, new Object[] { 20 }, null, null, null }, //
                        { false, "updateCounter21", new Class[] { int.class }, new Object[] { 21 }, "abcd", null, null }, //
                        { false, "updateCounter22", new Class[] { int.class }, new Object[] { 4 }, 22, null, null }, //
                        { false, "updateCounter23", new Class[] { int.class }, new Object[] { 4 }, null, null, null }, //
                        { false, "updateCounter24", new Class[] { Object.class }, new Object[] { new Object() }, null, null, null }, //
                        { false, "updateCounter25", new Class[] { String.class }, new Object[] { "xyzzyx" }, null, null, null }, //
                });
    }

    private static final int EXPIRATION = 80;

    private final Object reternValue;

    private final Object expectedValue;

    public UpdateCounterInCacheAdviceTest(final boolean isValid, final String methodName, final Class<?>[] paramTypes,
            final Object[] params, final Object returnValue, final Object expectedValue, final String cacheKey) {
        super(isValid, methodName, paramTypes, params, cacheKey);
        this.reternValue = returnValue;
        this.expectedValue = expectedValue;
    }

    @Before
    public void setUp() {
        super.setUp(new TestService());
    }

    @Test
    public void validUpdateCounterInCache() throws Throwable {
        Assume.assumeTrue(isValid);

        advice.cacheCounterInCache(pjp, reternValue);

        verify(cache).setCounter(eq(cacheKey), eq(EXPIRATION), eq(((Number) expectedValue).longValue()));
        verify(pjp, never()).proceed();
    }

    @Test
    public void invalidUpdateCounterInCache() throws Throwable {
        Assume.assumeThat(isValid, CoreMatchers.is(false));

        advice.cacheCounterInCache(pjp, reternValue);

        verify(cache, never()).setCounter(anyString(), anyInt(), anyLong());
        verify(cache, never()).set(anyString(), anyInt(), anyLong(), any(SerializationType.class));
        verify(pjp, never()).proceed();
    }

    @Override
    protected UpdateCounterInCacheAdvice createAdvice() {
        return new UpdateCounterInCacheAdvice();
    }

    private static class TestService {

        @ReturnDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public int updateCounter1(@ParameterValueKeyProvider final int id1) {
            return 1;
        }

        @ReturnDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public Integer updateCounter2(@ParameterValueKeyProvider final int id1) {
            return new Integer(2);
        }

        @ReturnDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public long updateCounter3(@ParameterValueKeyProvider final int id1) {
            return 1L;
        }

        @ReturnDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public Long updateCounter4(@ParameterValueKeyProvider final int id1) {
            return new Long(1L);
        }

        @ReturnDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public int updateCounter5(@ParameterValueKeyProvider(order = 1) final int id1, @ParameterValueKeyProvider(order = 5) final int id2) {
            return 1;
        }

        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public void updateCounter6(@ParameterValueKeyProvider final String id1, @ParameterDataUpdateContent final int value) {

        }

        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public void updateCounter7(@ParameterValueKeyProvider final String id, @ParameterDataUpdateContent final Integer value) {

        }

        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public void updateCounter8(@ParameterValueKeyProvider final Point p, @ParameterDataUpdateContent final long value) {

        }

        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public void updateCounter9(@ParameterValueKeyProvider(order = 2) final Long id1,
                @ParameterValueKeyProvider(order = 1) final String id2, @ParameterDataUpdateContent final Long value) {

        }

        // @ReturnDataUpdateContent is more important than @ParameterDataUpdateContent
        @ReturnDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public Integer updateCounter10(@ParameterDataUpdateContent @ParameterValueKeyProvider final int id1) {
            return new Integer(10);
        }

        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public void updateCounter11(@ParameterDataUpdateContent @ParameterValueKeyProvider final int id1,
                @ParameterValueKeyProvider(order = 5) final int id2) {

        }

        // no return
        @ReturnDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public void updateCounter20(@ParameterValueKeyProvider final int id1) {

        }

        // wrong return type
        @ReturnDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public String updateCounter21(@ParameterValueKeyProvider final int id1) {
            return "xyz";
        }

        // no @ParameterValueKeyProvider
        @ReturnDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public int updateCounter22(final int id1) {
            return 1;
        }

        // no @ReturnDataUpdateContent or @ParameterDataUpdateContent
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public void updateCounter23(@ParameterValueKeyProvider final int id1) {

        }

        // no wrong update type
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public void updateCounter24(@ParameterDataUpdateContent @ParameterValueKeyProvider final Object o) {

        }

        // no wrong update type
        @UpdateCounterInCache(namespace = NS, expiration = EXPIRATION)
        public void updateCounter25(@ParameterDataUpdateContent @ParameterValueKeyProvider final String s) {

        }

    }

}
