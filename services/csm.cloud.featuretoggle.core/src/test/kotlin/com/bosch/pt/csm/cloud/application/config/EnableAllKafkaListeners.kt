/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.application.config

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@Target(CLASS)
@Retention(RUNTIME)
@SpringJUnitConfig(classes = [AllListenerConfiguration::class])
annotation class EnableAllKafkaListeners
