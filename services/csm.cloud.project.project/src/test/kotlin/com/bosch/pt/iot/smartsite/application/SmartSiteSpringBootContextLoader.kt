/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application

import com.bosch.pt.iot.smartsite.application.SmartSiteSpringBootTest.WebEnvironment
import org.springframework.boot.ApplicationContextFactory
import org.springframework.boot.SpringApplication
import org.springframework.boot.WebApplicationType
import org.springframework.boot.WebApplicationType.NONE
import org.springframework.boot.WebApplicationType.REACTIVE
import org.springframework.boot.WebApplicationType.SERVLET
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent
import org.springframework.boot.test.context.ReactiveWebMergedContextConfiguration
import org.springframework.boot.test.context.SpringBootContextLoader
import org.springframework.boot.test.mock.web.SpringBootMockServletContext
import org.springframework.boot.test.util.TestPropertyValues
import org.springframework.boot.web.reactive.context.GenericReactiveWebApplicationContext
import org.springframework.boot.web.servlet.support.ServletContextApplicationContextInitializer
import org.springframework.context.ApplicationContext
import org.springframework.context.ApplicationContextInitializer
import org.springframework.context.ApplicationListener
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.PriorityOrdered
import org.springframework.core.SpringVersion
import org.springframework.core.annotation.MergedAnnotations
import org.springframework.core.annotation.MergedAnnotations.SearchStrategy
import org.springframework.core.env.ConfigurableEnvironment
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.test.context.MergedContextConfiguration
import org.springframework.test.context.support.TestPropertySourceUtils
import org.springframework.test.context.web.WebMergedContextConfiguration
import org.springframework.util.Assert
import org.springframework.web.context.ConfigurableWebApplicationContext
import org.springframework.web.context.support.GenericWebApplicationContext

class SmartSiteSpringBootContextLoader : SpringBootContextLoader() {

  override fun loadContext(config: MergedContextConfiguration): ApplicationContext {
    val configClasses = config.classes
    val configLocations = config.locations

    Assert.state(configClasses.isNotEmpty() || configLocations.isNotEmpty()) {
      "No configuration classes or locations found in @SpringApplicationConfiguration. " +
          "For default configuration detection to work you need Spring 4.0.3 or better (found " +
          SpringVersion.getVersion() +
          ")."
    }

    val application =
        springApplication.apply {
          mainApplicationClass = config.testClass
          addPrimarySources(configClasses.toList())
          sources.addAll(configLocations.toList())
        }

    val initializers = getInitializers(config, application)
    if (config is WebMergedContextConfiguration) {
      application.webApplicationType = SERVLET
      if (!isEmbeddedWebEnvironment(config)) {
        WebConfigurer().configure(config, initializers)
      }
    } else if (config is ReactiveWebMergedContextConfiguration) {
      application.webApplicationType = REACTIVE
    } else {
      application.webApplicationType = NONE
    }
    application.setApplicationContextFactory { type: WebApplicationType ->
      if (type != NONE && !isEmbeddedWebEnvironment(config)) {
        if (type == REACTIVE) {
          return@setApplicationContextFactory GenericReactiveWebApplicationContext()
        }
        if (type == SERVLET) {
          return@setApplicationContextFactory GenericWebApplicationContext()
        }
      }
      ApplicationContextFactory.DEFAULT.create(type)
    }
    application.setInitializers(initializers)

    val environment = this.environment
    if (environment != null) {
      prepareEnvironment(config, application, environment, false)
      application.setEnvironment(environment)
    } else {
      application.addListeners(PrepareEnvironmentListener(config))
    }

    return application.run(*SmartSiteSpringBootTestArgs[config.contextCustomizers])
  }

  private inner class PrepareEnvironmentListener(private val config: MergedContextConfiguration) :
      ApplicationListener<ApplicationEnvironmentPreparedEvent?>, PriorityOrdered {
    override fun getOrder(): Int {
      return Ordered.HIGHEST_PRECEDENCE
    }

    override fun onApplicationEvent(event: ApplicationEnvironmentPreparedEvent?) {
      prepareEnvironment(config, event!!.springApplication, event.environment, true)
    }
  }

  private fun setActiveProfiles(
      environment: ConfigurableEnvironment,
      profiles: Array<String>,
      applicationEnvironment: Boolean
  ) {
    if (profiles.isEmpty()) {
      return
    }
    if (!applicationEnvironment) {
      environment.setActiveProfiles(*profiles)
    }
    val pairs = arrayOfNulls<String>(profiles.size)
    for (i in profiles.indices) {
      pairs[i] = "spring.profiles.active[" + i + "]=" + profiles[i]
    }
    TestPropertyValues.of(*pairs)
        .applyTo(environment, TestPropertyValues.Type.MAP, "active-test-profiles")
  }

  private fun prepareEnvironment(
      config: MergedContextConfiguration,
      application: SpringApplication,
      environment: ConfigurableEnvironment,
      applicationEnvironment: Boolean
  ) {
    setActiveProfiles(environment, config.activeProfiles, applicationEnvironment)
    val resourceLoader =
        if (application.resourceLoader != null) application.resourceLoader
        else DefaultResourceLoader(null)
    TestPropertySourceUtils.addPropertiesFilesToEnvironment(
        environment, resourceLoader, *config.propertySourceLocations)
    TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
        environment, *getInlinedProperties(config))
  }

  /** Inner class to configure [WebMergedContextConfiguration]. */
  private class WebConfigurer {

    fun configure(
        configuration: MergedContextConfiguration,
        initializers: MutableList<ApplicationContextInitializer<*>?>
    ) {
      val webConfiguration = configuration as WebMergedContextConfiguration
      addMockServletContext(initializers, webConfiguration)
    }

    private fun addMockServletContext(
        initializers: MutableList<ApplicationContextInitializer<*>?>,
        webConfiguration: WebMergedContextConfiguration
    ) {
      val servletContext = SpringBootMockServletContext(webConfiguration.resourceBasePath)
      initializers.add(
          0,
          DefensiveWebApplicationContextInitializer(
              ServletContextApplicationContextInitializer(servletContext, true)))
    }

    private class DefensiveWebApplicationContextInitializer(
        private val delegate: ServletContextApplicationContextInitializer
    ) : ApplicationContextInitializer<ConfigurableApplicationContext?> {

      override fun initialize(applicationContext: ConfigurableApplicationContext?) {
        if (applicationContext is ConfigurableWebApplicationContext) {
          delegate.initialize(applicationContext)
        }
      }
    }
  }

  private fun isEmbeddedWebEnvironment(config: MergedContextConfiguration) =
      MergedAnnotations.from(config.testClass, SearchStrategy.TYPE_HIERARCHY)[
              SmartSiteSpringBootTest::class.java]
          .getValue("webEnvironment", WebEnvironment::class.java)
          .orElse(WebEnvironment.NONE)
          .isEmbedded
}
