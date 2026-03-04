package de.richargh.pipematrix

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.junit.jupiter.api.extension.ParameterContext
import org.junit.jupiter.api.extension.ParameterResolver

/**
 * JUnit 5 Extension for ArchUnit tests.
 *
 * Loads JavaClasses once before all tests and makes them available via parameter injection.
 *
 * Usage:
 * ```kotlin
 * @ExtendWith(ArchUnitExtension::class)
 * class MyArchTest {
 *     @Test
 *     fun `my rule`(classes: JavaClasses) {
 *         // use classes
 *     }
 * }
 * ```
 */
class ArchUnitExtension : BeforeAllCallback, ParameterResolver {

    companion object {
        private const val CLASSES_KEY = "archunit.classes"
        private const val BASE_PACKAGE = "de.richargh.pipematrix"
    }

    override fun beforeAll(context: ExtensionContext) {
        val classes = ClassFileImporter()
            .withImportOption(ImportOption.DoNotIncludeTests())
            .importPackages(BASE_PACKAGE)

        getStore(context).put(CLASSES_KEY, classes)
    }

    override fun supportsParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Boolean {
        return parameterContext.parameter.type == JavaClasses::class.java
    }

    override fun resolveParameter(
        parameterContext: ParameterContext,
        extensionContext: ExtensionContext
    ): Any {
        return getStore(extensionContext).get(CLASSES_KEY, JavaClasses::class.java)
            ?: throw IllegalStateException("JavaClasses not initialized")
    }

    private fun getStore(context: ExtensionContext): ExtensionContext.Store {
        return context.getStore(ExtensionContext.Namespace.create(javaClass))
    }
}
