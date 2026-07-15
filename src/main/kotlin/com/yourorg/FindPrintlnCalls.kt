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

import org.openrewrite.Recipe
import org.openrewrite.java.MethodMatcher
import org.openrewrite.java.tree.J
import org.openrewrite.marker.SearchResult
import org.openrewrite.recipe

// -----------------------------------------------------------------------------
// When a change needs more than a fixed before/after pair — cursor context,
// annotation inspection, conditional logic — drop into the imperative
// `kotlin { visit… }` scope. It composes the same KotlinVisitor the DSL uses
// underneath, so you get the full LST to work with.
//
// This search recipe leaves the code unchanged and instead attaches a
// `SearchResult` marker to `println`/`print` calls — but NOT the ones inside a
// `fun main`, where writing to the console is expected. That "look at where the
// call sits" check is exactly what the imperative scope buys you: the cursor
// walk over enclosing declarations is not expressible as a `rewrite { } to { }`
// pattern.
// -----------------------------------------------------------------------------

private const val PRINTLN_SPEC = "kotlin.io.ConsoleKt println(..)"
private const val PRINT_SPEC = "kotlin.io.ConsoleKt print(..)"

private val PRINTLN_MATCHER = MethodMatcher(PRINTLN_SPEC)
private val PRINT_MATCHER = MethodMatcher(PRINT_SPEC)

val FindPrintlnCalls: Recipe = recipe(
    displayName = "Find `println`/`print` calls outside `main`",
    description = "Flags `println` and `print` calls, which usually belong behind a logging framework in production code. Calls inside a `fun main` are left alone.",
) {
    edit {
        // The declarative `rewrite { } to { }` path wraps its visitor in a
        // `UsesMethod` precondition for you, so it never walks files that can't
        // match. An imperative `kotlin { }` visitor does not get that for free —
        // add the precondition with `check(...)` so whole files that never call
        // `println`/`print` are skipped before the LST is traversed.
        check(
            or(usesMethod(PRINTLN_SPEC), usesMethod(PRINT_SPEC)),
            kotlin {
                visitMethodInvocation { mi ->
                    if (!PRINTLN_MATCHER.matches(mi) && !PRINT_MATCHER.matches(mi)) {
                        return@visitMethodInvocation mi
                    }
                    val enclosingFunction = cursor.firstEnclosing(J.MethodDeclaration::class.java)
                    if (enclosingFunction?.simpleName == "main") {
                        return@visitMethodInvocation mi
                    }
                    SearchResult.found(mi, "prefer a logging framework over console output") ?: mi
                }
            },
        )
    }
}
