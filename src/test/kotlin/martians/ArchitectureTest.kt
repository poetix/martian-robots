package martians

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import io.kotest.core.spec.style.FunSpec

class ArchitectureTest : FunSpec({

    val productionClasses = ClassFileImporter()
        .withImportOption(ImportOption.DoNotIncludeTests())
        .importPackages("martians")

    val runtime = arrayOf("kotlin..", "java..", "org.jetbrains.annotations..")

    fun packageDependsOnlyOn(subject: String, vararg allowed: String) =
        classes().that().resideInAPackage(subject)
            .should().onlyDependOnClassesThat().resideInAnyPackage(subject, *allowed, *runtime)
            .check(productionClasses)

    test("the domain depends on nothing else in the project") {
        packageDependsOnlyOn("martians.domain..")
    }

    test("commands depend only on the domain") {
        packageDependsOnlyOn("martians.commands..", "martians.domain..")
    }

    test("parsing depends only on the domain") {
        packageDependsOnlyOn("martians.parsing..", "martians.domain..")
    }

    test("running depends only on the domain") {
        packageDependsOnlyOn("martians.running..", "martians.domain..")
    }
})
