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
import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.workarea.domain.asWorkAreaId
import com.bosch.pt.iot.smartsite.project.workarea.shared.model.WorkArea
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RestoreStrategyTest
open class RestoreWorkAreaSnapshotTest : AbstractRestoreIntegrationTestV2() {

  private val projectIdentifier by lazy { getIdentifier("project").asProjectId() }

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData()

    setAuthentication(getIdentifier("userCsm1"))
    projectEventStoreUtils.reset()
  }

  @Test
  open fun `validate that work area created event was processed successfully`() {
    val workArea = repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
    val aggregate = get<WorkAreaAggregateAvro>("workArea")!!

    validateWorkArea(workArea, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate that work area updated event was processed successfully`() {
    eventStreamGenerator.submitWorkArea(asReference = "workArea", eventType = UPDATED) {
      it.name = "Updated name"
    }

    val workArea = repositories.findWorkArea(getIdentifier("workArea").asWorkAreaId())!!
    val aggregate = get<WorkAreaAggregateAvro>("workArea")!!

    validateWorkArea(workArea, aggregate, projectIdentifier)
  }

  @Test
  open fun `validate working area deleted event deletes a work area`() {
    // Create a new WorkArea not used by any WorkAreaList and delete it testing idempotency
    eventStreamGenerator
        .submitWorkArea(asReference = "anotherWorkArea")
        .submitWorkArea(asReference = "anotherWorkArea", eventType = DELETED)
        .repeat(1)

    assertThat(repositories.findWorkArea(getIdentifier("anotherWorkArea").asWorkAreaId())).isNull()
  }

  private fun validateWorkArea(
      workArea: WorkArea,
      aggregate: WorkAreaAggregateAvro,
      projectIdentifier: ProjectId
  ) =
      with(workArea) {
        validateAuditingInformationAndIdentifierAndVersion(this, aggregate)
        assertThat(project.identifier).isEqualTo(projectIdentifier)
        assertThat(workArea.name).isEqualTo(aggregate.name)
      }
}
