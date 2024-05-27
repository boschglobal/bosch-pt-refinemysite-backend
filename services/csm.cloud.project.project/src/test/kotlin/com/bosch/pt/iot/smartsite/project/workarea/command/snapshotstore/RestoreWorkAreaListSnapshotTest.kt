/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.workarea.command.snapshotstore

import com.bosch.pt.csm.cloud.common.streamable.restoredb.RestoreStrategyTest
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getByReference
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.ITEMADDED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaListEventEnumAvro.REORDERED
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaListId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkAreaList
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreWorkAreaListSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm1"))
    projectEventStoreUtils.reset()
  }

  @Test
  open fun `validate that work area list created event was processed successfully`() {
    val workAreaList =
        repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
    val aggregate = get<WorkAreaListAggregateAvro>("workAreaList")!!

    validateWorkAreaList(
        workAreaList,
        aggregate,
        projectIdentifier,
        listOf(getIdentifier("workArea").asWorkAreaId()))
  }

  @Test
  open fun `validate that work area list item added event was processed successfully`() {
    eventStreamGenerator
        .submitWorkArea(asReference = "workAreaTwo", eventType = CREATED) {
          it.name = "New work area"
        }
        .submitWorkAreaList(asReference = "workAreaList", eventType = ITEMADDED) {
          it.workAreas = listOf(getByReference("workArea"), getByReference("workAreaTwo"))
        }

    val workAreaList =
        repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
    val aggregate = get<WorkAreaListAggregateAvro>("workAreaList")!!

    validateWorkAreaList(
        workAreaList,
        aggregate,
        projectIdentifier,
        listOf(
            getIdentifier("workArea").asWorkAreaId(), getIdentifier("workAreaTwo").asWorkAreaId()))
  }

  @Test
  open fun `validate that work area list item removed event was processed successfully`() {
    eventStreamGenerator
        .submitWorkArea(asReference = "workAreaTwo", eventType = CREATED) {
          it.name = "New work area"
        }
        .submitWorkAreaList(asReference = "workAreaList", eventType = ITEMADDED) {
          it.workAreas = listOf(getByReference("workArea"), getByReference("workAreaTwo"))
        }
        .submitWorkAreaList(asReference = "workAreaList", eventType = ITEMADDED) {
          it.workAreas = listOf(getByReference("workAreaTwo"))
        }

    val workAreaList =
        repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
    val aggregate = get<WorkAreaListAggregateAvro>("workAreaList")!!

    validateWorkAreaList(
        workAreaList,
        aggregate,
        projectIdentifier,
        listOf(getIdentifier("workAreaTwo").asWorkAreaId()))
  }

  @Test
  open fun `validate that work area list item reordered event was processed successfully`() {
    eventStreamGenerator
        .submitWorkArea(asReference = "workAreaTwo", eventType = CREATED) {
          it.name = "New work area"
        }
        .submitWorkAreaList(asReference = "workAreaList", eventType = ITEMADDED) {
          it.workAreas = listOf(getByReference("workArea"), getByReference("workAreaTwo"))
        }
        .submitWorkAreaList(asReference = "workAreaList", eventType = REORDERED) {
          it.workAreas = listOf(getByReference("workAreaTwo"), getByReference("workArea"))
        }

    val workAreaList =
        repositories.findWorkAreaList(getIdentifier("workAreaList").asWorkAreaListId())!!
    val aggregate = get<WorkAreaListAggregateAvro>("workAreaList")!!

    validateWorkAreaList(
        workAreaList,
        aggregate,
        projectIdentifier,
        listOf(
            getIdentifier("workAreaTwo").asWorkAreaId(), getIdentifier("workArea").asWorkAreaId()))
  }

  private fun validateWorkAreaList(
      workAreaList: WorkAreaList,
      aggregate: WorkAreaListAggregateAvro,
      projectIdentifier: ProjectId,
      workAreaIdentifiers: List<WorkAreaId> = listOf()
  ) =
      with(workAreaList) {
        validateAuditingInformationAndIdentifierAndVersion(this, aggregate)
        assertThat(project.identifier).isEqualTo(projectIdentifier)
        assertThat(workAreaList.workAreas)
            .extracting<WorkAreaId>(
                com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea::identifier)
            .containsExactlyElementsOf(workAreaIdentifiers)
      }
}
