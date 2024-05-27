/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.convert.MongoCustomConversions

@Configuration
class MongoCustomConversionConfiguration {

    @Bean
    fun mongoCustomConversions(): MongoCustomConversions {
        val converters = listOf(
            BsonToUuidConverter()
        )
        return MongoCustomConversions(converters)
    }
}
