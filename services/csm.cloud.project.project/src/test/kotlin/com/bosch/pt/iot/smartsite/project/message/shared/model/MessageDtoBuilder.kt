/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.shared.model

import com.bosch.pt.csm.cloud.common.api.asUserId
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.dto.MessageDto
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import java.time.Instant.now
import java.util.Date
import java.util.UUID.randomUUID

/** Builder for [MessageDto]. */
@Deprecated("To be removed with the cleanup of the old test data creation")
class MessageDtoBuilder private constructor() {

  // Message information
  private val identifier = MessageId()
  private val version = 0L
  private val content = randomUUID().toString()

  // Message Create User information
  private val createdByIdentifier = randomUUID()
  private val createdDate = Date.from(now())

  // Message Modify User information
  private val lastModifiedByIdentifier = randomUUID()
  private val lastModifiedDate = Date.from(now())

  // Topic information
  private val topicIdentifier = TopicId()

  /**
   * Creates the target [MessageDto] from a [Message].
   *
   * @return target messageDto
   */
  fun build(): MessageDto =
      MessageDto(
          identifier,
          version,
          content,
          createdByIdentifier.asUserId(),
          createdDate,
          lastModifiedByIdentifier.asUserId(),
          lastModifiedDate,
          topicIdentifier)

  companion object {
    fun messageDto(): MessageDtoBuilder = MessageDtoBuilder()
  }
}
