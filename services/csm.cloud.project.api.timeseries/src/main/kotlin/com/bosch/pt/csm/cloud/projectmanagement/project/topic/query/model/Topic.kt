/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.projectmanagement.project.topic.query.model

import com.bosch.pt.csm.cloud.projectmanagement.project.project.domain.ProjectId
import com.bosch.pt.csm.cloud.projectmanagement.project.task.domain.TaskId
import com.bosch.pt.csm.cloud.projectmanagement.project.topic.domain.TopicId
import com.bosch.pt.csm.cloud.projectmanagement.translation.shared.model.TranslatableEnum
import com.bosch.pt.csm.cloud.projectmanagement.user.user.domain.UserId
import java.time.LocalDateTime
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.TypeAlias
import org.springframework.data.mongodb.core.mapping.Document

const val TOPIC_PROJECTION = "TopicProjection"

@Document(TOPIC_PROJECTION)
@TypeAlias(TOPIC_PROJECTION)
data class Topic(
    @Id val identifier: TopicId,
    val version: Long,
    val project: ProjectId,
    val task: TaskId,
    val criticality: TopicCriticalityEnum,
    val description: String? = null,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
    val history: List<TopicVersion>
)

data class TopicVersion(
    val version: Long,
    val criticality: TopicCriticalityEnum,
    val description: String? = null,
    val deleted: Boolean = false,
    val eventAuthor: UserId,
    val eventDate: LocalDateTime,
)

enum class TopicCriticalityEnum(private val criticality: String) : TranslatableEnum {
  CRITICAL("CRITICAL"),
  UNCRITICAL("UNCRITICAL");

  companion object {
    const val KEY_PREFIX: String = "TOPIC_CRITICALITY_"
  }

  val shortKey: String
    get() = this.criticality

  override val key: String
    get() = "${KEY_PREFIX}${this.criticality}"

  override val messageKey: String
    get() = "${TopicCriticalityEnum::class.simpleName}_$this"
}
