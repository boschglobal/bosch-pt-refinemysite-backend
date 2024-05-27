/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.eventstore.repository

import com.bosch.pt.csm.cloud.common.eventstore.KafkaEventTestRepository
import com.bosch.pt.iot.smartsite.project.eventstore.ProjectContextKafkaEvent
import org.springframework.context.annotation.Profile

@Profile("test")
interface ProjectKafkaEventTestRepository : KafkaEventTestRepository<ProjectContextKafkaEvent>
