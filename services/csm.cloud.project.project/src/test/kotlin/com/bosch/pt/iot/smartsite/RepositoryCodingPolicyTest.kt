/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite

import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepository
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("Validate coding policies for repository classes, so that repositories ")
internal class RepositoryCodingPolicyTest : AbstractCodingPolicyTest() {

  @Test
  fun `should be named ending with Repository`() {
    classes()
        .that()
        .areAssignableTo(KafkaStreamableRepository::class.java)
        .and() // kafka.streamable.KafkaStreamableRepositoryImpl doesn't end with 'Repository'
        .haveSimpleNameNotContaining("KafkaStreamableRepositoryImpl")
        .should()
        .haveSimpleNameEndingWith("Repository")
        .because("repositories should be recognized easily")
        .check(sourceClasses)
  }
}
