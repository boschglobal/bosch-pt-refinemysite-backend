/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitDayCardG2
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.AggregateType.TASK
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.statistics.repository.DayCardRepository
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.DayCardEventEnumAvro
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskAggregateAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

@SmartSiteSpringBootTest
class StatisticsTests : AbstractStatisticsIntegrationTest() {

  @Autowired private lateinit var dayCardRepository: DayCardRepository

  @BeforeEach
  fun init() {
    initSecurityContext()
  }

  @Test
  fun `dayCard without a reason`() {
    eventStreamGenerator
        .submitDayCardG2(
            asReference = "daycard-with-null-reason-0", eventType = DayCardEventEnumAvro.CREATED) {
          it.reason = null
          it.task = eventStreamGenerator.get<TaskAggregateAvro>("task-1")!!.aggregateIdentifier
        }
        .submitDayCardG2(
            asReference = "daycard-with-null-reason-1", eventType = DayCardEventEnumAvro.CREATED) {
          it.reason = null
          it.task = eventStreamGenerator.get<TaskAggregateAvro>("task-1")!!.aggregateIdentifier
        }

    val taskObjectIdentifier =
        ObjectIdentifier(
            TASK,
            eventStreamGenerator
                .get<TaskAggregateAvro>("task-1")!!
                .aggregateIdentifier
                .identifier
                .toUUID())

    assertThat(dayCardRepository.findAllByTaskIdentifier(taskObjectIdentifier.identifier)[0].reason)
        .isNull()
    assertThat(dayCardRepository.findAllByTaskIdentifier(taskObjectIdentifier.identifier)[1].reason)
        .isNull()
  }
}
