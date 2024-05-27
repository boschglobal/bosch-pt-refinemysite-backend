/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.application.config

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@Target(CLASS)
@Retention(RUNTIME)
@SpringJUnitConfig(classes = [AllListenerConfiguration::class])
annotation class EnableAllKafkaListeners
