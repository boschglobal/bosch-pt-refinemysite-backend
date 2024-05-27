/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */

package com.bosch.pt.iot.smartsite.project.rfv.boundary

import com.bosch.pt.csm.cloud.common.command.DenyWebRequests
import com.bosch.pt.csm.cloud.common.exceptions.PreconditionViolationException
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.CREATED
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.DELETED
import com.bosch.pt.csm.cloud.projectmanagement.rfv.messages.RfvCustomizationEventEnumAvro.UPDATED
import com.bosch.pt.iot.smartsite.application.security.NoPreAuthorize
import com.bosch.pt.iot.smartsite.common.i18n.Key.RFV_VALIDATION_ERROR_DEACTIVATION_NOT_POSSIBLE
import com.bosch.pt.iot.smartsite.project.daycard.shared.model.DayCardReasonEnum
import com.bosch.pt.iot.smartsite.project.project.ProjectId
import com.bosch.pt.iot.smartsite.project.project.shared.repository.ProjectRepository
import com.bosch.pt.iot.smartsite.project.rfv.boundary.dto.RfvDto
import com.bosch.pt.iot.smartsite.project.rfv.boundary.dto.UpdateRfvDto
import com.bosch.pt.iot.smartsite.project.rfv.model.RfvCustomization
import com.bosch.pt.iot.smartsite.project.rfv.repository.RfvCustomizationRepository
import datadog.trace.api.Trace
import java.util.UUID
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation.MANDATORY
import org.springframework.transaction.annotation.Transactional
import org.springframework.util.IdGenerator

/**
 * This service allows to update rfv settings for a project. There is a fixed list of rfvs (defined
 * by the [DayCardReasonEnum]) that can be customized with this service. Rfvs are devided into
 * standard rfvs and custom rfvs. Users can activate / deactivate all rfvs. Deactivated rfvs can not
 * be used for not done day cards. Furthermore, users can change the names of the custom rfvs.
 * Custom rfvs are deactivated by default and have to be activated by the user to be able to use
 * them. Standard rfvs are activated by default.
 */
@Service
open class RfvService(
    private val rfvCustomizationRepository: RfvCustomizationRepository,
    private val projectRepository: ProjectRepository,
    private val idGenerator: IdGenerator,
    private val messageSource: MessageSource
) {

  @Trace
  @Transactional
  @PreAuthorize(
      "@rfvAuthorizationComponent.hasUpdateRfvPermissionOnProject(#rfvDto.projectIdentifier)")
  open fun update(rfvDto: UpdateRfvDto): RfvDto {

    assertAtLeastOneActiveRfvRemains(rfvDto.projectIdentifier, rfvDto)

    val rfvCustomization =
        rfvCustomizationRepository.findOneByKeyAndProjectIdentifier(
            rfvDto.key, rfvDto.projectIdentifier)

    return if (rfvDto.key.isCustom) updateCustomRfv(rfvCustomization, rfvDto)
    else updateStandardRfv(rfvCustomization, rfvDto)
  }

  private fun updateStandardRfv(rfvCustomization: RfvCustomization?, rfv: UpdateRfvDto): RfvDto =
      if (rfvCustomization == null) {
        if (rfv.active) {
          RfvDto(rfv.key, true, null)
        } else {
          val project = projectRepository.findOneByIdentifier(rfv.projectIdentifier)!!
          RfvCustomization(project, rfv.key, rfv.active, null)
              .apply { identifier = idGenerator.generateId() }
              .let {
                rfvCustomizationRepository.save(it, CREATED)
                RfvDto(it.key, it.active, it.name)
              }
        }
      } else {
        if (rfv.active) {
          rfvCustomizationRepository.delete(rfvCustomization, DELETED)
          RfvDto(rfv.key, true, null)
        } else {
          RfvDto(rfv.key, rfvCustomization.active, null)
        }
      }

  private fun updateCustomRfv(rfvCustomization: RfvCustomization?, rfv: UpdateRfvDto): RfvDto =
      if (rfvCustomization == null) {
        if (rfv.active || !rfv.name.isNullOrEmpty()) {
          val project = projectRepository.findOneByIdentifier(rfv.projectIdentifier)!!
          RfvCustomization(project, rfv.key, rfv.active, rfv.name)
              .apply { identifier = idGenerator.generateId() }
              .let {
                rfvCustomizationRepository.save(it, CREATED)
                RfvDto(it.key, it.active, it.name)
              }
        } else {
          RfvDto(rfv.key, false, null)
        }
      } else {
        if (rfv.active || !rfv.name.isNullOrEmpty()) {
          rfvCustomization.active = rfv.active
          rfvCustomization.name = rfv.name
          rfvCustomizationRepository.save(rfvCustomization, UPDATED)
          RfvDto(rfvCustomization.key, rfvCustomization.active, rfvCustomization.name)
        } else {
          rfvCustomizationRepository.delete(rfvCustomization, DELETED)
          RfvDto(rfv.key, false, null)
        }
      }

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@rfvAuthorizationComponent.hasViewPermissionOnRfv(#projectIdentifier)")
  open fun findAll(projectIdentifier: ProjectId): List<RfvDto> {
    val rfvCustomizations =
        rfvCustomizationRepository.findAllByProjectIdentifier(projectIdentifier).associateBy {
          it.key
        }

    return DayCardReasonEnum.values().map {
      val customization = rfvCustomizations[it]
      when (customization == null) {
        true -> RfvDto(it, !it.isCustom)
        else -> RfvDto(it, customization.active, customization.name)
      }
    }
  }

  @Trace
  @NoPreAuthorize(usedByController = true)
  @Transactional(readOnly = true)
  open fun findOneWithDetailsByIdentifier(identifier: UUID): RfvCustomization? =
      rfvCustomizationRepository.findOneWithDetailsByIdentifier(identifier)

  @Trace
  @Transactional(readOnly = true)
  @PreAuthorize("@rfvAuthorizationComponent.hasViewPermissionOnRfv(#projectIdentifier)")
  open fun resolveProjectRfvs(projectIdentifier: ProjectId): Map<DayCardReasonEnum, String> =
      findAll(projectIdentifier).associate {
        it.key to
            (it.name
                ?: messageSource.getMessage(
                    "DayCardReasonEnum_" + it.key.name, null, LocaleContextHolder.getLocale()))
      }

  @Trace
  @NoPreAuthorize
  @DenyWebRequests
  @Transactional(propagation = MANDATORY)
  open fun deleteRfvCustomizationsByProjectIdentifier(projectIdentifier: ProjectId) {
    rfvCustomizationRepository.findAllByProjectIdentifier(projectIdentifier).also {
      rfvCustomizationRepository.deleteAllInBatch(it)
    }
  }

  private fun assertAtLeastOneActiveRfvRemains(projectIdentifier: ProjectId, rfvDto: UpdateRfvDto) {
    if (rfvDto.active) return
    val activeRfvs = findAll(projectIdentifier).filter { it.active }.associateBy { it.key }
    if (activeRfvs.size <= 1 && activeRfvs.containsKey(rfvDto.key))
        throw PreconditionViolationException(RFV_VALIDATION_ERROR_DEACTIVATION_NOT_POSSIBLE)
  }
}
