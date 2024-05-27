/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement

import com.tngtech.archunit.base.DescribedPredicate
import com.tngtech.archunit.core.domain.JavaClass
import com.tngtech.archunit.core.domain.JavaClasses
import com.tngtech.archunit.core.domain.JavaMethod
import com.tngtech.archunit.core.domain.JavaModifier
import com.tngtech.archunit.core.domain.SourceCodeLocation
import com.tngtech.archunit.core.importer.ClassFileImporter
import com.tngtech.archunit.core.importer.ImportOption
import com.tngtech.archunit.lang.AbstractClassesTransformer
import com.tngtech.archunit.lang.ArchCondition
import com.tngtech.archunit.lang.ConditionEvents
import com.tngtech.archunit.lang.SimpleConditionEvent
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach

open class AbstractCodingPolicyTest {

  private lateinit var softly: SoftAssertions

  @BeforeEach
  fun beforeEach() {
    softly = SoftAssertions()
  }

  @AfterEach fun afterEach() = softly.assertAll()

  // Transformer
  fun methods() =
      object : AbstractClassesTransformer<JavaMethod>("methods") {
        override fun doTransform(javaClasses: JavaClasses) =
            javaClasses
                .map { obj: JavaClass -> obj.methods }
                .flatten()
                .filter { method -> !method.name.contains("\$default") }
                .filter { method -> !method.name.contains("get") }
      }

  fun arePublic() =
      object : DescribedPredicate<JavaMethod>("method has modifier public") {
        override fun test(input: JavaMethod) = input.modifiers.contains(JavaModifier.PUBLIC)
      }

  fun haveMethodReturnType(type: Class<*>) =
      object : ArchCondition<JavaMethod>("return type " + type.name) {
        override fun check(method: JavaMethod, events: ConditionEvents) {
          val typeMatches = method.rawReturnType.isAssignableTo(type)
          val message =
              "${method.fullName} returns ${method.rawReturnType.name} in ${SourceCodeLocation.of(method.owner, 0)}"
          events.add(SimpleConditionEvent(method, typeMatches, message))
        }
      }

  fun haveAnnotationOfType(type: Class<out Annotation>) = checkAnnotationArchCondition(type, true)

  fun notHaveAnnotationOfType(type: Class<out Annotation>) =
      checkAnnotationArchCondition(type, false)

  private fun checkAnnotationArchCondition(type: Class<out Annotation>, matches: Boolean) =
      object : ArchCondition<JavaMethod>("not annotated with " + type.name) {
        override fun check(method: JavaMethod, events: ConditionEvents) {
          val typeMatches =
              if (matches) method.isAnnotatedWith(type) else !method.isAnnotatedWith(type)
          val message =
              "${method.fullName} is annotated with ${type.simpleName} in ${SourceCodeLocation.of(method.owner, 0)}"
          events.add(SimpleConditionEvent(method, typeMatches, message))
        }
      }

  companion object {

    const val PACKAGE_ROOT = "com.bosch.pt.csm.cloud.usermanagement."
    lateinit var sourceClasses: JavaClasses

    @JvmStatic
    @BeforeAll
    fun beforeAll() {
      sourceClasses =
          ClassFileImporter()
              .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
              .importPackages("com.bosch.pt.csm.cloud.usermanagement.")
    }
  }
}
