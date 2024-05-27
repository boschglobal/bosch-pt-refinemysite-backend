/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement

import com.bosch.pt.csm.cloud.usermanagement.application.security.AdminAuthorization
import com.bosch.pt.csm.cloud.usermanagement.application.security.NoPreAuthorize
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaMethodCall
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses
import java.util.Locale
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController

@DisplayName("Validate coding policies for boundary classes, so that ")
internal class BoundaryCodingPolicyTest : AbstractCodingPolicyTest() {

  @DisplayName("classes")
  @Nested
  internal inner class ServiceClassesCodingPolicyTest {

    @Test
    fun `should be named ending with Service`() {
      classes()
          .that(areServiceClasses())
          .should()
          .haveSimpleNameEndingWith("Service")
          .because("Services should be recognized easily")
          .check(sourceClasses)
    }

    @Test
    fun `should not be annotated with transactional`() {
      classes()
          .that(areServiceClasses())
          .should()
          .notBeAnnotatedWith(Transactional::class.java)
          .because("Service methods should be annotated explicitly")
          .check(sourceClasses)
    }

    @Test
    fun `internal service classes must not be referenced from a controller`() {
      noClasses()
          .that()
          .resideInAnyPackage("..rest..")
          .should()
          .accessClassesThat(areInternalServiceClasses())
          .check(sourceClasses)
    }
  }

  @DisplayName("methods")
  @Nested
  internal inner class ServiceMethodsCodingPolicyTest {
    @Test
    fun `should be transactional`() {
      all(methods())
          .that(belongToAServiceClass())
          .and(arePublic())
          .should(haveAnnotationOfType(Transactional::class.java))
          .because("The transactional limit is defined here")
          .check(sourceClasses)
    }

    @Test
    fun `should be annotated with an authorization annotation`() {
      all(methods())
          .that(belongToAServiceClass())
          .and(arePublic())
          .should(
              haveAnnotationOfType(NoPreAuthorize::class.java)
                  .or(haveAnnotationOfType(PreAuthorize::class.java))
                  .or(haveAnnotationOfType(AdminAuthorization::class.java)))
          .because("Public transactions should be authorized")
          .check(sourceClasses)
    }

    @Test
    fun `with @NoPreAuthorize should not be called by a controller`() {
      all(methods())
          .that(belongToAServiceClass())
          .and(annotatedWithNoPreAuthorize())
          .should(notBeCalledByControllerClass())
          .because("we don't want to expose resources without authorization")
          .check(sourceClasses)
    }
  }

  fun areServiceClasses() =
      object :
          DescribedPredicate<JavaClass>(
              "class residing in package boundary and annotated with service") {
        override fun test(input: JavaClass) = isServiceClass(input)
      }

  private fun areInternalServiceClasses() =
      object :
          DescribedPredicate<JavaClass>("service class that must not be used from the outside") {
        private val internalServiceClasses = mutableSetOf("employeeservice, companyservice")

        override fun test(input: JavaClass) =
            internalServiceClasses.contains(input.simpleName.lowercase(Locale.getDefault()))
      }

  fun belongToAServiceClass() =
      object : DescribedPredicate<JavaMethod>("method belongs to a service") {
        override fun test(input: JavaMethod) = isServiceClass(input.owner)
      }

  fun annotatedWithNoPreAuthorize() =
      object : DescribedPredicate<JavaMethod>("annotated with @NoPreAuthorized") {
        override fun test(input: JavaMethod) =
            (input.isAnnotatedWith(NoPreAuthorize::class.java) &&
                !input.getAnnotationOfType(NoPreAuthorize::class.java).usedByController)
      }

  fun notBeCalledByControllerClass() =
      object : ArchCondition<JavaMethod>("not be called by a controller") {
        override fun check(method: JavaMethod, events: ConditionEvents) {
          val conditionSatisfied =
              method.callsOfSelf.stream().allMatch { call: JavaMethodCall ->
                !call.originOwner.isAnnotatedWith(RestController::class.java)
              }
          events.add(
              SimpleConditionEvent(
                  method,
                  conditionSatisfied,
                  "${method.name} in ${method.owner.name} is called by a controller "))
        }
      }

  // Conditions that can be reused
  fun isServiceClass(input: JavaClass) =
      (input.getPackage().name.lowercase().contains(".boundary") &&
          input.isAnnotatedWith(Service::class.java))
}
