/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement

import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
import com.tngtech.archunit.lang.ArchRule
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class ArchitectureValidationTest {

  private val layerDefinition =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .layer("API")
          .definedBy("..api..", "..facade..")
          .layer("Boundary")
          .definedBy("..boundary..")
          .layer("Authorization")
          .definedBy("..authorization..")
          .layer("Repository")
          .definedBy("..repository..")
          .layer("Model")
          .definedBy("..model..")

  private val layerAccess: ArchRule =
      layerDefinition
          .whereLayer("API")
          .mayNotBeAccessedByAnyLayer()
          .`as`("No access to api layer")
          .whereLayer("Boundary")
          .mayOnlyBeAccessedByLayers("API")
          .`as`("Access boundary layer only from API layer")
          .whereLayer("Authorization")
          .mayOnlyBeAccessedByLayers("Boundary", "API")
          .`as`("Access authorization layer only from boundary layer and api layer")
          .whereLayer("Repository")
          .mayOnlyBeAccessedByLayers("Boundary", "Authorization")
          .`as`("Access repository layer only from boundary and authorization layer")

  @ParameterizedTest
  @ValueSource(strings = ["statistics"])
  @DisplayName("Verify layer dependencies are respected for module")
  fun testLayerDependencies(packageName: String) {
    val classes =
        ClassFileImporter()
            .withImportOption(DO_NOT_INCLUDE_TESTS)
            .importPackages(PACKAGE_ROOT + packageName)

    layerAccess.check(classes)
  }

  @Test
  @DisplayName("Verify module common has no outgoing dependencies to business modules")
  fun testCommonModuleDependencies() {
    val classes =
        ClassFileImporter().withImportOption(DO_NOT_INCLUDE_TESTS).importPackages(PACKAGE_ROOT)

    val commonModule =
        noClasses()
            .that()
            .resideInAPackage("..common..")
            .should()
            .accessClassesThat()
            .resideInAnyPackage("..statistics..")

    commonModule.check(classes)
  }

  // The modules are not free of cycles yet. Therefore, this test is disabled for now.
  @Disabled
  @Test
  @DisplayName("Verify modules have no cyclic dependencies")
  fun testCyclicDependencies() {
    val classes =
        ClassFileImporter().withImportOption(DO_NOT_INCLUDE_TESTS).importPackages(PACKAGE_ROOT)

    val slices = slices().matching("..projectmanagement.(*)..").should().beFreeOfCycles()

    slices.check(classes)
  }

  companion object {
    private const val PACKAGE_ROOT = "com.bosch.pt.csm.cloud.projectmanagement."
  }
}
