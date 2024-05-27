/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2022
 *
 * ************************************************************************
 */

package com.bosch.pt.csm.cloud.job.job.command.handler

import com.bosch.pt.csm.cloud.job.job.api.CompleteJobCommand
import com.bosch.pt.csm.cloud.job.job.api.EnqueueJobCommand
import com.bosch.pt.csm.cloud.job.job.api.FailJobCommand
import com.bosch.pt.csm.cloud.job.job.api.JobCommand
import com.bosch.pt.csm.cloud.job.job.api.MarkJobResultReadCommand
import com.bosch.pt.csm.cloud.job.job.api.StartJobCommand
import com.bosch.pt.csm.cloud.job.job.command.handler.exception.InvalidJobStateTransitionException
import java.time.Clock
import java.time.LocalDateTime
import org.slf4j.Logger
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class JobCommandDispatcher(
    private val enqueueJobCommandHandler: EnqueueJobCommandHandler,
    private val startJobCommandHandler: StartJobCommandHandler,
    private val completeJobCommandHandler: CompleteJobCommandHandler,
    private val failJobCommandHandler: FailJobCommandHandler,
    private val markJobResultReadCommandHandler: MarkJobResultReadCommandHandler,
    private val clock: Clock,
    private val logger: Logger
) {
  @Transactional
  fun dispatch(command: JobCommand) {
    val now = LocalDateTime.now(clock)
    try {
      doDispatch(command, now)
    } catch (e: InvalidJobStateTransitionException) {
      logger.warn(
          "Dropping command: ${command.javaClass.simpleName} for ${command.jobIdentifier}: ${e.logMessage}")
    }
  }

  private fun doDispatch(command: JobCommand, now: LocalDateTime) {
    when (command) {
      is EnqueueJobCommand -> enqueueJobCommandHandler.handle(command, now)
      is StartJobCommand -> startJobCommandHandler.handle(command, now)
      is CompleteJobCommand -> completeJobCommandHandler.handle(command, now)
      is FailJobCommand -> failJobCommandHandler.handle(command, now)
      is MarkJobResultReadCommand -> markJobResultReadCommandHandler.handle(command, now)
    }
  }
}
