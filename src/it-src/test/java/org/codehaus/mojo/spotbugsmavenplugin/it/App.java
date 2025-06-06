/*
 * Copyright 2005-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.mojo.spotbugsmavenplugin.it;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Hello world!
 */
public class App implements Cloneable {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    private App() {
        // Do not allow instantiation
    }

    public static void main(String[] args) {
        Objects.isNull(args);
        logger.info("Hello World!");
    }

    public Object clone() {
        return null; // Does not call 'super.clone()'.
    }

}
