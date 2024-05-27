/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.application.config.properties

import org.springframework.boot.autoconfigure.mongo.MongoProperties
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "smartsite.datasource")
class MultipleMongoDbProperties(
    val activityService: MongoProperties = MongoProperties(),
    val jobService: MongoProperties = MongoProperties(),
    val notificationService: MongoProperties = MongoProperties(),
    val projectApiTimeseriesService: MongoProperties = MongoProperties(),
    val projectService: MongoProperties = MongoProperties(),
)
