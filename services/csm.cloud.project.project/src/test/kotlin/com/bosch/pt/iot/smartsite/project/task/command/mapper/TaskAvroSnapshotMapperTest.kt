/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.task.command.mapper

import com.bosch.pt.csm.cloud.common.api.UserId
import com.bosch.pt.iot.smartsite.project.participant.ParticipantId
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.projectcraft.domain.ProjectCraftId
import com.bosch.pt.iot.smartsite.project.task.command.snapshotstore.TaskSnapshot
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskStatusEnum
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.workarea.domain.WorkAreaId
import java.time.LocalDateTime.now
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TaskAvroSnapshotMapperTest {

  @Test
  fun `rootContextIdentifier must be the project identifier`() {
    val projectIdentifier = ProjectId()
    val snapshot = createTaskSnapshot(projectIdentifier = projectIdentifier)

    val messageKey = TaskAvroSnapshotMapper.toMessageKeyWithCurrentVersion(snapshot)

    assertThat(messageKey.rootContextIdentifier).isEqualTo(projectIdentifier.toUuid())
  }

  private fun createTaskSnapshot(projectIdentifier: ProjectId = ProjectId()) =
      TaskSnapshot(
          identifier = TaskId(),
          version = 0,
          createdBy = UserId(),
          createdDate = now(),
          lastModifiedBy = UserId(),
          lastModifiedDate = now(),
          projectIdentifier = projectIdentifier,
          name = "name",
          description = "description",
          location = "location",
          projectCraftIdentifier = ProjectCraftId(),
          assigneeIdentifier = ParticipantId(),
          workAreaIdentifier = WorkAreaId(),
          status = TaskStatusEnum.DRAFT,
          topics = listOf(TopicId()),
          editDate = now(),
          deleted = false)
}
