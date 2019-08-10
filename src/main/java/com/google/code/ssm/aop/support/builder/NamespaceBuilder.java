/*
 * Copyright (c) 2014-2019 Jakub Białek
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
 * 
 */

package com.google.code.ssm.aop.support.builder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import org.springframework.util.StringUtils;

import com.google.code.ssm.aop.support.AnnotationData;
import com.google.code.ssm.api.AnnotationConstants;

/**
 * 
 * @author Jakub Białek
 * @since 3.6.0
 *
 */
public class NamespaceBuilder extends AbstractDataBuilder {

    @Override
    protected void build(final AnnotationData data, final Annotation annotation, final Class<? extends Annotation> expectedAnnotationClass,
            final Method targetMethod) throws Exception {
        final String namespace = invokeMethod(annotation, expectedAnnotationClass, "namespace");
        if (AnnotationConstants.DEFAULT_STRING.equals(namespace) || !StringUtils.hasLength(namespace)) {
            throwException("Namespace for annotation [%s] must be defined on [%s]", expectedAnnotationClass, targetMethod);
        }

        data.setNamespace(namespace);
    }

}
