/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretogglemanagement.feature.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.featuretogglemanagement.common.SubjectTypeEnum.PROJECT
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.domain.WhitelistedSubject
import com.bosch.pt.csm.cloud.featuretogglemanagement.feature.query.FeatureQueryService
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@ConditionalOnBean(ParticipantAuthorization::class)
@ApiVersion(from = 1)
@RestController
@RequestMapping("\${custom.feature.endpoint.prefix}")
internal class FeatureController(
    private val featureQueryService: FeatureQueryService,
    private val participantAuthorization: ParticipantAuthorization
) {

  @GetMapping("/projects/{projectIdentifier}/features")
  fun getEnabledFeatures(
      @PathVariable projectIdentifier: String
  ): ResponseEntity<BimFeatureListResource> {
    if (!participantAuthorization.isParticipantOf(projectIdentifier))
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build()

    return ResponseEntity.ok(
        BimFeatureListResource(
            featureQueryService.getEnabledFeatures(WhitelistedSubject(projectIdentifier, PROJECT))))
  }

  @ExceptionHandler(IllegalArgumentException::class)
  fun handleBadRequest() = ResponseEntity.badRequest().build<Void>()
}

typealias FeatureName = String

data class BimFeatureListResource(
    val items: List<FeatureName>,
)
