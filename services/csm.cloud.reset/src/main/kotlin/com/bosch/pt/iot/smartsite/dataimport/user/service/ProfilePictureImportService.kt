/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.ProfilePictureResource
import com.bosch.pt.iot.smartsite.dataimport.user.model.ProfilePicture
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.FileSystemResource
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod.POST
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Service
import org.springframework.util.Assert
import org.springframework.util.LinkedMultiValueMap
import org.springframework.util.MultiValueMap
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class ProfilePictureImportService(
    private val restTemplate: RestTemplate,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository,
    @Value("\${csm-cloud-user.url}") private val serviceUrl: String
) : ImportService<ProfilePicture> {

  override fun importData(data: ProfilePicture) {
    authenticationService.selectUser(data.userId)
    val resource = data.resource ?: FileSystemResource(data.path)
    val userId = idRepository[TypedId.typedId(ResourceTypeEnum.user, data.userId)]

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
            .path("v3/users/current/picture")
            .buildAndExpand(userId)

    val responseEntity =
        restTemplate.exchange(
            uriBuilder.toUriString(), POST, httpEntity, ProfilePictureResource::class.java)

    Assert.isTrue(
        HttpStatus.CREATED == responseEntity.statusCode,
        "Upload of profile picture failed: " + resource.filename)
  }
}
