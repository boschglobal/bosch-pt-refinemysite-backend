/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.dataimport.featuretoggle.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.api.resource.request.CreateFeatureCommand
import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.api.resource.response.FeatureToggleResource
import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.model.FeatureToggle
import com.bosch.pt.iot.smartsite.dataimport.featuretoggle.rest.FeatureToggleRestClient
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.user.service.CraftImportService
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum.featuretoggle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class FeatureToggleImportService(
    private val existingFeatureFlags: MutableList<FeatureToggleResource> = mutableListOf(),
    private val authenticationService: AuthenticationService,
    private val featureToggleRestClient: FeatureToggleRestClient,
    private val idRepository: IdRepository
) : ImportService<FeatureToggle> {

  override fun importData(data: FeatureToggle) {
    authenticationService.selectAdmin()
    filterExistingFeatureFlag(data)

    if (idRepository.containsId(TypedId.typedId(featuretoggle, data.id))) {
      LOGGER.warn("Skipped existing feature flag (id: " + data.id + ")")
      return
    }

    val toggleResource = call { featureToggleRestClient.create(CreateFeatureCommand(data.name)) }!!
    call { featureToggleRestClient.enableFeature(toggleResource.name) }
    idRepository.store(TypedId.typedId(ResourceTypeEnum.craft, data.id), toggleResource.id)
  }

  fun resetFeatureToggleData() = existingFeatureFlags.clear()

  fun loadExistingFeatureToggles() {
    authenticationService.selectAdmin()

    val featureToggleListResource = call { featureToggleRestClient.existingFeatureToggles() }!!
    existingFeatureFlags.addAll(featureToggleListResource.featureToggles)
  }

  private fun filterExistingFeatureFlag(toggle: FeatureToggle) {
    if (idRepository.containsId(TypedId.typedId(featuretoggle, toggle.id))) return
    val existingFeatureFlag = existingFeatureFlags.firstOrNull { (it.name == toggle.name) }
    existingFeatureFlag?.let {
      idRepository.store(TypedId.typedId(featuretoggle, toggle.id), it.id)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(CraftImportService::class.java)
  }
}
