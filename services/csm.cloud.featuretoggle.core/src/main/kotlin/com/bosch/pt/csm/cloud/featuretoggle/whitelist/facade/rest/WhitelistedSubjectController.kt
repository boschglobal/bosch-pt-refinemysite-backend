/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.FeatureController.Companion.PATH_VARIABLE_FEATURE_NAME
import com.bosch.pt.csm.cloud.featuretoggle.feature.query.FeatureQueryService
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api.AddSubjectToWhitelistCommand
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.api.DeleteSubjectFromWhitelistCommand
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.handler.AddSubjectToWhitelistCommandHandler
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.command.handler.DeleteSubjectFromWhitelistCommandHandler
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.request.CreateWhitelistedSubjectResource
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.FeatureToggleSubjectResource
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.WhitelistedSubjectIdResource
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.WhitelistedSubjectResource
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.factory.FeatureToggleSubjectBatchResourceFactory
import com.bosch.pt.csm.cloud.featuretoggle.whitelist.facade.rest.resource.response.factory.WhitelistedSubjectResourceFactory
import java.util.UUID
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ApiVersion
@RestController
class WhitelistedSubjectController(
    private val addHandler: AddSubjectToWhitelistCommandHandler,
    private val deleteHandler: DeleteSubjectFromWhitelistCommandHandler,
    private val featureQueryService: FeatureQueryService,
    private val featureToggleSubjectBatchResourceFactory: FeatureToggleSubjectBatchResourceFactory,
    private val whitelistedSubjectResourceFactory: WhitelistedSubjectResourceFactory
) {

  @GetMapping(WHITELIST_BY_SUBJECT_ENDPOINT)
  fun findWhitelistInformation(
      @PathVariable(PATH_VARIABLE_SUBJECT_ID) subjectId: UUID
  ): ResponseEntity<BatchResponseResource<FeatureToggleSubjectResource>> =
      ResponseEntity.ok()
          .body(
              featureToggleSubjectBatchResourceFactory.build(
                  featureQueryService.findAllExceptDisabledFeaturesWithDetails(), subjectId))

  @PutMapping(WHITELISTED_SUBJECT_ENDPOINT)
  fun addSubjectToFeatureWhitelist(
      @PathVariable(PATH_VARIABLE_FEATURE_NAME) featureName: String,
      @PathVariable(PATH_VARIABLE_SUBJECT_ID) subjectId: UUID,
      @Valid @RequestBody body: CreateWhitelistedSubjectResource
  ): ResponseEntity<WhitelistedSubjectResource> {
    val featureId =
        addHandler.handle(AddSubjectToWhitelistCommand(featureName, subjectId, body.type))
    val whitelistedSubject =
        whitelistedSubjectResourceFactory.build(
            requireNotNull(featureQueryService.findByFeatureIdWithDetails(featureId))
                .whitelistedSubjects
                .first { it.subjectRef == subjectId },
            featureId)
    return ResponseEntity.ok().body(whitelistedSubject)
  }

  @DeleteMapping(WHITELISTED_SUBJECT_ENDPOINT)
  fun deleteSubjectFromFeatureWhitelist(
      @PathVariable(PATH_VARIABLE_FEATURE_NAME) featureName: String,
      @PathVariable(PATH_VARIABLE_SUBJECT_ID) subjectId: UUID
  ): ResponseEntity<WhitelistedSubjectIdResource> {
    val featureId = deleteHandler.handle(DeleteSubjectFromWhitelistCommand(featureName, subjectId))
    return ResponseEntity.ok().body(WhitelistedSubjectIdResource(subjectId, featureId))
  }

  companion object {
    private const val WHITELISTED_SUBJECT_ENDPOINT = "/features/{featureName}/subjects/{subjectId}"
    private const val WHITELIST_BY_SUBJECT_ENDPOINT = "/features/subjects/{subjectId}"
    private const val PATH_VARIABLE_SUBJECT_ID = "subjectId"
  }
}
