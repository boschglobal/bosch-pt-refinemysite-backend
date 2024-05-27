/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.application.config

import com.bosch.pt.iot.smartsite.common.kafka.streamable.KafkaStreamableRepositoryImpl
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@Configuration
@EnableJpaRepositories(
    basePackages =
        [
            "com.bosch.pt.iot.smartsite.project.attachment.repository",
            "com.bosch.pt.iot.smartsite.project.external.repository",
            "com.bosch.pt.iot.smartsite.project.messageattachment.repository",
            "com.bosch.pt.iot.smartsite.project.projectpicture.repository",
            "com.bosch.pt.iot.smartsite.project.projectstatistics.repository",
            "com.bosch.pt.iot.smartsite.project.relation.repository",
            "com.bosch.pt.iot.smartsite.project.rfv.repository",
            "com.bosch.pt.iot.smartsite.project.taskstatistics.repository",
            "com.bosch.pt.iot.smartsite.project.taskconstraint.repository",
            "com.bosch.pt.iot.smartsite.project.taskattachment.repository",
            "com.bosch.pt.iot.smartsite.project.topicattachment.repository"],
    repositoryBaseClass = KafkaStreamableRepositoryImpl::class)
class JpaKafkaStreamableRepositoryConfiguration
