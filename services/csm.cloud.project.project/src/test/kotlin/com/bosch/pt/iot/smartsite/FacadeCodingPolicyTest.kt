/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.all
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.ResponseEntity
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.RestController

@DisplayName("Validate coding policies for facade classes, so that ")
internal class FacadeCodingPolicyTest : AbstractCodingPolicyTest() {

  @DisplayName("classes")
  @Nested
  internal inner class ControllerClassesCodingPolicyTest {
    @Test
    fun `should be placed into an api package`() {
      classes()
          .that(areControllerClasses())
          .should()
          .resideInAnyPackage("..facade.rest..")
          .check(sourceClasses)
    }

    @Test
    fun `should be named ending with Controller`() {
      classes()
          .that()
          .areAnnotatedWith(RestController::class.java)
          .should()
          .haveNameMatching(".*Controller(V[0-9])?")
          .because("Controllers should be recognized easily")
          .check(sourceClasses)
    }

    @Test
    fun `should be annotated with @RestController`() {
      classes()
          .that(areControllerClasses())
          .and()
          .doNotHaveSimpleName("ErrorController")
          .should()
          .beAnnotatedWith(RestController::class.java)
          .because("it works like this")
          .check(sourceClasses)
    }

    @Test
    fun `should be annotated with @ApiVersion`() {
      classes()
          .that(areControllerClasses())
          .and()
          .doNotHaveSimpleName("ErrorController")
          .should()
          .beAnnotatedWith(ApiVersion::class.java)
          .because("otherwise versioning is not applied properly")
          .check(sourceClasses)
    }

    @Test
    @DisplayName("should not be annotated as transactional")
    fun `should not be annotated as transactional`() {
      classes()
          .that(areControllerClasses())
          .should()
          .notBeAnnotatedWith(Transactional::class.java)
          .because("boundary services are transactional, not controllers")
          .check(sourceClasses)
    }
  }

  @DisplayName("methods")
  @Nested
  internal inner class ControllerMethodsCodingPolicyTest {

    @Test
    fun `should not have methods annotated with @transactional`() {
      all(methods())
          .that(belongToAControllerClass())
          .should(notHaveAnnotationOfType(Transactional::class.java))
          .because("boundary services are transactional, not controllers")
          .check(sourceClasses)
    }

    @Test
    fun `should always return responseEntities`() {
      all(methods())
          .that(belongToAControllerClass())
          .and(arePublic())
          .should(haveMethodReturnType(ResponseEntity::class.java))
          .because("we don't want to directly output resources")
          .check(sourceClasses)
    }
  }

  fun areControllerClasses() =
      object : DescribedPredicate<JavaClass>("class is a controller") {
        override fun test(input: JavaClass): Boolean = isControllerClass(input)
      }

  fun belongToAControllerClass() =
      object : DescribedPredicate<JavaMethod>("method belongs to a controller") {
        override fun test(input: JavaMethod): Boolean = isControllerClass(input.owner)
      }

  fun isControllerClass(input: JavaClass) =
      (input.isAnnotatedWith(RestController::class.java) &&
          input.simpleName.endsWith("Controller") &&
          !input.simpleName.endsWith("ErrorController") &&
          !input.simpleName.endsWith("CalendarExportController") &&
          !input.simpleName.endsWith("CalendarExportV3Controller") &&
          !input.simpleName.endsWith("CalendarExportV1Controller"))
}
