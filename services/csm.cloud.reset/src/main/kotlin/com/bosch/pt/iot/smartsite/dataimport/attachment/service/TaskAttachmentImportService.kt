/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.attachment.service

import com.bosch.pt.iot.smartsite.dataimport.attachment.api.response.AttachmentResource
import com.bosch.pt.iot.smartsite.dataimport.attachment.model.Attachment
import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.message
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.task
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.topic
import java.net.URI
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class TaskAttachmentImportService(
    private val restTemplate: RestTemplate,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository,
    @Value("\${csm-cloud-project.url}") private val serviceUrl: String
) : ImportService<Attachment> {

  override fun importData(data: Attachment) {
    authenticationService.selectUser(data.createWithUserId!!)
    val resource = data.resource ?: FileSystemResource(data.path)
    val taskId = idRepository[TypedId.typedId(task, data.taskId)]
    val topicId = idRepository[TypedId.typedId(topic, data.topicId)]
    val messageId = idRepository[TypedId.typedId(message, data.messageId)]
    val zoneOffset = data.zoneOffset

    // Set headers
    val httpHeaders = HttpHeaders()
    httpHeaders.contentType = MediaType.MULTIPART_FORM_DATA

    // Add file as multipart upload content
    val parameters: MultiValueMap<String, Any?> = LinkedMultiValueMap()
    parameters.add("file", resource)

    // Set request parameters
    val httpEntity = HttpEntity(parameters, httpHeaders)

    // Upload attachment
    val uri: URI =
        if (taskId != null) {
          UriComponentsBuilder.fromHttpUrl(serviceUrl)
              .path("v5/projects/tasks/{taskId}/attachments")
              .queryParam(ZONE_OFFSET, "{zoneOffset}")
              .encode()
              .buildAndExpand(taskId, zoneOffset)
              .toUri()
        } else if (topicId != null) {
          UriComponentsBuilder.fromHttpUrl(serviceUrl)
              .path("v5/projects/tasks/topics/{topicId}/attachments")
              .queryParam(ZONE_OFFSET, "{zoneOffset}")
              .encode()
              .buildAndExpand(topicId, zoneOffset)
              .toUri()
        } else if (messageId != null) {
          UriComponentsBuilder.fromHttpUrl(serviceUrl)
              .path("v5/projects/tasks/topics/messages/{messageId}/attachments")
              .queryParam(ZONE_OFFSET, "{zoneOffset}")
              .encode()
              .buildAndExpand(messageId, zoneOffset)
              .toUri()
        } else {
          throw IllegalArgumentException("Neither task id nor report id is set")
        }

    val responseEntity =
        restTemplate.exchange(uri, HttpMethod.POST, httpEntity, AttachmentResource::class.java)

    Assert.isTrue(
        HttpStatus.CREATED == responseEntity.statusCode,
        "Upload of attachment failed: " + resource.filename)
  }

  companion object {
    private const val ZONE_OFFSET = "zoneOffset"
  }
}
