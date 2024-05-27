/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.shared.model

import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicBuilder
import com.bosch.pt.iot.smartsite.user.model.User
import java.time.LocalDateTime

/** Builder for [Message]. */
@Deprecated("To be removed with the cleanup of the old test data creation")
class MessageBuilder

/** Private constructor. */
private constructor() {
  private var topic: Topic? = null
  private var content: String? = null
  private var identifier: MessageId = MessageId()
  private var createdBy: User? = null
  private var lastModifiedBy: User? = null
  private var createdDate = LocalDateTime.now()
  private var lastModifiedDate = LocalDateTime.now()

  /**
   * Sets the topic the message belongs to.
   *
   * @param topic the topic
   * @return the builder
   */
  fun withTopic(topic: Topic): MessageBuilder {
    this.topic = topic
    return this
  }

  /**
   * Sets the content of the message.
   *
   * @param content the content of the message
   * @return the builder
   */
  fun withContent(content: String?): MessageBuilder {
    this.content = content
    return this
  }

  /**
   * Sets the creator of the message.
   *
   * @param user the user
   * @return the builder
   */
  fun withCreatedBy(user: User): MessageBuilder {
    createdBy = user
    return this
  }

  /**
   * Sets the identifier of the message.
   *
   * @param identifier the identifier
   * @return the builder
   */
  fun withIdentifier(identifier: MessageId): MessageBuilder {
    this.identifier = identifier
    return this
  }

  /**
   * Sets the user who modified the message last.
   *
   * @param lastModifiedBy the user
   * @return the builder
   */
  fun withLastModifiedBy(lastModifiedBy: User): MessageBuilder {
    this.lastModifiedBy = lastModifiedBy
    return this
  }

  /**
   * Sets creation date.
   *
   * @param createdDate the date of creation
   * @return the builder
   */
  fun withCreatedDate(createdDate: LocalDateTime): MessageBuilder {
    this.createdDate = createdDate
    return this
  }

  /**
   * Sets date of last modification.
   *
   * @param lastModifiedDate the date of last modification
   * @return the builder
   */
  fun withLastModifiedDate(lastModifiedDate: LocalDateTime): MessageBuilder {
    this.lastModifiedDate = lastModifiedDate
    return this
  }

  /**
   * Creates the target [Message].
   *
   * @return target message
   */
  fun build(): Message {
    val message = Message(identifier, content, topic!!)
    message.setCreatedDate(createdDate)
    message.setLastModifiedDate(lastModifiedDate)
    message.topic = topic!!
    message.identifier = identifier
    createdBy?.getAuditUserId()?.let { message.setCreatedBy(it) }
    createdBy?.getAuditUserId()?.let { message.setLastModifiedBy(it) }
    return message
  }

  companion object {

    /**
     * Creates new builder.
     *
     * @return new builder
     */
    fun message(): MessageBuilder =
        MessageBuilder()
            .withIdentifier(MessageId())
            .withContent("content")
            .withTopic(TopicBuilder.topic().build())
  }
}
