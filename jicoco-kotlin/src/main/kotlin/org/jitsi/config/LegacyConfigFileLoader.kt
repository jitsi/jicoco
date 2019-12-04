/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.config

import com.typesafe.config.Config
import com.typesafe.config.ConfigFactory
import org.jitsi.utils.logging2.LoggerImpl
import java.nio.file.InvalidPathException
import java.nio.file.Paths

/**
 * Used to load a config file that exists outside of the typesafe config
 * library's convention for log file locations.  For example, we use this to
 * load a legacy sip-communicator.properties config file into a [Config]
 * instance.
 */
class LegacyConfigFileLoader {
    companion object {
        private val logger = LoggerImpl(LegacyConfigFileLoader::class.java.name)

        /**
         * [first] and [rest] are [String]s which will be combined to form
         * the [java.nio.file.Path] which we'll use to load the file.
         */
        fun load(first: String?, vararg rest: String?): Config {
            logger.info("Attempting to load legacy config file at path " +
            listOf(first, *rest).joinToString())
            return try {
                // Sanitize any null arguments, since Paths.get requires they
                // be non-null.  If any get changed/filtered here, throw an
                // InvalidPathException
                val firstPathArg = first ?: throw InvalidPathException(first, "null path")
                val otherPathArgs = rest.filterNotNull().toTypedArray()
                if (otherPathArgs.size != rest.size) {
                    throw InvalidPathException("", "null path")
                }
                val path = Paths.get(firstPathArg, *otherPathArgs)
                val file = path.toFile()
                if (!file.exists()) {
                    throw InvalidPathException(path.toString(), "path doesn't exist")
                }
                ConfigFactory.parseFile(file)
            } catch (e: InvalidPathException) {
                logger.info("No legacy config file found: $e")
                ConfigFactory.parseString("")
            } catch (e: NullPointerException) {
                logger.info("No legacy config file found: $e")
                ConfigFactory.parseString("")
            }
        }
    }
}
