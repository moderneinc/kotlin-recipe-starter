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

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.tuple
import org.junit.jupiter.api.Test
import org.openrewrite.kotlin.Assertions.kotlin
import org.openrewrite.test.RecipeSpec
import org.openrewrite.test.RewriteTest

class FindKotlinFunctionsTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        spec.recipe(FindKotlinFunctions())
    }

    @Test
    fun `records each function and its parameter count`() = rewriteRun(
        { spec ->
            spec.dataTable(KotlinFunctions.Row::class.java) { rows ->
                assertThat(rows)
                    .extracting("functionName", "parameterCount")
                    .containsExactlyInAnyOrder(
                        tuple("greet", 0),
                        tuple("add", 2),
                    )
            }
        },
        kotlin(
            """
            fun greet() {
            }
            fun add(a: Int, b: Int): Int = a + b
            """,
        ),
    )
}
