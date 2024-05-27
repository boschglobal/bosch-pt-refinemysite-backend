/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition
import org.assertj.core.util.Strings
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Validate architecture rules, so that ")
internal class ArchitectureValidationTest {

  private val layerDefinition =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("Facade")
          .definedBy("..facade..")
          .layer("Boundary")
          .definedBy("..boundary..")
          .layer("Repository")
          .definedBy("..repository..")
          .layer("Model")
          .definedBy("..model..")

  private val layerAccess =
      layerDefinition
          .whereLayer("Facade")
          .mayNotBeAccessedByAnyLayer()
          .`as`("No access to facade layer")
          .whereLayer("Boundary")
          .mayOnlyBeAccessedByLayers("Facade")
          .`as`("Access boundary layer only from Facade layer")
          .whereLayer("Repository")
          .mayOnlyBeAccessedByLayers("Boundary")
          .`as`("Access repository layer only from boundary layer")

  @Test
  @DisplayName("layer dependencies are respected")
  fun testLayerDependenciesCrossModules() {
    val classes =
        ClassFileImporter()
            .withImportOption(DO_NOT_INCLUDE_TESTS)
            .importPackages(
                FUNCTIONAL_PACKAGES.map { p -> Strings.concat(PACKAGE_ROOT, p) }.toSet())

    layerAccess.check(classes)
  }

  @Test
  @DisplayName("module common has no outgoing dependencies to business modules")
  fun testCommonModuleDependencies() {
    val classes =
        ClassFileImporter().withImportOption(DO_NOT_INCLUDE_TESTS).importPackages(PACKAGE_ROOT)

    val packages = FUNCTIONAL_PACKAGES.map { p -> Strings.concat("..", p, "..") }.toTypedArray()

    val commonModule =
        noClasses()
            .that()
            .resideInAPackage("..projectmanagement.common..")
            .should()
            .accessClassesThat()
            .resideInAnyPackage(*packages)

    commonModule.check(classes)
  }

  // We do have cycles currently
  @Disabled
  @Test
  @DisplayName("modules have no cyclic dependencies")
  fun testCyclicDependencies() {
    val classes =
        ClassFileImporter().withImportOption(DO_NOT_INCLUDE_TESTS).importPackages(PACKAGE_ROOT)

    val slices =
        SlicesRuleDefinition.slices()
            .matching("..projectmanagement.(*)..")
            .should()
            .beFreeOfCycles()

    slices.check(classes)
  }

  companion object {

    private const val PACKAGE_ROOT = "com.bosch.pt.csm.cloud.projectmanagement."

    private val FUNCTIONAL_PACKAGES = arrayOf("company", "event", "notification", "project", "user")
  }
}
