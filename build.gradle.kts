plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.test) apply false
    alias(libs.plugins.baselineprofile) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
}

// Enable the tracked pre-commit hook on every sync/build; idempotent, warns outside a git checkout.
val hooksEnabled = runCatching {
    providers.exec {
        workingDir(rootDir)
        commandLine("git", "config", "core.hooksPath", ".githooks")
        isIgnoreExitValue = true
    }.result.get().exitValue == 0
}.getOrDefault(false)

if (!hooksEnabled) {
    logger.warn("Could not enable git hooks; run `git config core.hooksPath .githooks` manually.")
}

// ktlint without a plugin, as documented at https://pinterest.github.io/ktlint/latest/install/integrations/
// Rules live in .editorconfig so the IDE and ktlint agree.
val ktlint: Configuration by configurations.creating

dependencies {
    ktlint(libs.ktlint.cli) {
        attributes { attribute(Bundling.BUNDLING_ATTRIBUTE, objects.named(Bundling.EXTERNAL)) }
    }
}

val ktlintSources = listOf("**/src/**/*.kt", "**/*.gradle.kts", "!**/build/**")

tasks.register<JavaExec>("ktlintCheck") {
    group = "verification"
    description = "Check Kotlin code style."
    classpath = ktlint
    mainClass = "com.pinterest.ktlint.Main"
    args(ktlintSources)
}

tasks.register<JavaExec>("ktlintFormat") {
    group = "formatting"
    description = "Fix Kotlin code style violations."
    classpath = ktlint
    mainClass = "com.pinterest.ktlint.Main"
    // Needed on JDK 16+ for ktlint's IntelliJ-based parser.
    jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
    args("-F", *ktlintSources.toTypedArray())
}
