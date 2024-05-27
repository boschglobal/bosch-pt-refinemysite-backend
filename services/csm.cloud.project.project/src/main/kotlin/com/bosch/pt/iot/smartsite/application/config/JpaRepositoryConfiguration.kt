/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

/** Configuration of JPA Repositories that have no specific base class */
@Configuration
@EnableJpaRepositories(
    "com.bosch.pt.csm.cloud.common.businesstransaction.jpa",
    "com.bosch.pt.iot.smartsite.project.daycard.shared.repository",
    "com.bosch.pt.iot.smartsite.project.eventstore",
    "com.bosch.pt.iot.smartsite.project.importer.repository",
    "com.bosch.pt.iot.smartsite.project.message.shared.repository",
    "com.bosch.pt.iot.smartsite.project.milestone.shared.repository",
    "com.bosch.pt.iot.smartsite.project.participant.shared.repository",
    "com.bosch.pt.iot.smartsite.project.project.shared.repository",
    "com.bosch.pt.iot.smartsite.project.projectcraft.shared.repository",
    "com.bosch.pt.iot.smartsite.project.workarea.shared.repository",
    "com.bosch.pt.iot.smartsite.project.workday.shared.repository",
    "com.bosch.pt.iot.smartsite.project.taskschedule.shared.repository",
    "com.bosch.pt.iot.smartsite.project.task.shared.repository",
    "com.bosch.pt.iot.smartsite.project.topic.shared.repository",
    "com.bosch.pt.iot.smartsite.user.authorization.repository")
class JpaRepositoryConfiguration
