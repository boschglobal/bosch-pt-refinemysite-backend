/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application

import java.lang.reflect.AnnotatedElement
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy
import org.springframework.test.context.ContextCustomizer
import org.springframework.test.context.MergedContextConfiguration

class SmartSiteSpringBootTestArgs(testClass: AnnotatedElement) : ContextCustomizer {

  val args: Array<String?> =
      MergedAnnotations.from(testClass, SearchStrategy.TYPE_HIERARCHY)
          .get(SmartSiteSpringBootTest::class.java)
          .getValue("args", Array<String?>::class.java)
          .orElse(NO_ARGS)

  override fun customizeContext(
      context: ConfigurableApplicationContext,
      mergedConfig: MergedContextConfiguration
  ) = Unit

  override fun equals(other: Any?) =
      (other != null &&
          javaClass == other.javaClass &&
          args.contentEquals((other as SmartSiteSpringBootTestArgs).args))

  override fun hashCode() = args.contentHashCode()

  companion object {

    private val NO_ARGS = arrayOfNulls<String>(size = 0)

    /**
     * Return the application arguments from the given customizers.
     *
     * @param customizers the customizers to check
     * @return the application args or an empty array
     */
    operator fun get(customizers: Set<ContextCustomizer?>): Array<String?> {
      for (customizer in customizers) if (customizer is SmartSiteSpringBootTestArgs)
          return customizer.args
      return NO_ARGS
    }
  }
}
