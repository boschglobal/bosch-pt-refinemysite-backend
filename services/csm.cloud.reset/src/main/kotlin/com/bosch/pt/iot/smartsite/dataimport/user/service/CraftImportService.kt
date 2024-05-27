/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2020
 *
 *  *************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.user.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.ImportService
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.request.CreateCraftResource
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.request.dto.CraftTranslation
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.CraftListResource
import com.bosch.pt.iot.smartsite.dataimport.user.api.resource.response.CraftResource
import com.bosch.pt.iot.smartsite.dataimport.user.model.Craft
import com.bosch.pt.iot.smartsite.dataimport.user.model.Translation
import com.bosch.pt.iot.smartsite.dataimport.user.rest.CraftRestClient
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class CraftImportService(
    private val existingCrafts: MutableList<CraftResource> = ArrayList(),
    private val craftRestClient: CraftRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : ImportService<Craft> {

  override fun importData(data: Craft) {
    authenticationService.selectAdmin()
    filterExistingCraft(data)

    if (idRepository.containsId(TypedId.typedId(ResourceTypeEnum.craft, data.id))) {
      LOGGER.warn("Skipped existing craft (id: " + data.id + ")")
      return
    }

    val translationResources: MutableSet<CraftTranslation> = HashSet()
    data.translations.forEach { translation: Translation ->
      translationResources.add(CraftTranslation(translation.locale, translation.value))
    }

    val craftResource = call { craftRestClient.create(CreateCraftResource(translationResources)) }!!
    idRepository.store(TypedId.typedId(ResourceTypeEnum.craft, data.id), craftResource.identifier)
  }

  fun loadExistingCrafts() {
    authenticationService.selectAdmin()
    val page = AtomicInteger()
    var craftListResource: CraftListResource

    do {
      craftListResource = call { craftRestClient.existingCrafts(page.getAndIncrement()) }!!
      existingCrafts.addAll(craftListResource.crafts)
    } while (page.get() < craftListResource.totalPages)
  }

  fun resetCraftData() = existingCrafts.clear()

  private fun filterExistingCraft(craft: Craft) {
    if (idRepository.containsId(TypedId.typedId(ResourceTypeEnum.craft, craft.id))) {
      return
    }

    val existingCraft =
        existingCrafts.firstOrNull {
          (it.name ==
              craft.translations
                  .first { translation: Translation ->
                    translation.locale == Locale.UK.language.lowercase(Locale.getDefault())
                  }
                  .value)
        }

    existingCraft?.let {
      idRepository.store(TypedId.typedId(ResourceTypeEnum.craft, craft.id), it.id)
    }
  }

  companion object {
    private val LOGGER = LoggerFactory.getLogger(CraftImportService::class.java)
  }
}
