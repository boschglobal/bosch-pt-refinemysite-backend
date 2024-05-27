/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.common.streamable.restoredb

import java.lang.annotation.Inherited
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@Target(CLASS)
@Retention(RUNTIME)
@MustBeDocumented
@Inherited
@SpringBootTest
@ActiveProfiles("test", "restore-db-test", "idp-bosch-dev")
@EnableAutoConfiguration(exclude = [KafkaAutoConfiguration::class])
@EnableConfigurationProperties(KafkaProperties::class)
annotation class RestoreStrategyTest
