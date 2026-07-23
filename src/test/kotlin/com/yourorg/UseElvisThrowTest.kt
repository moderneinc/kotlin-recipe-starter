/*
 * Copyright 2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.yourorg

import org.junit.jupiter.api.Test
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class UseElvisThrowTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(UseElvisThrow())
    }

    @Test
    fun `braced null guard becomes an Elvis throw`() = rewriteRun(
        kotlin(
            """
            fun greet(name: String?) {
                if (name == null) {
                    throw IllegalArgumentException("name must not be null")
                }
                println(name)
            }
            """,
            """
            fun greet(name: String?) {
                name ?: throw IllegalArgumentException("name must not be null")
                println(name)
            }
            """,
        ),
    )

    @Test
    fun `braceless null guard becomes an Elvis throw`() = rewriteRun(
        kotlin(
            """
            fun greet(name: String?) {
                if (name == null) throw IllegalArgumentException("required")
            }
            """,
            """
            fun greet(name: String?) {
                name ?: throw IllegalArgumentException("required")
            }
            """,
        ),
    )

    @Test
    fun `null on the left-hand side is handled`() = rewriteRun(
        kotlin(
            """
            fun greet(name: String?) {
                if (null == name) {
                    throw IllegalArgumentException("required")
                }
            }
            """,
            """
            fun greet(name: String?) {
                name ?: throw IllegalArgumentException("required")
            }
            """,
        ),
    )

    @Test
    fun `the thrown exception is reused whatever its type`() = rewriteRun(
        kotlin(
            """
            fun greet(name: String?) {
                if (name == null) {
                    throw IllegalStateException("required")
                }
            }
            """,
            """
            fun greet(name: String?) {
                name ?: throw IllegalStateException("required")
            }
            """,
        ),
    )

    @Test
    fun `a guard with an else branch is left alone`() = rewriteRun(
        kotlin(
            """
            fun describe(name: String?): String {
                if (name == null) {
                    throw IllegalArgumentException("required")
                } else {
                    return name
                }
            }
            """,
        ),
    )
}
