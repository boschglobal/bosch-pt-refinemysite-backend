/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig

@Target(CLASS)
@Retention(RUNTIME)
@SpringJUnitConfig(classes = [KafkaListenerConfiguration::class])
annotation class EnableKafkaListeners
