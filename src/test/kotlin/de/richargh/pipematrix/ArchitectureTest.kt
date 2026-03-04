package de.richargh.pipematrix

import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ArchUnitExtension::class)
class ArchitectureTest {

    companion object {
        private const val SLICES = "de.richargh.pipematrix.(*).."

        private const val APP = "..app.."
        private const val APP_EXPOSED = "..app.exposed.."
        private const val APP_HIDDEN = "..app.hidden.."
        private const val CONFIG = "..config.."
        private const val INFRASTRUCTURE = "..infrastructure.."
        private const val PRESENTATION = "..presentation.."

        private val STANDARD_LIBS = arrayOf(
            "java..",
            "kotlin..",
            "kotlinx..",
            "org.jetbrains.."  // Compiler-generated annotations
        )
    }

    @Test
    fun `slices should be free of cycles`(classes: JavaClasses) {
        slices()
            .matching(SLICES)
            .should().beFreeOfCycles()
            .check(classes)
    }

    @Test
    fun `app slice should not depend on outside slices`(classes: JavaClasses) {
        noClasses()
            .that().resideInAPackage(APP)
            .should().dependOnClassesThat().resideOutsideOfPackages(
                APP,
                "com.gitlab.api..",  // API models/DTOs are allowed
                *STANDARD_LIBS
            )
            .because("App slice must be isolated from other application slices")
            .check(classes)
    }

}
