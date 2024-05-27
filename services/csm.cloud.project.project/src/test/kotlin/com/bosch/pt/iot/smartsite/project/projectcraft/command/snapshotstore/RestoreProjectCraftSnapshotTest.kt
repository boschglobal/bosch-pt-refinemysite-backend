/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectcraft.command.snapshotstore

import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftAggregateG2Avro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.UPDATED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.util.ProjectCraftTestUtil.validateProjectCraft
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreProjectCraftSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
  }

  @Test
  open fun `validate that project craft created event was processed successfully`() {
    val projectCraft =
        repositories.findProjectCraft(getIdentifier("projectCraft").asProjectCraftId())!!
    val aggregate = get<ProjectCraftAggregateG2Avro>("projectCraft")!!

    validateProjectCraft(projectCraft, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that project craft updated event was processed successfully`() {
    eventStreamGenerator.submitProjectCraftG2(asReference = "projectCraft", eventType = UPDATED) {
      it.name = "Updated name"
    }

    val projectCraft =
        repositories.findProjectCraft(getIdentifier("projectCraft").asProjectCraftId())!!
    val projectCraftAggregate = get<ProjectCraftAggregateG2Avro>("projectCraft")!!

    validateProjectCraft(projectCraft, projectCraftAggregate, projectIdentifier)
  }

  @Test
  open fun `validate that project craft deleted event deletes a project craft`() {
    // Create a new project craft not used by any task and delete it testing idempotency
    eventStreamGenerator
        .submitProjectCraftG2(asReference = "anotherProjectCraft")
        .submitProjectCraftG2(asReference = "anotherProjectCraft", eventType = DELETED)
        .repeat(1)

    assertThat(
            repositories.findProjectCraft(getIdentifier("anotherProjectCraft").asProjectCraftId()))
        .isNull()
  }
}
