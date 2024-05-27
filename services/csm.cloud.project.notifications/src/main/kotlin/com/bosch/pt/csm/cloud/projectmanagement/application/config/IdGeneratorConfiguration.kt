/*
 * *************************************************************************
 *
 * Copyright:       Robert Bosch Power Tools GmbH, 2019
 *
 * *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.application.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.util.AlternativeJdkIdGenerator
import org.springframework.util.IdGenerator

@Configuration
class IdGeneratorConfiguration {

    /**
     * Creates new [IdGenerator].
     *
     * @return the id generator
     */
    @Bean
    fun idGenerator(): IdGenerator {
        return AlternativeJdkIdGenerator()
    }
}
