/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite

import com.bosch.pt.iot.smartsite.project.exporter.model.tree.Tree
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS
import com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Validate coding policies, so that ")
internal class GenericCodingPolicyTest : AbstractCodingPolicyTest() {

  @Test
  fun `interfaces have names not containing Interface`() {
    noClasses()
        .that()
        .areInterfaces()
        .should()
        .haveSimpleNameContaining("Interface")
        .because("bad coding style")
        .check(sourceClasses)
  }

  @Test
  fun `no generic exceptions are thrown`() {
    NO_CLASSES_SHOULD_THROW_GENERIC_EXCEPTIONS.check(sourceClasses)
  }

  @Test
  fun `no one should access system stream (in and out)`() {
    val classes =
        ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            // AbstractNode has a
            .withImportOption { !it.contains(Tree::class.java.simpleName) }
            .importPackages("com.bosch.pt.iot.smartsite.")

    NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS.check(classes)
  }

  @Test
  fun `standard logging should not be used`() {
    NO_CLASSES_SHOULD_USE_JAVA_UTIL_LOGGING.check(sourceClasses)
  }
}
