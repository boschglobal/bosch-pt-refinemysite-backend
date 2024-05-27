/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2021
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.application.config

import com.bosch.pt.csm.cloud.common.test.event.EventStreamGenerator
import com.bosch.pt.csm.common.event.CompanyServiceEventStreamContext
import com.bosch.pt.csm.company.employee.command.eventprocessor.RemoveEmployeeOnUserDeletedEventListener
import com.bosch.pt.csm.company.employee.query.employableuser.EmployableUserProjectorEventListener
import com.bosch.pt.csm.company.eventstore.CompanyContextLocalEventBus
import com.bosch.pt.csm.company.eventstore.CompanyContextRestoreSnapshotsEventListener
import com.bosch.pt.csm.user.user.query.UserProjectorEventListener
import org.slf4j.Logger
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.transaction.support.TransactionTemplate

@TestConfiguration
class AllListenerConfiguration(
    private val transactionTemplate: TransactionTemplate,
    private val companyEventBus: CompanyContextLocalEventBus,
    private val logger: Logger
) {

  @Bean
  fun companyContextRestoreEventListener() =
      CompanyContextRestoreSnapshotsEventListener(transactionTemplate, companyEventBus, logger)

  @Bean
  fun eventStreamContext(
      userProjectorEventListener: UserProjectorEventListener,
      employableUserProjectorEventListener: EmployableUserProjectorEventListener,
      removeEmployeeOnUserDeletedEventListener: RemoveEmployeeOnUserDeletedEventListener,
      companyContextRestoreSnapshotsEventListener: CompanyContextRestoreSnapshotsEventListener,
  ) =
      CompanyServiceEventStreamContext(
              HashMap(),
              HashMap(),
              TimeLineGeneratorImpl(),
              mutableMapOf(
                  "user" to
                      listOf(
                          userProjectorEventListener::listenToUserEvents,
                          removeEmployeeOnUserDeletedEventListener::listenToUserEvents,
                          employableUserProjectorEventListener::listenToUserEvents)),
              mutableMapOf(
                  "company" to
                      listOf(
                          companyContextRestoreSnapshotsEventListener::listenToCompanyEvents,
                          employableUserProjectorEventListener::listenToCompanyEvents),
                  "user" to
                      listOf(
                          userProjectorEventListener::listenToUserEvents,
                          employableUserProjectorEventListener::listenToUserEvents)))
          .useRestoreListener()

  @Bean
  fun eventStreamGenerator(eventStreamContext: CompanyServiceEventStreamContext) =
      EventStreamGenerator(eventStreamContext)
}
