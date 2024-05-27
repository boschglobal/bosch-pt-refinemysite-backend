/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2024
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.message.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.message.domain.MessageId
import com.bosch.pt.iot.smartsite.project.message.shared.model.Message
import com.bosch.pt.iot.smartsite.project.message.shared.repository.MessageRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class MessageSnapshotEntityCache(private val repository: MessageRepository) :
    AbstractSnapshotEntityCache<MessageId, Message>() {

  override fun loadOneFromDatabase(identifier: MessageId) =
      repository.findOneByIdentifier(identifier)

  fun loadAllFromDatabase(identifiers: List<MessageId>) =
      repository.findAllByIdentifierIn(identifiers)
}
