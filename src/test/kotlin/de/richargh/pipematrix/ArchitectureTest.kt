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
        private const val CONFIG_HIDDEN = "..config.hidden.."
        private const val INFRASTRUCTURE = "..infrastructure.."
        private const val INFRASTRUCTURE_HIDDEN = "..infrastructure.hidden.."
        private const val PRESENTATION = "..presentation.."
        private const val PRESENTATION_HIDDEN = "..presentation.hidden.."

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

    @Test
    fun `hidden packages in any slice are only accessible from that slice`(classes: JavaClasses) {
        // Check each slice's hidden package
        listOf(
            APP to APP_HIDDEN,
            CONFIG to CONFIG_HIDDEN,
            INFRASTRUCTURE to INFRASTRUCTURE_HIDDEN,
            PRESENTATION to PRESENTATION_HIDDEN
        ).forEach { (slicePackage, hiddenPackage) ->
            noClasses()
                .that().resideOutsideOfPackage(slicePackage)
                .should().dependOnClassesThat().resideInAPackage(hiddenPackage)
                .because("Hidden package '$hiddenPackage' should only be accessible from '$slicePackage'")
                .check(classes)
        }
    }

}
