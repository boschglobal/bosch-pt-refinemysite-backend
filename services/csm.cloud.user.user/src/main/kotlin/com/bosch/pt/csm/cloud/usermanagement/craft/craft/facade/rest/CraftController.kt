/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest

import com.bosch.pt.csm.cloud.common.facade.rest.ApiVersion
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.CraftId
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.command.handler.CreateCraftCommandHandler
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.CraftListResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.CraftResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.CreateCraftResource
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.factory.CraftListResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.factory.CraftResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.facade.rest.resource.factory.MultilingualCraftResourceFactory
import com.bosch.pt.csm.cloud.usermanagement.craft.craft.query.CraftQueryService
import java.util.Locale
import jakarta.validation.Valid
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.data.domain.Pageable
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.created
import org.springframework.http.ResponseEntity.notFound
import org.springframework.http.ResponseEntity.ok
import org.springframework.util.IdGenerator
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.support.ServletUriComponentsBuilder

@ApiVersion(to = 1)
@RestController
class CraftController(
    private val createCraftCommandHandler: CreateCraftCommandHandler,
    private val craftQueryService: CraftQueryService,
    private val multilingualCraftResourceFactory: MultilingualCraftResourceFactory,
    private val craftResourceFactory: CraftResourceFactory,
    private val craftListResourceFactory: CraftListResourceFactory,
    private val idGenerator: IdGenerator
) {

  @GetMapping(CRAFTS_ENDPOINT_PATH)
  fun findAllCraftsByLanguage(
      locale: Locale,
      pageable: Pageable?
  ): ResponseEntity<CraftListResource> =
      craftQueryService.findAllByLanguage(locale.language, pageable!!).let {
        ok(craftListResourceFactory.build(it))
      }

  @GetMapping(CRAFT_BY_CRAFT_ID_ENDPOINT_PATH)
  fun findOneByIdentifier(
      @PathVariable(PATH_VARIABLE_CRAFT_ID) identifier: CraftId
  ): ResponseEntity<CraftResource> =
      craftQueryService.findOneByIdentifier(identifier).let {
        when (it == null) {
          true -> notFound().build()
          else -> ok(craftResourceFactory.build(it, LocaleContextHolder.getLocale())!!)
        }
      }

  @PostMapping(CRAFTS_ENDPOINT_PATH, CRAFT_BY_CRAFT_ID_ENDPOINT_PATH)
  fun create(
      @PathVariable(value = PATH_VARIABLE_CRAFT_ID, required = false) craftId: CraftId?,
      @RequestBody body: @Valid CreateCraftResource
  ): ResponseEntity<*> =
      createCraftCommandHandler
          .handle(body.toCommand(craftId ?: CraftId(idGenerator.generateId())))
          .let { craftQueryService.findOneWithUserAndTranslationsByIdentifier(it)!! }
          .let {
            created(craftLocation(it.identifier)).body(multilingualCraftResourceFactory.build(it))
          }

  private fun craftLocation(identifier: CraftId) =
      ServletUriComponentsBuilder.fromCurrentContextPath()
          .path(CRAFT_BY_CRAFT_ID_ENDPOINT_PATH)
          .buildAndExpand(identifier.toUuid())
          .toUri()

  companion object {
    const val CRAFTS_ENDPOINT_PATH = "/crafts"
    const val CRAFT_BY_CRAFT_ID_ENDPOINT_PATH = "/crafts/{craftId}"
    const val PATH_VARIABLE_CRAFT_ID = "craftId"
  }
}
