/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement

import com.tngtech.archunit.core.domain.JavaClass.Predicates.resideOutsideOfPackage
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption.Predefined.DO_NOT_INCLUDE_TESTS
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.Architectures.layeredArchitecture
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices
import org.assertj.core.util.Strings
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("Validate architecture rules, so that ")
internal class PackageDependenciesTest : AbstractCodingPolicyTest() {

  private fun layerDefinition() =
      layeredArchitecture()
          .consideringOnlyDependenciesInLayers()
          .optionalLayer("FacadeJob")
          .definedBy("..facade.job..")
          .optionalLayer("FacadeRest")
          .definedBy("..facade.rest..")
          .optionalLayer("FacadeKafka")
          .definedBy("..facade.listener..")
          .optionalLayer("Boundary")
          .definedBy("..boundary..")
          .optionalLayer("Command")
          .definedBy("..command..")
          .optionalLayer("Query")
          .definedBy("..query..")
          .optionalLayer("Authorization")
          .definedBy("..authorization..")
          .optionalLayer("Control")
          .definedBy("..control..")
          .optionalLayer("Repository")
          .definedBy("..repository..")
          .optionalLayer("Model")
          .definedBy("..model..")

  @ParameterizedTest
  @MethodSource("functionalPackages")
  fun `facade job layer dependencies are respected for module`(packageName: String) {
    layerDefinition()
        .whereLayer("FacadeJob")
        .mayOnlyBeAccessedByLayers("FacadeRest")
        .`as`("Job facade layer can only be accessed by REST facade when REST API submits a job.")
        .check(getClassesByPackage(packageName))
  }

  @ParameterizedTest
  @MethodSource("functionalPackages")
  fun `facade listener layer dependencies are respected for module`(packageName: String) {
    layerDefinition()
        .whereLayer("FacadeKafka")
        .mayNotBeAccessedByAnyLayer()
        .`as`("No access to kafka facade layer")
        .check(getClassesByPackage(packageName))
  }

  @ParameterizedTest
  @MethodSource("functionalPackages")
  fun `boundary layer dependencies are respected for module`(packageName: String) {
    layerDefinition()
        .whereLayer("Boundary")
        .mayOnlyBeAccessedByLayers("FacadeRest", "FacadeKafka", "FacadeJob", "Command", "Query")
        .`as`("Access boundary layer only from facade layer, command layer or query layer")
        .check(getClassesByPackage(packageName))
  }

  @ParameterizedTest
  @MethodSource("functionalPackages")
  fun `command layer dependencies are respected for module`(packageName: String) {
    layerDefinition()
        .whereLayer("Command")
        .mayOnlyBeAccessedByLayers("FacadeRest", "FacadeKafka", "FacadeJob", "Boundary")
        .`as`(
            "Commands can be triggered from a facade only. " +
                "(From boundaries only for the transition phase to Arch 2.0")
        .check(getClassesByPackage(packageName))
  }

  @ParameterizedTest
  @MethodSource("functionalPackages")
  fun `query layer dependencies are respected for module`(packageName: String) {
    layerDefinition()
        .whereLayer("Query")
        .mayOnlyBeAccessedByLayers("FacadeRest", "FacadeKafka", "FacadeJob", "Boundary")
        .`as`(
            "Queries can be triggered from a facade only." +
                "(From boundaries only for the transition phase to Arch 2.0")
        .check(getClassesByPackage(packageName))
  }

  @ParameterizedTest
  @MethodSource("functionalPackages")
  fun `control layer dependencies are respected for module`(packageName: String) {
    layerDefinition()
        .whereLayer("Control")
        .mayOnlyBeAccessedByLayers("Boundary", "Command")
        .`as`("Access control layer only form boundary layer")
        .check(getClassesByPackage(packageName))
  }

  @ParameterizedTest
  @MethodSource("functionalPackages")
  fun `authorization layer dependencies are respected for module`(packageName: String) {
    layerDefinition()
        .whereLayer("Authorization")
        .mayOnlyBeAccessedByLayers("Command", "Query", "Boundary", "FacadeRest")
        .`as`("Access authorization layer only from boundary layer, command layer and facade layer")
        .check(getClassesByPackage(packageName))
  }

  @Test
  fun `module common has no outgoing dependencies to business modules`() {
    val packages = FUNCTIONAL_PACKAGES.map { p -> Strings.concat("..", p, "..") }.toTypedArray()

    noClasses()
        .that()
        .resideInAnyPackage("..usermanagement.common..")
        .and(resideOutsideOfPackage("..usermanagement.common.facade.rest.resource.."))
        .should()
        .accessClassesThat()
        .resideInAnyPackage(*packages)
        .check(sourceClasses)
  }

  // The modules are not free of cycles yet. Therefore, this test is disabled for now.
  @Disabled
  @Test
  fun `modules have no cyclic dependencies`() {
    slices().matching("..usermanagement.(*)..").should().beFreeOfCycles().check(sourceClasses)
  }

  private fun getClassesByPackage(packageName: String) =
      ClassFileImporter()
          .withImportOption(DO_NOT_INCLUDE_TESTS)
          .importPackages(PACKAGE_ROOT + packageName)!!

  companion object {
    private val FUNCTIONAL_PACKAGES = listOf("attachment", "company", "craft", "user")

    @JvmStatic fun functionalPackages() = FUNCTIONAL_PACKAGES
  }
}
