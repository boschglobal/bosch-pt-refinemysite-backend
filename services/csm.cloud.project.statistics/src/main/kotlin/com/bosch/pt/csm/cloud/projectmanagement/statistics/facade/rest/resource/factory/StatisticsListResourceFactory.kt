/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.factory

import com.bosch.pt.csm.cloud.projectmanagement.application.security.AuthorizationUtils.hasRoleAdmin
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.NamedObjectService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.ParticipantMappingService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.boundary.RfvCustomizationService
import com.bosch.pt.csm.cloud.projectmanagement.statistics.facade.rest.resource.StatisticsListResource
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.DayCardReasonVarianceEnum
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.ParticipantMapping
import com.bosch.pt.csm.cloud.projectmanagement.statistics.model.TimeFrame
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Component

@Component
class StatisticsListResourceFactory(
    private val metricsResourceFactory: MetricsResourceFactory,
    private val namedObjectService: NamedObjectService,
    private val statisticsResourceFactory: StatisticsResourceFactory,
    private val timeMetricsListResourceFactory: TimeMetricsListResourceFactory,
    private val participantMappingService: ParticipantMappingService,
    private val rfvCustomizationService: RfvCustomizationService
) {

  @SuppressWarnings("kotlin:S107")
  fun buildUngrouped(
      projectId: UUID,
      startDate: LocalDate,
      endDate: LocalDate,
      duration: Long,
      ppcTotal: Long?,
      ppcPerWeek: Map<TimeFrame, Long?>?,
      rfvTotal: Map<DayCardReasonVarianceEnum, Long>?,
      rfvPerWeek: Map<TimeFrame, Map<DayCardReasonVarianceEnum, Long>>?
  ): StatisticsListResource {

    val translatedRfvs = rfvCustomizationService.resolveProjectRfvs(projectId)

    return StatisticsListResource(
        listOf(
            statisticsResourceFactory.build(
                startDate,
                endDate,
                timeMetricsListResourceFactory.build(
                    startDate,
                    duration,
                    ppcPerWeek ?: emptyMap(),
                    rfvPerWeek ?: emptyMap(),
                    translatedRfvs),
                metricsResourceFactory.build(ppcTotal, rfvTotal, translatedRfvs))))
  }

  @SuppressWarnings("kotlin:S107")
  fun buildGrouped(
      projectId: UUID,
      startDate: LocalDate,
      endDate: LocalDate,
      duration: Long,
      ppcTotals: Map<Pair<UUID?, UUID>, Long?>,
      ppcPerWeek: Map<Triple<UUID?, UUID, TimeFrame>, Long?>,
      rfvTotals: Map<Pair<UUID?, UUID>, Map<DayCardReasonVarianceEnum, Long>>,
      rfvPerWeek: Map<Triple<UUID?, UUID, TimeFrame>, Map<DayCardReasonVarianceEnum, Long>>,
  ): StatisticsListResource {

    // Join all company UUID's and craft UUID's from ppc and rfv data in one unique set to work with
    val companyIdentifiers: MutableSet<UUID?> = HashSet()
    val craftIdentifiers: MutableSet<UUID> = HashSet()
    val companyCraftPairSet: MutableSet<Pair<UUID?, UUID>> = HashSet()

    companyCraftPairSet.addAll(ppcTotals.keys)
    companyIdentifiers.addAll(ppcTotals.keys.map { it.first })
    craftIdentifiers.addAll(ppcTotals.keys.map { it.second })

    companyCraftPairSet.addAll(rfvTotals.keys)
    companyIdentifiers.addAll(rfvTotals.keys.map { it.first })
    craftIdentifiers.addAll(rfvTotals.keys.map { it.second })

    val companiesMap =
        namedObjectService.findCompanyNames(companyIdentifiers.filterNotNull()).associateBy {
          it.getIdentifierUuid()
        }

    val craftsMap =
        namedObjectService.findProjectCraftNames(craftIdentifiers).associateBy {
          it.getIdentifierUuid()
        }

    val participant =
        participantMappingService.findParticipantMappingByProjectAndCurrentUser(projectId)

    val translatedRfvs = rfvCustomizationService.resolveProjectRfvs(projectId)

    return StatisticsListResource(
        companyCraftPairSet
            .filter { filterAuthorizedCompanies(it.first, participant) }
            .map { companyCraftPair ->
              statisticsResourceFactory.build(
                  startDate,
                  endDate,
                  timeMetricsListResourceFactory.build(
                      startDate,
                      duration,
                      filterPpcOfCompany(ppcPerWeek, companyCraftPair),
                      filterRfvOfCompany(rfvPerWeek, companyCraftPair),
                      translatedRfvs),
                  metricsResourceFactory.build(
                      ppcTotals[companyCraftPair], rfvTotals[companyCraftPair], translatedRfvs),
                  companiesMap[companyCraftPair.first],
                  craftsMap[companyCraftPair.second])
            })
  }

  private fun filterPpcOfCompany(
      ppcMetric: Map<Triple<UUID?, UUID, TimeFrame>, Long?>,
      companyCraftPair: Pair<UUID?, UUID>
  ) =
      ppcMetric
          .filterKeys { it.first == companyCraftPair.first && it.second == companyCraftPair.second }
          .mapKeys { it.key.third }

  private fun filterRfvOfCompany(
      rfvMetric: Map<Triple<UUID?, UUID, TimeFrame>, Map<DayCardReasonVarianceEnum, Long>>,
      companyCraftPair: Pair<UUID?, UUID>
  ) =
      rfvMetric
          .filterKeys { it.first == companyCraftPair.first && it.second == companyCraftPair.second }
          .mapKeys { it.key.third }

  private fun filterAuthorizedCompanies(
      companyIdentifier: UUID?,
      participant: ParticipantMapping?
  ) =
      hasRoleAdmin() ||
          participant?.let { CSM == it.participantRole || isCrOrFmOfCompany(it, companyIdentifier) }
              ?: false

  private fun isCrOrFmOfCompany(participant: ParticipantMapping, companyIdentifier: UUID?) =
      setOf(CR, FM).contains(participant.participantRole) &&
          participant.companyIdentifier == companyIdentifier

  companion object {
    private const val CSM = "CSM"
    private const val CR = "CR"
    private const val FM = "FM"
  }
}
