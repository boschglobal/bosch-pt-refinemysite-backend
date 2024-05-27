/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */
package com.bosch.pt.iot.smartsite.application

import com.bosch.pt.iot.smartsite.common.extensions.DefaultLocaleExtension
import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.ANNOTATION_CLASS
import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.core.annotation.AliasFor
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.BootstrapWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@Target(ANNOTATION_CLASS, CLASS)
@Retention(RUNTIME)
@Inherited
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = [KafkaAutoConfiguration::class])
@BootstrapWith(SmartSiteSpringBootTestContextBootstrapper::class)
@ExtendWith(SpringExtension::class)
@ExtendWith(DefaultLocaleExtension::class)
annotation class SmartSiteSpringBootTest(
    @get:AliasFor("properties") val value: Array<String> = [],
    @get:AliasFor("value") val properties: Array<String> = [],
    val classes: Array<KClass<*>> = [],
    val webEnvironment: WebEnvironment = WebEnvironment.MOCK
) {

  enum class WebEnvironment(val isEmbedded: Boolean) {
    MOCK(false),
    RANDOM_PORT(true),
    DEFINED_PORT(true),
    NONE(false)
  }
}
