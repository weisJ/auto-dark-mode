/*
 * MIT License
 *
 * Copyright (c) 2020-2023 Jannis Weis
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and
 * associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute,
 * sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or
 * substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT
 * NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM,
 * DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.github.weisj.darkmode.platform;

import static com.github.weisj.darkmode.platform.ClassLoaderKt.withContextClassLoader;

import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

public class ServiceUtil {

    private ServiceUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates a new service loader for the given service type, using the
     * current appropriate Classloader. This method should be used instal of
     * {@link ServiceLoader#load(Class)} for plugins running on the IntelliJ platform.
     * <a href=
     * "https://jetbrains.org/intellij/sdk/docs/basics/plugin_structure/plugin_class_loaders.html#using-serviceloader">...</a>
     *
     * @throws ServiceConfigurationError
     *                                   if the service type is not accessible to the caller or the
     *                                   caller is in an explicit module and its module descriptor does
     *                                   not declare that it uses {@code service}
     */
    public static <T> ServiceLoader<T> load(final Class<T> serviceClass) {
        return withContextClassLoader(serviceClass.getClassLoader(), () -> ServiceLoader.load(serviceClass));
    }
}
