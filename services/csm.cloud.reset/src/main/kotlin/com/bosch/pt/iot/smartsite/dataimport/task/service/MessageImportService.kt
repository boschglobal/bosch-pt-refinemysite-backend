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
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateMessageResource
import com.bosch.pt.iot.smartsite.dataimport.task.model.Message
import com.bosch.pt.iot.smartsite.dataimport.task.rest.MessageRestClient
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import org.springframework.stereotype.Service

@Service
class MessageImportService(
    private val messageRestClient: MessageRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : ImportService<Message> {

  override fun importData(data: Message) {
    authenticationService.selectUser(data.createWithUserId!!)
    val topicId = idRepository[TypedId.typedId(ResourceTypeEnum.topic, data.topicId)]!!

    idRepository.store(
        TypedId.typedId(ResourceTypeEnum.message, data.id),
        call { messageRestClient.create(topicId, map(data)) }!!.id)
  }

  private fun map(message: Message): CreateMessageResource = CreateMessageResource(message.content)
}
