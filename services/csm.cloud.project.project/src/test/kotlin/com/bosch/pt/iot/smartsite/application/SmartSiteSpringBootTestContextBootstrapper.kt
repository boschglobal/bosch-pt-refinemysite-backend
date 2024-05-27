/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application

import com.bosch.pt.csm.cloud.common.test.context.RelaxedCacheAwareContextLoaderDelegate
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest.WebEnvironment.DEFINED_PORT
import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy.INHERITED_ANNOTATIONS
import org.springframework.core.annotation.MergedAnnotations.from
import org.springframework.test.context.CacheAwareContextLoaderDelegate
import org.springframework.test.context.web.WebAppConfiguration

class SmartSiteSpringBootTestContextBootstrapper : SpringBootTestContextBootstrapper() {

  override fun getDefaultContextLoaderClass(testClass: Class<*>) =
      SmartSiteSpringBootContextLoader::class.java

  override fun getCacheAwareContextLoaderDelegate(): CacheAwareContextLoaderDelegate =
      RelaxedCacheAwareContextLoaderDelegate.getInstance()

  override fun getWebEnvironment(testClass: Class<*>): WebEnvironment? {
    val environmentName = getTestAnnotation(testClass)?.webEnvironment?.name
    return environmentName?.let { WebEnvironment.valueOf(environmentName) }
  }

  override fun getClasses(testClass: Class<*>) =
      getTestAnnotation(testClass)?.classes?.map { it.java }?.toTypedArray()

  override fun getProperties(testClass: Class<*>) = getTestAnnotation(testClass)?.properties

  private fun getTestAnnotation(testClass: Class<*>) =
      from(testClass, INHERITED_ANNOTATIONS)[SmartSiteSpringBootTest::class.java]
          .synthesize { it.isPresent }
          .orElse(null)

  override fun verifyConfiguration(testClass: Class<*>) {
    val springBootTest = getTestAnnotation(testClass)
    check(
        !(springBootTest != null &&
            isListeningOnPort(springBootTest.webEnvironment) &&
            from(testClass, INHERITED_ANNOTATIONS).isPresent(WebAppConfiguration::class.java))) {
      ("@WebAppConfiguration should only be used " +
          "with @SpringBootTest when @SpringBootTest is configured with a " +
          "mock web environment. Please remove @WebAppConfiguration or reconfigure @SpringBootTest.")
    }
  }

  private fun isListeningOnPort(webEnvironment: SmartSiteSpringBootTest.WebEnvironment) =
      (webEnvironment === DEFINED_PORT || webEnvironment === RANDOM_PORT)
}
