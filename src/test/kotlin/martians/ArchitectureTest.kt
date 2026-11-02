package martians

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import io.kotest.core.spec.style.FunSpec

class ArchitectureTest : FunSpec({

    val productionClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("martians")

    test("the domain depends on nothing outside the domain, apart from the Kotlin and Java runtimes") {
        classes().that().resideInAPackage("martians.domain..")
            .should().onlyDependOnClassesThat()
            .resideInAnyPackage("martians.domain..", "kotlin..", "java..", "org.jetbrains.annotations..")
            .check(productionClasses)
    }
})
