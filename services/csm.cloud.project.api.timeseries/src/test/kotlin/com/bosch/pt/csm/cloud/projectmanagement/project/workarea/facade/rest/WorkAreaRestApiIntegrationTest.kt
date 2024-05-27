/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest

import com.bosch.pt.csm.cloud.projectmanagement.application.RmsSpringBootTest
import com.bosch.pt.csm.cloud.projectmanagement.application.common.AbstractRestApiIntegrationTest
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProject
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkArea
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitWorkAreaList
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.domain.asWorkAreaId
import com.bosch.pt.csm.cloud.projectmanagement.project.workarea.facade.rest.resource.response.WorkAreaListResource
import com.bosch.pt.csm.cloud.projectmanagement.test.eventTimestamp
import com.bosch.pt.csm.cloud.projectmanagement.workarea.message.getIdentifier
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.workarea.messages.WorkAreaEventEnumAvro
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@RmsSpringBootTest
class WorkAreaRestApiIntegrationTest : AbstractRestApiIntegrationTest() {

  lateinit var aggregateV0: WorkAreaAggregateAvro

  lateinit var aggregateV1: WorkAreaAggregateAvro

  @BeforeEach
  fun init() {
    setAuthentication("csm-user")
    submitBaseEvents()
  }

  @Test
  fun `query work area`() {
    submitEvents()

    // Execute query
    val workAreaList = query(false)

    // Validate payload
    assertThat(workAreaList.workAreas).hasSize(2)

    val workAreaV0 = workAreaList.workAreas[0]
    assertThat(workAreaV0.id).isEqualTo(aggregateV0.getIdentifier().asWorkAreaId())
    assertThat(workAreaV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(workAreaV0.name).isEqualTo(aggregateV0.name)
    assertThat(workAreaV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(workAreaV0.deleted).isFalse()

    val workAreaV1 = workAreaList.workAreas[1]
    assertThat(workAreaV1.id).isEqualTo(aggregateV1.getIdentifier().asWorkAreaId())
    assertThat(workAreaV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(workAreaV1.name).isEqualTo(aggregateV1.name)
    assertThat(workAreaV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(workAreaV1.deleted).isFalse()
  }

  @Test
  fun `query work area latest only`() {
    submitEvents()

    // Execute query
    val workAreaList = query(true)

    // Validate payload
    assertThat(workAreaList.workAreas).hasSize(1)
    val workArea = workAreaList.workAreas.first()

    assertThat(workArea.id).isEqualTo(aggregateV1.getIdentifier().asWorkAreaId())
    assertThat(workArea.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(workArea.name).isEqualTo(aggregateV1.name)
    assertThat(workArea.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(workArea.deleted).isFalse()
  }

  @Test
  fun `query work area without work area list`() {
    eventStreamGenerator.submitWorkArea()

    // Execute query
    val workAreaList = query(false)

    // Validate that the working area is returned even if there's no
    // working area list. This is different to the graphql api.
    assertThat(workAreaList.workAreas).hasSize(1)
  }

  @Test
  fun `query deleted work area`() {
    submitAsDeletedEvents()

    // Execute query
    val workAreaList = query(false)

    // Validate payload
    assertThat(workAreaList.workAreas).hasSize(2)

    val workAreaV0 = workAreaList.workAreas[0]
    assertThat(workAreaV0.id).isEqualTo(aggregateV0.getIdentifier().asWorkAreaId())
    assertThat(workAreaV0.version).isEqualTo(aggregateV0.aggregateIdentifier.version)
    assertThat(workAreaV0.name).isEqualTo(aggregateV0.name)
    assertThat(workAreaV0.eventTimestamp).isEqualTo(aggregateV0.eventTimestamp())
    assertThat(workAreaV0.deleted).isFalse()

    val workAreaV1 = workAreaList.workAreas[1]
    assertThat(workAreaV1.id).isEqualTo(aggregateV1.getIdentifier().asWorkAreaId())
    assertThat(workAreaV1.version).isEqualTo(aggregateV1.aggregateIdentifier.version)
    assertThat(workAreaV1.name).isEqualTo(aggregateV1.name)
    assertThat(workAreaV1.eventTimestamp).isEqualTo(aggregateV1.eventTimestamp())
    assertThat(workAreaV1.deleted).isTrue()
  }

  @Test
  fun `query deleted work area latest only`() {
    submitAsDeletedEvents()

    // Execute query
    val workAreaList = query(true)

    // Validate payload
    assertThat(workAreaList.workAreas).isEmpty()
  }

  fun submitEvents() {
    aggregateV0 = eventStreamGenerator.submitWorkArea().submitWorkAreaList().get("workArea")!!
    aggregateV1 =
        eventStreamGenerator
            .submitWorkArea(eventType = WorkAreaEventEnumAvro.UPDATED) {
              it.name = "Updated work area"
            }
            .get("workArea")!!
  }

  fun submitAsDeletedEvents() {
    aggregateV0 = eventStreamGenerator.submitWorkArea().submitWorkAreaList().get("workArea")!!
    aggregateV1 =
        eventStreamGenerator
            .submitWorkArea(eventType = WorkAreaEventEnumAvro.DELETED)
            .get("workArea")!!
  }

  fun submitBaseEvents() {
    eventStreamGenerator.submitProject().submitCsmParticipant()
  }

  private fun query(latestOnly: Boolean) =
      super.query(
          latestProjectApi("/projects/workareas"),
          latestOnly,
          WorkAreaListResource::class.java)
}
