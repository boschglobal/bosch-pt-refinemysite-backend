/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.common.facade.rest.LinkUtils.getCurrentApiVersionPrefix
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.BatchResponseResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.ActivateFeatureWhitelistCommand
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.CreateFeatureCommand
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.DeleteFeatureCommand
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.DisableFeatureCommand
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.api.EnableFeatureCommand
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.handler.ActivateFeatureWhitelistCommandHandler
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.handler.CreateFeatureCommandHandler
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.handler.DeleteFeatureCommandHandler
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.handler.DisableFeatureCommandHandler
import com.bosch.pt.csm.cloud.featuretoggle.feature.command.handler.EnableFeatureCommandHandler
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.request.CreateFeatureResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.FeatureIdResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.FeatureResource
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.factory.FeatureBatchResourceFactory
import com.bosch.pt.csm.cloud.featuretoggle.feature.facade.rest.resource.response.factory.FeatureResourceFactory
import com.bosch.pt.csm.cloud.featuretoggle.feature.query.FeatureQueryService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder.fromCurrentContextPath

@ApiVersion
@RestController
@Suppress("LongParameterList")
class FeatureController(
    private val createFeatureHandler: CreateFeatureCommandHandler,
    private val enableFeatureHandler: EnableFeatureCommandHandler,
    private val activateFeatureWhitelistHandler: ActivateFeatureWhitelistCommandHandler,
    private val disableFeatureHandler: DisableFeatureCommandHandler,
    private val deleteFeatureCommandHandler: DeleteFeatureCommandHandler,
    private val featureBatchResourceFactory: FeatureBatchResourceFactory,
    private val featureResourceFactory: FeatureResourceFactory,
    private val featureQueryService: FeatureQueryService
) {

  @GetMapping(FEATURES_ENDPOINT_PATH)
  fun getFeatures(): ResponseEntity<BatchResponseResource<FeatureResource>> =
      ResponseEntity.ok()
          .body(featureBatchResourceFactory.build(featureQueryService.findAllFeatures()))

  @PostMapping(FEATURES_ENDPOINT_PATH)
  fun createFeature(
      @RequestBody @Valid body: CreateFeatureResource
  ): ResponseEntity<FeatureResource> {
    val featureId = createFeatureHandler.handle(CreateFeatureCommand(body.name))
    val feature = featureQueryService.findByFeatureId(featureId)
    val location =
        fromCurrentContextPath()
            .path(getCurrentApiVersionPrefix() + FEATURE_ENDPOINT_PATH)
            .buildAndExpand(body.name)
            .toUri()
    return ResponseEntity.created(location)
        .body(featureResourceFactory.build(requireNotNull(feature)))
  }

  @PostMapping(ENABLE_FEATURE_ENDPOINT_PATH)
  fun enableFeature(
      @PathVariable(PATH_VARIABLE_FEATURE_NAME) featureName: String,
  ): ResponseEntity<FeatureResource> {

    val featureId = enableFeatureHandler.handle(EnableFeatureCommand(featureName))
    val feature = featureQueryService.findByFeatureId(featureId)
    return ResponseEntity.ok().body(featureResourceFactory.build(requireNotNull(feature)))
  }

  @PostMapping(DISABLE_FEATURE_ENDPOINT_PATH)
  fun disableFeature(
      @PathVariable(PATH_VARIABLE_FEATURE_NAME) featureName: String,
  ): ResponseEntity<FeatureResource> {
    val featureId = disableFeatureHandler.handle(DisableFeatureCommand(featureName))
    val feature = featureQueryService.findByFeatureId(featureId)
    return ResponseEntity.ok().body(featureResourceFactory.build(requireNotNull(feature)))
  }

  @PostMapping(ACTIVATE_WHITELIST_ENDPOINT_PATH)
  fun activateFeatureWhitelist(
      @PathVariable(PATH_VARIABLE_FEATURE_NAME) featureName: String,
  ): ResponseEntity<FeatureResource> {
    val featureId =
        activateFeatureWhitelistHandler.handle(ActivateFeatureWhitelistCommand(featureName))
    val feature = featureQueryService.findByFeatureId(featureId)
    return ResponseEntity.ok().body(featureResourceFactory.build(requireNotNull(feature)))
  }

  @DeleteMapping(FEATURE_ENDPOINT_PATH)
  fun deleteFeature(
      @PathVariable(PATH_VARIABLE_FEATURE_NAME) featureName: String
  ): ResponseEntity<FeatureIdResource> =
      ResponseEntity.ok()
          .body(
              FeatureIdResource(
                  deleteFeatureCommandHandler.handle(DeleteFeatureCommand(featureName))))

  companion object {
    const val FEATURES_ENDPOINT_PATH = "/features"
    const val ENABLE_FEATURE_ENDPOINT_PATH = "/features/{featureName}/enable"
    const val DISABLE_FEATURE_ENDPOINT_PATH = "/features/{featureName}/disable"
    const val ACTIVATE_WHITELIST_ENDPOINT_PATH = "/features/{featureName}/activate-whitelist"
    const val FEATURE_ENDPOINT_PATH = "/features/{featureName}"
    const val PATH_VARIABLE_FEATURE_NAME = "featureName"
  }
}
