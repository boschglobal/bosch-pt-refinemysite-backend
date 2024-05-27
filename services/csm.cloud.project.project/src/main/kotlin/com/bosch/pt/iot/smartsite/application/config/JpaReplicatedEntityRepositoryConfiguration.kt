/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.common.repository.impl.ReplicatedEntityRepositoryImpl
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    basePackages =
        [
            "com.bosch.pt.iot.smartsite.company.repository",
            "com.bosch.pt.iot.smartsite.craft.repository",
            "com.bosch.pt.iot.smartsite.user.repository"],
    repositoryBaseClass = ReplicatedEntityRepositoryImpl::class)
open class JpaReplicatedEntityRepositoryConfiguration
