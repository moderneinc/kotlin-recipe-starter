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

import org.openrewrite.Column
import org.openrewrite.DataTable
import org.openrewrite.ExecutionContext
import org.openrewrite.Recipe
import org.openrewrite.SourceFile
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinIsoVisitor

// -----------------------------------------------------------------------------
// A data table lets a recipe emit structured rows (exported as CSV / surfaced on
// the Moderne platform) instead of, or alongside, changing code. Inserting a row
// needs the `ExecutionContext`, which the visit method receives directly — so
// data-table recipes are written as a full `Recipe` with a `KotlinIsoVisitor`
// rather than through the `kotlin { visitX { node -> … } }` DSL sugar, whose
// lambda exposes the node but not the context.
//
// This recipe makes no code changes; it records the name and parameter count of
// every Kotlin function declaration.
// -----------------------------------------------------------------------------

class KotlinFunctions(recipe: Recipe) : DataTable<KotlinFunctions.Row>(
    recipe,
    "Kotlin functions",
    "The name and parameter count of every Kotlin function declaration.",
) {
    data class Row(
        @field:Column(displayName = "Source path", description = "Path of the file that declares the function.")
        val sourcePath: String,
        @field:Column(displayName = "Function name", description = "The declared function name.")
        val functionName: String,
        @field:Column(displayName = "Parameter count", description = "Number of value parameters the function declares.")
        val parameterCount: Int,
    )
}

class FindKotlinFunctions : Recipe() {

    override fun getDisplayName(): String = "Find Kotlin function declarations"

    override fun getDescription(): String =
        "Records the name and parameter count of every Kotlin function declaration in a data table."

    override fun getVisitor(): TreeVisitor<*, ExecutionContext> =
        object : KotlinIsoVisitor<ExecutionContext>() {
            private val functions = KotlinFunctions(this@FindKotlinFunctions)

            override fun visitMethodDeclaration(
                method: J.MethodDeclaration,
                ctx: ExecutionContext,
            ): J.MethodDeclaration {
                val sourcePath = cursor.firstEnclosing(SourceFile::class.java)?.sourcePath?.toString() ?: ""
                val parameterCount = method.parameters.count { it !is J.Empty }
                functions.insertRow(ctx, KotlinFunctions.Row(sourcePath, method.simpleName, parameterCount))
                return super.visitMethodDeclaration(method, ctx)
            }
        }
}
