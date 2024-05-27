/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.event.facade.listener.impl

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.common.CSM_USER
import com.bosch.pt.csm.cloud.projectmanagement.common.FM_PARTICIPANT
import com.bosch.pt.csm.cloud.projectmanagement.common.FM_USER
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractEventStreamIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.news.boundary.NewsService
import com.bosch.pt.csm.cloud.projectmanagement.news.model.ObjectIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitTask
import com.bosch.pt.csm.cloud.projectmanagement.common.submitInitProjectData
import com.bosch.pt.csm.cloud.projectmanagement.task.messages.TaskStatusEnumAvro
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource

@DisplayName("When 'process state only' is enabled")
@SmartSiteSpringBootTest
@TestPropertySource(
    properties =
        [
            "custom.process-state-only.enabled=true",
            "custom.process-state-only.until-date=2021-01-15"])
class EventListenerProcessStateOnlyTest(@Autowired private val newsService: NewsService) :
    AbstractEventStreamIntegrationTest() {

  @Test
  fun `no news are created for message date before 'until date'`() {
    eventStreamGenerator.submitInitProjectData().submitTask(
            auditUserReference = CSM_USER,
            time = LocalDateTime.of(2021, 1, 14, 23, 59, 59, 0).toInstant(UTC)) {
      it.assignee = getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.OPEN
    }

    val taskAggregateIdentifier = ObjectIdentifier(getByReference(TASK))
    val fmUserIdentifier = getIdentifier(FM_USER)

    assertThat(
            newsService.findAllByUserIdentifierAndRootObject(
                fmUserIdentifier, taskAggregateIdentifier))
        .isEmpty()
  }

  @Test
  fun `news are created for message date after 'until date'`() {
    eventStreamGenerator.submitInitProjectData().submitTask(
            auditUserReference = CSM_USER,
            time = LocalDateTime.of(2021, 1, 15, 0, 0, 0, 0).toInstant(UTC)) {
      it.assignee = getByReference(FM_PARTICIPANT)
      it.status = TaskStatusEnumAvro.OPEN
    }

    val taskAggregateIdentifier = ObjectIdentifier(getByReference(TASK))
    val fmUserIdentifier = getIdentifier(FM_USER)

    assertThat(
            newsService.findAllByUserIdentifierAndRootObject(
                fmUserIdentifier, taskAggregateIdentifier))
        .hasSize(1)
  }

  companion object {
    const val TASK = "task"
  }
}
