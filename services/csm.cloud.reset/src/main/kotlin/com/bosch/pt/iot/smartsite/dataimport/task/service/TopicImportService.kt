/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateTopicResource
import com.bosch.pt.iot.smartsite.dataimport.task.model.Topic
import com.bosch.pt.iot.smartsite.dataimport.task.rest.TopicRestClient
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import org.springframework.stereotype.Service

@Service
class TopicImportService(
    private val topicRestClient: TopicRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : ImportService<Topic> {

  override fun importData(data: Topic) {
    authenticationService.selectUser(data.createWithUserId)
    val taskId = idRepository[TypedId.typedId(ResourceTypeEnum.task, data.taskId)]!!
    val createdTopic = call { topicRestClient.create(taskId, map(data)) }!!
    idRepository.store(TypedId.typedId(ResourceTypeEnum.topic, data.id), createdTopic.id)
  }

  private fun map(topic: Topic): CreateTopicResource =
      CreateTopicResource(topic.description, topic.criticality)
}
