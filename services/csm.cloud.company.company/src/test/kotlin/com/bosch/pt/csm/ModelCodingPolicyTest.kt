/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */ package com.bosch.pt.csm

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.csm.cloud.common.eventstore.AbstractKafkaEvent
import com.bosch.pt.csm.common.model.LocalEntity
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaConstructor
import com.tngtech.archunit.core.domain.SourceCodeLocation
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import jakarta.persistence.Entity
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("Validate coding policies for model classes, so that ")
internal class ModelCodingPolicyTest : AbstractCodingPolicyTest() {

  @DisplayName("classes")
  @Nested
  internal inner class ModelClassesCodingPolicyTest {

    @Disabled("needs rework once the refactoring to arch 2.0 are done")
    @Test
    fun `are snapshots, kafka streamable, kafka event or data entity`() {
      classes()
          .that(areModelClasses())
          .should()
          .beAssignableTo(AbstractSnapshotEntity::class.java)
          .orShould()
          .beAssignableTo(AbstractKafkaEvent::class.java)
          .orShould()
          .beAssignableTo(LocalEntity::class.java)
          .check(sourceClasses)
    }

    @Test
    fun `should have at least one constructor with no parameters`() {
      classes()
          .that(areModelClasses())
          .should(haveAtLeastZeroParameterConstructor())
          .because("of JPA")
          .check(sourceClasses)
    }
  }

  // DescribePredicates
  fun areModelClasses() =
      object : DescribedPredicate<JavaClass>("class annotated with entity") {
        override fun test(input: JavaClass) = input.isAnnotatedWith(Entity::class.java)
      }

  // ArchConditions
  fun haveAtLeastZeroParameterConstructor() =
      object : ArchCondition<JavaClass>("constructor with 0 parameters") {
        override fun check(item: JavaClass, events: ConditionEvents) {
          val conditionSatisfied =
              item.allConstructors.stream().anyMatch { constructor: JavaConstructor ->
                constructor.reflect().parameterCount == 0
              }
          events.add(
              SimpleConditionEvent(
                  item,
                  conditionSatisfied,
                  "${item.simpleName} has no constructor with 0 parameters in ${SourceCodeLocation.of(item, 0)}"))
        }
      }
}
