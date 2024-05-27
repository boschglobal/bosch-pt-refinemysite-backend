/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.topic.command.snapshotstore

import com.bosch.pt.csm.cloud.common.command.snapshotstore.AbstractSnapshotEntityCache
import com.bosch.pt.iot.smartsite.project.topic.domain.TopicId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.Topic
import com.bosch.pt.iot.smartsite.project.topic.shared.repository.TopicRepository
import org.springframework.stereotype.Component
import org.springframework.web.context.annotation.RequestScope

@Component
@RequestScope
class TopicSnapshotEntityCache(private val repository: TopicRepository) :
    AbstractSnapshotEntityCache<TopicId, Topic>() {

  override fun loadOneFromDatabase(identifier: TopicId) = repository.findOneByIdentifier(identifier)

  fun loadAllFromDatabase(identifiers: List<TopicId>) =
      repository.findAllByIdentifierIn(identifiers)
}
