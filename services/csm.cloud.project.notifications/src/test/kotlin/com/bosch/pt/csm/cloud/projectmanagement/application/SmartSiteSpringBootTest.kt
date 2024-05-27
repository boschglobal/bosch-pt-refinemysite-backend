/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application

import com.bosch.pt.csm.cloud.projectmanagement.event.boundary.EventServiceTestConfiguration
import java.lang.annotation.Inherited
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

@Target(AnnotationTarget.ANNOTATION_CLASS, AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Inherited
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = [KafkaAutoConfiguration::class])
@SpringBootTest
@MongoDbTest
@Import(EventServiceTestConfiguration::class)
annotation class SmartSiteSpringBootTest
