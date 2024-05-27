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
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.craft.messages.ProjectCraftListEventEnumAvro.ITEMREMOVED
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftG2
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectCraftList
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.asProjectCraftListId
import com.bosch.pt.iot.smartsite.project.projectcraft.util.ProjectCraftTestUtil.validateProjectCraftList
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreProjectCraftListSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()
  }

  @Test
  open fun `validate that project craft list created event was processed successfully`() {
    val projectCraftList =
        repositories.findProjectCraftList(
            getIdentifier("projectCraftList").asProjectCraftListId())!!
    val aggregate = get<ProjectCraftListAggregateAvro>("projectCraftList")!!

    validateProjectCraftList(
        projectCraftList,
        aggregate,
        projectIdentifier,
        listOf(getIdentifier("projectCraft").asProjectCraftId()))
  }

  @Test
  open fun `validate that project craft list item added event was processed successfully`() {
    eventStreamGenerator
        .submitProjectCraftG2(asReference = "anotherProjectCraft")
        .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMADDED) {
          it.projectCrafts =
              listOf(getByReference("projectCraft"), getByReference("anotherProjectCraft"))
        }

    val projectCraftList =
        repositories.findProjectCraftList(
            getIdentifier("projectCraftList").asProjectCraftListId())!!
    val aggregate = get<ProjectCraftListAggregateAvro>("projectCraftList")!!

    validateProjectCraftList(
        projectCraftList,
        aggregate,
        projectIdentifier,
        listOf(
            getIdentifier("projectCraft").asProjectCraftId(),
            getIdentifier("anotherProjectCraft").asProjectCraftId()))
  }

  @Test
  open fun `validate that project craft list item removed event was processed successfully`() {
    eventStreamGenerator
        .submitProjectCraftG2(asReference = "anotherProjectCraft1")
        .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMADDED) {
          it.projectCrafts =
              listOf(getByReference("projectCraft"), getByReference("anotherProjectCraft1"))
        }
        .submitProjectCraftList(asReference = "projectCraftList", eventType = ITEMREMOVED) {
          it.projectCrafts = listOf(getByReference("projectCraft"))
        }
        .submitProjectCraftG2(asReference = "anotherProjectCraft1", eventType = DELETED)

    val projectCraftList =
        repositories.findProjectCraftList(
            getIdentifier("projectCraftList").asProjectCraftListId())!!
    val aggregate = get<ProjectCraftListAggregateAvro>("projectCraftList")!!

    validateProjectCraftList(
        projectCraftList,
        aggregate,
        projectIdentifier,
        listOf(getIdentifier("projectCraft").asProjectCraftId()))
  }
}
