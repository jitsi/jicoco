/*
 * Copyright @ 2015 - present, 8x8 Inc
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
package org.jitsi.assertions;

import org.jitsi.utils.*;

/**
 * Utility class for making assertions on variables.
 *
 * @author Pawel Domas
 */
public class Assert
{
    /**
     * Will thrown an <tt>IllegalArgumentException</tt> if given <tt>str</tt>
     * is either <tt>null</tt> or is empty.
     *
     * @param str the <tt>String</tt> instance to be verified.
     * @param errorMsg the error message that will be passed to the constructor
     *        of <tt>IllegalArgumentException</tt>.
     *
     * @throws IllegalArgumentException if given <tt>str</tt> is either
     *         <tt>null</tt> or empty.
     * @return the string.
     */
    static public String notNullNorEmpty(String str, String errorMsg)
    {
        if (StringUtils.isNullOrEmpty(str))
        {
            throw new IllegalArgumentException(errorMsg);
        }
        return str;
    }
}
