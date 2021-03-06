/* ServiceWrapperFactory.java
 *
 * Copyright 2009-2012 Comcast Interactive Media, LLC.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.fishwife.jrugged;

/**
 * A {@link ServiceWrapperFactory} is a way to create several related but
 * distinct {@link ServiceWrapper} instances.
 *
 */
public interface ServiceWrapperFactory {

    /**
     * Create a new <code>ServiceWrapper</code> using the given (presumed
     * unique) identifier.
     * 
     * @param name
     *            to apply to the new <code>ServiceWrapper</code>
     * @return <code>ServiceWrapper</code>
     */
    ServiceWrapper getWrapperWithName(String name);
}
