/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.application.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    "com.bosch.pt.csm.cloud.featuretoggle.eventstore",
    "com.bosch.pt.csm.cloud.featuretoggle.feature.shared.repository",
    "com.bosch.pt.csm.cloud.user.query")
class JpaRepositoryConfiguration
