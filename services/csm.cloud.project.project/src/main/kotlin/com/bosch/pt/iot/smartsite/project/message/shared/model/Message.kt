/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.project.message.shared.model

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntity
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.ForeignKey
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.validation.constraints.Size

@Entity
@Table(indexes = [Index(name = "UK_Message_Identifier", columnList = "identifier", unique = true)])
class Message : AbstractSnapshotEntity<Long, MessageId> {

  /** The content field of the message. */
  @field:Size(max = MAX_CONTENT_LENGTH)
  @Column(length = MAX_CONTENT_LENGTH)
  var content: String? = null

  /** Associated topic. */
  @JoinColumn(foreignKey = ForeignKey(name = "FK_Message_Topic"))
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  lateinit var topic: Topic

  /** Constructor for JPA. */
  constructor()

  /**
   * Creates new instance of [Message].
   *
   * @param identifier identifier of the message
   * @param content content field of the message
   * @param topic the corresponding topic of the message
   */
  constructor(identifier: MessageId, content: String?, topic: Topic) {
    this.identifier = identifier
    this.content = content
    this.topic = topic
  }

  override fun getDisplayName(): String? = content

  companion object {
    const val MAX_CONTENT_LENGTH = 320
  }
}
