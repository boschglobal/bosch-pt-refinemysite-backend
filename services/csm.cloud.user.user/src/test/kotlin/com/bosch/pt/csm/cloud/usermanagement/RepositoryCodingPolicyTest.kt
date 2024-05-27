/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement

import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.jpa.repository.JpaRepository

@DisplayName("Validate coding policies for repository classes, so that repositories ")
internal class RepositoryCodingPolicyTest : AbstractCodingPolicyTest() {

  @Test
  fun `should be named ending with Repository`() {
    classes()
        .that()
        .areAssignableTo(JpaRepository::class.java)
        .should()
        .haveSimpleNameEndingWith("Repository")
        .because("repositories should be recognized easily")
        .check(sourceClasses)
  }
}
