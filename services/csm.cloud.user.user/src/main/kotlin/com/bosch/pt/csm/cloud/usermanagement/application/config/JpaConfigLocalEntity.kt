/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2020
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.application.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    basePackages =
        [
            "com.bosch.pt.csm.cloud.usermanagement.announcement.repository",
            "com.bosch.pt.csm.cloud.usermanagement.user.authorization.repository"])
class JpaConfigLocalEntity
