/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.dataimport.task.service

import com.bosch.pt.iot.smartsite.dataimport.common.service.DateBasedImportService
import com.bosch.pt.iot.smartsite.dataimport.security.service.AuthenticationService
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.request.CreateDayCardResource
import com.bosch.pt.iot.smartsite.dataimport.task.api.resource.response.CancelDayCardResource
import com.bosch.pt.iot.smartsite.dataimport.task.model.DayCard
import com.bosch.pt.iot.smartsite.dataimport.task.model.DayCardReasonEnum.DELAYED_MATERIAL
import com.bosch.pt.iot.smartsite.dataimport.task.model.DayCardStatusEnum.APPROVED
import com.bosch.pt.iot.smartsite.dataimport.task.model.DayCardStatusEnum.DONE
import com.bosch.pt.iot.smartsite.dataimport.task.model.DayCardStatusEnum.NOTDONE
import com.bosch.pt.iot.smartsite.dataimport.task.rest.DayCardRestClient
import com.bosch.pt.iot.smartsite.dataimport.util.IdRepository
import com.bosch.pt.iot.smartsite.dataimport.util.TypedId
import com.bosch.pt.iot.smartsite.importer.boundary.resource.ResourceTypeEnum
import java.time.LocalDate
import java.util.UUID
import org.springframework.stereotype.Service

@Service
class DayCardImportService(
    private val dayCardRestClient: DayCardRestClient,
    private val authenticationService: AuthenticationService,
    private val idRepository: IdRepository
) : DateBasedImportService<DayCard> {

  override fun importData(data: DayCard) = importData(data, LocalDate.now())

  override fun importData(data: DayCard, rootDate: LocalDate) {
    authenticationService.selectUser(data.createWithUserId)
    val taskId = idRepository[TypedId.typedId(ResourceTypeEnum.task, data.taskId)]!!

    val dayCardId = UUID.randomUUID()
    val eTag = data.etag
    call { dayCardRestClient.create(taskId, dayCardId, eTag, map(data, rootDate)) }

    if (data.status != null) {
      when (data.status) {
        DONE -> call { dayCardRestClient.complete(dayCardId, "0") }
        NOTDONE ->
            call {
              dayCardRestClient.cancel(dayCardId, "0", CancelDayCardResource(DELAYED_MATERIAL))
            }
        APPROVED -> call { dayCardRestClient.approve(dayCardId, "0") }
        else -> {
          throw IllegalArgumentException("Unexpected day card status: ${data.status}")
        }
      }
    }
  }

  private fun map(dayCard: DayCard, rootDate: LocalDate): CreateDayCardResource =
      CreateDayCardResource(
          dayCard.title,
          dayCard.manpower,
          rootDate.plusDays(((dayCard.task?.start ?: 0).toLong() + dayCard.date)),
          dayCard.notes)
}
