/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.craft.facade.listener.strategies

import com.bosch.pt.csm.cloud.common.model.AggregateIdentifier
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.projectmanagement.application.SmartSiteSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.common.facade.listener.strategies.notifications.BaseNotificationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(value = [SpringExtension::class])
@DisplayName("State must be cleaned up on project craft deleted event")
@SmartSiteSpringBootTest
class CleanupStateFromProjectCraftDeletedEventTest : BaseNotificationTest() {

  @Test
  fun `is cleaned up after projectCraft deleted event`() {
    eventStreamGenerator.submitProject().submitProjectCraftG2()
    val projectCraftIdentifier = getByReference(PROJECT_CRAFT).toAggregateIdentifier()
    assertThat(findProjectCraft(projectCraftIdentifier)).isNotNull

    eventStreamGenerator.submitProjectCraftG2(asReference = PROJECT_CRAFT, eventType = DELETED)
    assertThat(findProjectCraft(projectCraftIdentifier)).isNull()
  }
  private fun findProjectCraft(id: AggregateIdentifier) =
      repositories.projectCraftRepository.findById(id).orElse(null)
}
