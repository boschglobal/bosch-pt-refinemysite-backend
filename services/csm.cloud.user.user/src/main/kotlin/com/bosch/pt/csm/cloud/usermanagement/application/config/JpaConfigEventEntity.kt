/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.usermanagement.application.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    "com.bosch.pt.csm.cloud.usermanagement.user.eventstore",
    "com.bosch.pt.csm.cloud.usermanagement.consents.eventstore",
    "com.bosch.pt.csm.cloud.usermanagement.pat.eventstore"
)
class JpaConfigEventEntity
