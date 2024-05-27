/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import java.util.Locale
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Validate coding policies for repository classes, so that repositories ")
internal class RepositoryCodingPolicyTest : AbstractCodingPolicyTest() {

  @Test
  fun `can only be accessed by certain packages`() {
    classes()
        .that(areRepositoryClasses())
        .should()
        .onlyBeAccessed()
        .byAnyPackage(
            "..repository..",
            "..authorization..",
            "..boundary..",
            "..query..",
            "..strategy..",
            "..metrics..",
            "..command..")
        .check(sourceClasses)
  }

  @Test
  fun `should not access higher layers`() {
    noClasses()
        .that(areRepositoryClasses())
        .should()
        .accessClassesThat()
        .resideInAnyPackage("..facade..", "..boundary..")
        .because("repositories should not depend on higher layers")
        .check(sourceClasses)
  }

  private fun areRepositoryClasses() =
      object : DescribedPredicate<JavaClass>("residing in package repository") {
        override fun test(input: JavaClass) =
            (input.getPackage().name.lowercase(Locale.getDefault()).contains(".repository") &&
                !input.getPackage().name.lowercase(Locale.getDefault()).endsWith(".dto"))
      }
}
