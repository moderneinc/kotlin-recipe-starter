# kotlin-recipe-starter

A template for authoring [OpenRewrite](https://docs.openrewrite.org/) recipes for **Kotlin**, using
the Kotlin recipe DSL. Click **Use this template** on GitHub, rename the `com.yourorg` group to your
own, and start writing recipes.

For a walkthrough of the DSL and the ideas behind it, read
[Kotlin recipes for OpenRewrite](https://moderne.ai/blog/kotlin-recipes-for-openrewrite). For a large,
production example, see [`moderneinc/recipes-kotlin`](https://github.com/moderneinc/recipes-kotlin).

## Why a separate Kotlin starter?

The Kotlin LST *extends* the Java LST, so an ordinary Java or declarative-YAML recipe already runs
against Kotlin sources — that path lives in
[`rewrite-recipe-starter`](https://github.com/moderneinc/rewrite-recipe-starter), and
[`src/main/resources/META-INF/rewrite/rewrite.yml`](src/main/resources/META-INF/rewrite/rewrite.yml)
shows a declarative recipe doing exactly that here.

What this repo adds is the **Kotlin recipe DSL**: a K2 compiler plugin, shipped inside `rewrite-kotlin`,
that turns `rewrite { } to { }` before/after lambdas into recipes at compile time. That plugin needs a
dedicated Gradle + Kotlin build, which is what this template wires up. See
[`build.gradle.kts`](build.gradle.kts) — the `kotlinCompilerPluginClasspath("org.openrewrite:rewrite-kotlin")`
line is the piece that enables the DSL.

## Prerequisites

- JDK 21 (`rewrite-kotlin`'s parser is built and validated on JDK 21 — an `.sdkmanrc` is included)
- Gradle is provided via the wrapper (`./gradlew`)

## The three ways to write a Kotlin recipe

### 1. Declarative DSL — `rewrite { } to { }` (the common case)

Most Kotlin recipes are pattern-shaped: a before expression and an after expression. See
[`UseModernKotlinApis.kt`](src/main/kotlin/com/yourorg/UseModernKotlinApis.kt).

```kotlin
val UseUppercase: Recipe = recipe(
    displayName = "Use `uppercase()` instead of `toUpperCase()`",
    description = "`String.toUpperCase()` was deprecated in Kotlin 1.5 ...",
) {
    edit {
        rewrite { s: String -> s.toUpperCase() } to { s -> s.uppercase() }
    }
}
```

`recipes(...)` composes several of these into one runnable recipe.

### 2. Imperative — `kotlin { visit… }` (when you need LST context)

When a change needs cursor context, annotation inspection, or conditional logic, drop into the
imperative visitor scope. The DSL composes with this `KotlinVisitor` underneath, not in place of it.
See [`FindPrintlnCalls.kt`](src/main/kotlin/com/yourorg/FindPrintlnCalls.kt), which flags
`println`/`print` calls but uses the cursor to skip the ones inside a `fun main` — a "where does the
call sit" decision that a `rewrite { } to { }` pattern cannot express.

```kotlin
val FindPrintlnCalls: Recipe = recipe(/* ... */) {
    edit {
        // Unlike `rewrite { } to { }`, an imperative visitor is not wrapped in a
        // `UsesMethod` precondition automatically — add one with `check(...)` so files
        // that never call println/print are skipped before the LST is walked.
        check(
            or(usesMethod(PRINTLN_SPEC), usesMethod(PRINT_SPEC)),
            kotlin {
                visitMethodInvocation { mi ->
                    if (!PRINTLN_MATCHER.matches(mi) && !PRINT_MATCHER.matches(mi)) {
                        return@visitMethodInvocation mi
                    }
                    val enclosing = cursor.firstEnclosing(J.MethodDeclaration::class.java)
                    if (enclosing?.simpleName == "main") return@visitMethodInvocation mi
                    SearchResult.found(mi, "...") ?: mi
                }
            },
        )
    }
}
```

### 3. Declarative YAML (Java/type-based recipes, which also apply to Kotlin)

Compose existing recipes in
[`src/main/resources/META-INF/rewrite/rewrite.yml`](src/main/resources/META-INF/rewrite/rewrite.yml),
or build them visually at [app.moderne.io/recipes/builder](https://app.moderne.io/recipes/builder).

## Data tables

A recipe can emit structured rows (exported as CSV / surfaced on the Moderne platform) instead of, or
alongside, changing code. Inserting a row needs the `ExecutionContext`, which a visit method receives
directly — so data-table recipes are written as a full `Recipe` with a `KotlinIsoVisitor` rather than
through the `kotlin { visitX { node -> … } }` DSL sugar, whose lambda exposes the node but not the
context. See [`FindKotlinFunctions.kt`](src/main/kotlin/com/yourorg/FindKotlinFunctions.kt), which
records the name and parameter count of every Kotlin function declaration.

## Build and test

```bash
./gradlew build
```

Tests use `RewriteTest` with the `kotlin(...)` source helper from `rewrite-kotlin`; see the
[`src/test`](src/test/kotlin/com/yourorg) directory. Because pattern-mode rewrites update an
invocation's name but not its attached `JavaType.Method`, the tests set
`TypeValidation.none()` — this is expected and does not affect whether the rewrite fired.

## Run your recipes against a codebase

Publish the recipe module to your local Maven repository, then run it with the
[Moderne CLI](https://docs.moderne.io/user-documentation/moderne-cli/getting-started/cli-intro) or the
[rewrite build plugin](https://docs.openrewrite.org/running-recipes):

```bash
./gradlew publishToMavenLocal
```

## License

Licensed under the [Apache License, Version 2.0](LICENSE).
