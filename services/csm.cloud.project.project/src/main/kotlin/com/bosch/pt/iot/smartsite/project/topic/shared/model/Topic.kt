/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.topic.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.task.shared.model.Task
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import jakarta.validation.constraints.Size

@Entity
@Table(indexes = [Index(name = "UK_Topic_Identifier", columnList = "identifier", unique = true)])
class Topic : AbstractSnapshotEntity<Long, TopicId> {

  @Column(nullable = false, length = MAX_CRITICALITY_LENGTH, columnDefinition = "varchar(30)")
  @Enumerated(EnumType.STRING)
  lateinit var criticality: TopicCriticalityEnum

  @field:Size(max = MAX_DESCRIPTION_LENGTH)
  @Column(length = MAX_DESCRIPTION_LENGTH)
  var description: String? = null

  @JoinColumn(foreignKey = ForeignKey(name = "FK_Topic_Task"))
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var task: Task

  @OneToMany(fetch = FetchType.LAZY, mappedBy = "topic")
  private var messages: MutableSet<Message> = mutableSetOf()

  @Column(columnDefinition = "bit not null default 0", insertable = false) val deleted = false

  constructor()

  constructor(
      identifier: TopicId,
      criticality: TopicCriticalityEnum,
      description: String?,
      task: Task,
      messages: MutableSet<Message>,
  ) {
    this.identifier = identifier
    this.criticality = criticality
    this.description = description
    this.task = task
    this.messages = messages
  }

  fun getMessages(): MutableSet<Message> = messages

  fun isDeleted(): Boolean = deleted

  fun setMessages(messages: MutableSet<Message>) {
    this.messages = messages
  }

  override fun getDisplayName(): String? = description

  companion object {
    const val MAX_DESCRIPTION_LENGTH = 320
    const val MAX_CRITICALITY_LENGTH = 30
  }
}
