/*
 * Copyright (c) 2012-2019 Jakub Białek
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
package com.google.code.ssm;

import com.google.code.ssm.api.BridgeMethodMappings;

import lombok.Data;

/**
 * Global SSM settings.
 * 
 * @author Jakub Białek
 * @since 3.1.0
 * 
 */
@Data
public class Settings {

    /**
     * The order of cache advice. The default order (0) allows the SSM cache advises to be invoked before transaction
     * interceptor.
     */
    private int order = 0;

    /**
     * If true then SSM caching is disabled. This value can be overwritten by system property: ssm.cache.disable. 
     * 
     * @since 3.6.0
     */
    private boolean disableCache = false;

    /**
     * If true SSM annotations can be declared in interfaces. If an interface is generic and annotated method has 
     * at least one generic parameter it's required to provide additional information in each implementing class 
     * using {@link BridgeMethodMappings}.
     * 
     * @since 4.1.0
     */
    private boolean enableAnnotationsInInterface = false; 
}
