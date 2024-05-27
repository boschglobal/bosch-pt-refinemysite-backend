/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.application.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    "com.bosch.pt.csm.company.company.shared.repository",
    "com.bosch.pt.csm.company.employee.query.employableuser",
    "com.bosch.pt.csm.company.employee.shared.repository",
    "com.bosch.pt.csm.company.eventstore",
    "com.bosch.pt.csm.user.authorization.repository",
    "com.bosch.pt.csm.user.user.query")
class JpaRepositoryConfiguration
