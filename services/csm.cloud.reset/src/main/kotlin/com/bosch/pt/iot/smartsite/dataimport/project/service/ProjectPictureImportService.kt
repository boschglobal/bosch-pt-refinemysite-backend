/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.project.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.project.api.resource.response.ProjectPictureResource
import com.bosch.pt.iot.smartsite.dataimport.project.model.ProjectPicture
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import org.springframework.beans.factory.annotation.Qualifier
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
class ProjectPictureImportService(
    @Qualifier("restTemplate") private val restTemplate: RestTemplate,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository,
    @Value("\${csm-cloud-project.url}") private val serviceUrl: String
) : ImportService<ProjectPicture> {

  override fun importData(data: ProjectPicture) {
    authenticationService.selectUser(data.createWithUserId!!)
    val resource = data.resource ?: FileSystemResource(data.path)
    val projectId = idRepository[TypedId.typedId(ResourceTypeEnum.project, data.projectId)]

    // Set headers
    val httpHeaders = HttpHeaders()
    httpHeaders.contentType = MediaType.MULTIPART_FORM_DATA

    // Add file as multipart upload content
    val parameters: MultiValueMap<String, Any?> = LinkedMultiValueMap()
    parameters.add("file", resource)

    // Set request parameters
    val httpEntity = HttpEntity(parameters, httpHeaders)

    // Upload attachment
    val uriBuilder =
        UriComponentsBuilder.fromHttpUrl(serviceUrl)
            .path("v5/projects/{projectId}/picture")
            .buildAndExpand(projectId)

    val responseEntity =
        restTemplate.exchange(
            uriBuilder.toUriString(),
            HttpMethod.POST,
            httpEntity,
            ProjectPictureResource::class.java)

    Assert.isTrue(
        HttpStatus.CREATED == responseEntity.statusCode,
        "Upload of attachment failed: " + resource.filename)
  }
}
