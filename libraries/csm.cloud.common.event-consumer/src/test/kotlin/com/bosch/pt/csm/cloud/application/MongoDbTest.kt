/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.application

import com.bosch.pt.csm.cloud.common.mongodb.extensions.MongoDbTestExtension
import kotlin.annotation.AnnotationRetention.RUNTIME
import kotlin.annotation.AnnotationTarget.CLASS
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.extension.ExtendWith

@Target(CLASS)
@Retention(RUNTIME)
@ExtendWith(MongoDbTestExtension::class)
@Tag("event-consumer")
annotation class MongoDbTest
