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

import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinTemplate
import org.openrewrite.kotlin.KotlinVisitor

// -----------------------------------------------------------------------------
// `KotlinTemplate` (the Kotlin counterpart to `JavaTemplate`) parses a snippet of
// Kotlin source into fresh LST and splices it in, filling each `#{...}` hole with a
// node lifted from the matched code. Reach for it when the "after" side is a
// different syntactic *shape* than the "before" — not just a fixed rewrite of the
// matched arguments — which the declarative `rewrite { } to { }` DSL (see
// `UseModernKotlinApis`) cannot express.
//
// This recipe rewrites a hand-written null guard
//
//     if (name == null) {
//         throw IllegalArgumentException("name must not be null")
//     }
//
// into the equivalent Elvis-`throw` one-liner
//
//     name ?: throw IllegalArgumentException("name must not be null")
//
// Two things put this beyond `rewrite { } to { }`: the before-pattern is an `if`
// *statement* (control flow), not an expression call it can match; and the guarded
// value moves in front of a new `?:` operator, reusing the original `throw` on the
// right. The whole `if` is replaced, and the replacement is a different node type
// (`J.If` -> an Elvis expression), so the visitor is a non-iso `KotlinVisitor`.
// -----------------------------------------------------------------------------

class UseElvisThrow : Recipe() {

    override fun getDisplayName(): String = "Use an Elvis `throw` for null guards"

    override fun getDescription(): String =
        "Replaces a hand-written `if (x == null) throw ...` guard with the equivalent `x ?: throw ...` Elvis " +
            "expression, reusing the thrown exception unchanged."

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : KotlinVisitor<ExecutionContext>() {

            override fun visitIf(iff: J.If, ctx: ExecutionContext): J {
                val nullCheck = iff.ifCondition.tree as? J.Binary ?: return super.visitIf(iff, ctx)
                if (nullCheck.operator != J.Binary.Type.Equal || iff.elsePart != null) {
                    return super.visitIf(iff, ctx)
                }
                val value = when {
                    J.Literal.isLiteralValue(nullCheck.right, null) -> nullCheck.left
                    J.Literal.isLiteralValue(nullCheck.left, null) -> nullCheck.right
                    else -> return super.visitIf(iff, ctx)
                }
                val exception = when (val thenPart = iff.thenPart) {
                    is J.Throw -> thenPart.exception
                    is J.Block -> (thenPart.statements.singleOrNull() as? J.Throw)?.exception
                        ?: return super.visitIf(iff, ctx)
                    else -> return super.visitIf(iff, ctx)
                }
                return KotlinTemplate.builder("#{any()} ?: throw #{any()}").build()
                    .apply(cursor, iff.coordinates.replace(), value, exception)
            }
        }
}
