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
// `Character.isSpace` is named on purpose — it is the deprecated before-pattern the recipe
// matches, not code we run — so suppress the deprecation error the Kotlin compiler raises.
@file:Suppress("DEPRECATION", "DEPRECATION_ERROR")

package com.yourorg

import org.openrewrite.Recipe
import org.openrewrite.recipe

// -----------------------------------------------------------------------------
// The `rewrite { } to { }` DSL is authored in Kotlin, but the recipe it compiles to is
// language-agnostic: at recipe-compile time it becomes a MethodMatcher-driven Recipe over
// the shared Java LST that rewrite-kotlin extends. So when the before/after lambdas name a
// *pure Java* API — here the JDK's `java.lang.Character`, default-imported unqualified in
// both languages — the resulting recipe rewrites the call in Java sources just as it does in
// Kotlin ones. `UseIsWhitespaceTest` asserts both.
//
// `Character.isSpace(char)` has been deprecated since JDK 1.1 in favor of the identically
// shaped `Character.isWhitespace(char)`; because only the method name changes, the `to { }`
// replacement is valid syntax in Java and Kotlin alike.
//
// Keep `displayName`/`description` as plain string literals: the DSL compiler plugin silently
// falls back to a non-serializable recipe when they are built with `+` concatenation.
// -----------------------------------------------------------------------------

val UseIsWhitespace: Recipe = recipe(
    displayName = "Use `Character.isWhitespace` instead of the deprecated `Character.isSpace`",
    description = "`java.lang.Character.isSpace(char)` has been deprecated since JDK 1.1 in favor of `Character.isWhitespace(char)`, which also recognizes Unicode whitespace.",
) {
    edit {
        rewrite { c: Char -> Character.isSpace(c) } to { c -> Character.isWhitespace(c) }
    }
}
