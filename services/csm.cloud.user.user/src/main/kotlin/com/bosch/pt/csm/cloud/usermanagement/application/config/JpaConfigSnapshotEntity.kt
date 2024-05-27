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
            "com.bosch.pt.csm.cloud.usermanagement.craft.craft.shared.repository",
            "com.bosch.pt.csm.cloud.usermanagement.consents.consents.shared.repository",
            "com.bosch.pt.csm.cloud.usermanagement.consents.document.shared.repository",
            "com.bosch.pt.csm.cloud.usermanagement.pat.pat.shared.repository",
            "com.bosch.pt.csm.cloud.usermanagement.user.user.shared.repository",
            "com.bosch.pt.csm.cloud.usermanagement.user.picture.shared.repository"])
class JpaConfigSnapshotEntity
