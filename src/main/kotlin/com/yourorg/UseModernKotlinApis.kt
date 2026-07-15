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
// The `rewrite { }` lambdas name deprecated stdlib APIs on purpose — that call is the
// before-pattern the recipe matches, not code we run — so suppress the deprecation errors.
@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

package com.yourorg

import org.openrewrite.Recipe
import org.openrewrite.recipe
import org.openrewrite.recipes

// -----------------------------------------------------------------------------
// Declarative "pattern-shaped" recipes, written with the Kotlin recipe DSL.
//
// `recipe { edit { rewrite { <before> } to { <after> } } }` reads as a before/after
// pair of Kotlin lambdas. At recipe-compile time the K2 compiler plugin bundled in
// rewrite-kotlin turns each `rewrite { } to { }` into a MethodMatcher-driven Recipe
// — you never write a visitor by hand for this shape. See
// https://moderne.ai/blog/kotlin-recipes-for-openrewrite
// -----------------------------------------------------------------------------

val UseUppercase: Recipe = recipe(
    displayName = "Use `uppercase()` instead of `toUpperCase()`",
    description = "`String.toUpperCase()` was deprecated in Kotlin 1.5 in favor of the locale-explicit `uppercase()`.",
) {
    edit {
        rewrite { s: String -> s.toUpperCase() } to { s -> s.uppercase() }
    }
}

val UseLowercase: Recipe = recipe(
    displayName = "Use `lowercase()` instead of `toLowerCase()`",
    description = "`String.toLowerCase()` was deprecated in Kotlin 1.5 in favor of the locale-explicit `lowercase()`.",
) {
    edit {
        rewrite { s: String -> s.toLowerCase() } to { s -> s.lowercase() }
    }
}

val UseCharCode: Recipe = recipe(
    displayName = "Use `Char.code` instead of `Char.toInt()`",
    description = "`Char.toInt()` was deprecated in Kotlin 1.5; the replacement `Char.code` makes the conversion-to-codepoint intent explicit (the old name collided with `Number.toInt()`).",
) {
    edit {
        rewrite { c: Char -> c.toInt() } to { c -> c.code }
    }
}

// -----------------------------------------------------------------------------
// `recipes(...)` composes several recipes into one. Point consumers at the
// composite and they get every sub-recipe; each sub-recipe is still runnable on
// its own.
// -----------------------------------------------------------------------------

val UseModernKotlinApis: Recipe = recipes(
    displayName = "Use modern Kotlin stdlib APIs",
    description = "Replaces Kotlin stdlib APIs deprecated between 1.4 and 2.0 with their modern equivalents.",
    UseUppercase,
    UseLowercase,
    UseCharCode,
)
