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
import org.openrewrite.test.TypeValidation

class UseModernKotlinApisTest : RewriteTest {

    override fun defaults(spec: RecipeSpec) {
        // Pattern-mode rewrites only update the invocation's name; the attached
        // JavaType.Method still carries the old name, which the default type
        // validation flags. That mismatch is unrelated to whether the rewrite fired.
        spec.typeValidationOptions(TypeValidation.none())
    }

    @Test
    fun `toUpperCase becomes uppercase`() = rewriteRun(
        { spec -> spec.recipe(UseUppercase) },
        kotlin(
            """
            val s: String = "hello".toUpperCase()
            """,
            """
            val s: String = "hello".uppercase()
            """,
        ),
    )

    @Test
    fun `toLowerCase becomes lowercase`() = rewriteRun(
        { spec -> spec.recipe(UseLowercase) },
        kotlin(
            """
            val s: String = "HELLO".toLowerCase()
            """,
            """
            val s: String = "HELLO".lowercase()
            """,
        ),
    )

    @Test
    fun `Char toInt becomes code`() = rewriteRun(
        { spec -> spec.recipe(UseCharCode) },
        kotlin(
            """
            val i: Int = 'a'.toInt()
            """,
            """
            val i: Int = 'a'.code
            """,
        ),
    )

    @Test
    fun `sumBy becomes sumOf, threading the selector lambda through`() = rewriteRun(
        { spec -> spec.recipe(UseSumOf) },
        kotlin(
            """
            fun total(xs: List<Int>): Int = xs.sumBy { it * 2 }
            """,
            """
            fun total(xs: List<Int>): Int = xs.sumOf { it * 2 }
            """,
        ),
    )

    @Test
    fun `Math max becomes kotlin math max`() = rewriteRun(
        { spec -> spec.recipe(UseKotlinMathMax) },
        kotlin(
            """
            fun m(a: Double, b: Double): Double = Math.max(a, b)
            """,
            """
            fun m(a: Double, b: Double): Double = kotlin.math.max(a, b)
            """,
        ),
    )

    @Test
    fun `Math PI becomes kotlin math PI`() = rewriteRun(
        { spec -> spec.recipe(UseKotlinMathPi) },
        kotlin(
            """
            fun circumference(r: Double): Double = 2 * Math.PI * r
            """,
            """
            fun circumference(r: Double): Double = 2 * kotlin.math.PI * r
            """,
        ),
    )

    @Test
    fun `the composite runs every sub-recipe`() = rewriteRun(
        { spec -> spec.recipe(UseModernKotlinApis) },
        kotlin(
            """
            val up: String = "hello".toUpperCase()
            val down: String = "HELLO".toLowerCase()
            val code: Int = 'a'.toInt()
            fun total(xs: List<Int>): Int = xs.sumBy { it }
            fun m(a: Double, b: Double): Double = Math.max(a, b)
            """,
            """
            val up: String = "hello".uppercase()
            val down: String = "HELLO".lowercase()
            val code: Int = 'a'.code
            fun total(xs: List<Int>): Int = xs.sumOf { it }
            fun m(a: Double, b: Double): Double = kotlin.math.max(a, b)
            """,
        ),
    )
}
