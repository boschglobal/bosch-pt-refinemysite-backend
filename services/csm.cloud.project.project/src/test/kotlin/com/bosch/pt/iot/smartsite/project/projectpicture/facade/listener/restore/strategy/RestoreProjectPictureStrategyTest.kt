/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.projectpicture.facade.listener.restore.strategy

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGeneratorStaticExtensions.Companion.get
import com.bosch.pt.csm.cloud.projectmanagement.project.event.submitProjectPicture
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureAggregateAvro
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.project.messages.ProjectPictureEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.common.event.setupDatasetTestData
import com.bosch.pt.iot.smartsite.common.facade.listener.AbstractRestoreIntegrationTestV2
import com.bosch.pt.iot.smartsite.project.project.asProjectId
import com.bosch.pt.iot.smartsite.project.projectpicture.model.ProjectPicture
import com.bosch.pt.iot.smartsite.util.getIdentifier
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

open class RestoreProjectPictureStrategyTest : AbstractRestoreIntegrationTestV2() {

  @BeforeEach
  fun init() {
    eventStreamGenerator.setupDatasetTestData().submitProjectPicture()
  }

  @Test
  open fun `validate that picture created event was processed successfully`() {
    val pictureAggregate = get<ProjectPictureAggregateAvro>("projectPicture")!!
    val picture =
        repositories.projectPictureRepository.findOneByIdentifier(
            pictureAggregate.getIdentifier())!!

    validateBasicAttributes(picture, pictureAggregate)
    validateAuditableAndVersionedEntityAttributes(picture, pictureAggregate)
  }

  @Test
  open fun `validate that picture updated event was processed successfully`() {
    eventStreamGenerator.submitProjectPicture(eventType = UPDATED) { it.fullAvailable = true }

    val pictureAggregate = get<ProjectPictureAggregateAvro>("projectPicture")!!

    val picture =
        repositories.projectPictureRepository.findOneByIdentifier(
            pictureAggregate.getIdentifier())!!

    validateBasicAttributes(picture, pictureAggregate)
    validateAuditableAndVersionedEntityAttributes(picture, pictureAggregate)
  }

  @Test
  open fun `validate picture deleted event deletes a project picture`() {
    assertThat(repositories.projectPictureRepository.findAll()).hasSize(1)

    eventStreamGenerator.submitProjectPicture(eventType = DELETED)

    assertThat(repositories.projectPictureRepository.findAll()).isEmpty()

    // Send event again to test idempotency
    eventStreamGenerator.repeat(1)
  }

  private fun validateBasicAttributes(
      picture: ProjectPicture,
      pictureAggregate: ProjectPictureAggregateAvro
  ) {
    assertThat(picture.fileSize).isEqualTo(pictureAggregate.fileSize)
    assertThat(picture.height).isEqualTo(pictureAggregate.height)
    assertThat(picture.project!!.identifier)
        .isEqualTo(pictureAggregate.project.identifier.asProjectId())
    assertThat(picture.width).isEqualTo(pictureAggregate.width)
    assertThat(picture.isFullAvailable()).isEqualTo(pictureAggregate.fullAvailable)
    assertThat(picture.isSmallAvailable()).isEqualTo(pictureAggregate.smallAvailable)
  }
}
