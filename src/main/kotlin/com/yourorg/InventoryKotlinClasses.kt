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
import org.openrewrite.ScanningRecipe
import org.openrewrite.SourceFile
import org.openrewrite.Tree
import org.openrewrite.TreeVisitor
import org.openrewrite.java.tree.J
import org.openrewrite.kotlin.KotlinIsoVisitor
import org.openrewrite.text.PlainText
import kotlin.io.path.Path

// -----------------------------------------------------------------------------
// Some recipes can't decide what to do from a single file — they need to survey
// the whole codebase first, then act. Those are scanning recipes, and they run
// in phases:
//
//   getInitialValue  -> create a fresh accumulator for the run
//   getScanner       -> visit every source, filling the accumulator
//   generate         -> emit brand-new source files from what was accumulated
//   getVisitor       -> (optional) modify existing files with the surveyed knowledge
//
// The `rewrite { } to { }` / `kotlin { visit… }` DSL covers pattern edits and
// single-pass imperative edits. When you need the scan-then-generate lifecycle,
// extend `ScanningRecipe<Accumulator>` directly — a plain Kotlin class, using
// the same `KotlinIsoVisitor` the DSL composes underneath.
//
// This recipe scans every Kotlin file for its class/interface/object
// declarations, then writes a single `kotlin-classes.txt` inventory listing
// them. `generate` creates that file when it is absent; `getVisitor` rewrites it
// when it is already present, so a stale inventory is brought up to date rather
// than left alone. Both paths render the same text from the same accumulator,
// and the visitor returns the source untouched when the text already matches —
// that identity check is what lets a second convergence cycle find nothing to do.
// -----------------------------------------------------------------------------

private val INVENTORY_PATH = Path("kotlin-classes.txt")

class InventoryKotlinClasses : ScanningRecipe<InventoryKotlinClasses.Accumulator>() {

    class Accumulator {
        val classNames: MutableSet<String> = mutableSetOf()
        var inventoryExists: Boolean = false

        fun renderInventory(): String = classNames.sorted().joinToString("\n")
    }

    override fun getDisplayName(): String = "Inventory Kotlin class declarations"

    override fun getDescription(): String =
        "Scans every Kotlin source for its class, interface, and object declarations and " +
            "generates a `kotlin-classes.txt` file listing their names."

    override fun getInitialValue(ctx: ExecutionContext): Accumulator = Accumulator()

    override fun getScanner(acc: Accumulator): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<SourceFile, ExecutionContext>() {
            override fun visit(tree: Tree?, ctx: ExecutionContext): SourceFile? {
                val sourceFile = tree as? SourceFile ?: return null
                if (sourceFile.sourcePath == INVENTORY_PATH) {
                    acc.inventoryExists = true
                }
                object : KotlinIsoVisitor<ExecutionContext>() {
                    override fun visitClassDeclaration(
                        classDeclaration: J.ClassDeclaration,
                        c: ExecutionContext,
                    ): J.ClassDeclaration {
                        acc.classNames.add(classDeclaration.name.simpleName)
                        return super.visitClassDeclaration(classDeclaration, c)
                    }
                }.visit(sourceFile, ctx)
                return sourceFile
            }
        }

    override fun generate(acc: Accumulator, ctx: ExecutionContext): Collection<SourceFile> {
        if (acc.inventoryExists || acc.classNames.isEmpty()) {
            return emptyList()
        }
        return listOf(
            PlainText.builder()
                .id(Tree.randomId())
                .sourcePath(INVENTORY_PATH)
                .text(acc.renderInventory())
                .build(),
        )
    }

    override fun getVisitor(acc: Accumulator): TreeVisitor<*, ExecutionContext> =
        object : TreeVisitor<SourceFile, ExecutionContext>() {
            override fun visit(tree: Tree?, ctx: ExecutionContext): SourceFile? {
                val sourceFile = tree as? SourceFile ?: return null
                if (sourceFile !is PlainText ||
                    sourceFile.sourcePath != INVENTORY_PATH ||
                    acc.classNames.isEmpty()
                ) {
                    return sourceFile
                }
                val inventory = acc.renderInventory()
                return if (sourceFile.text == inventory) sourceFile else sourceFile.withText(inventory)
            }
        }
}
