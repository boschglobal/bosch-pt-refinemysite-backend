/*
 * ************************************************************************
 *
 *  Copyright:       Robert Bosch Power Tools GmbH, 2018 - 2023
 *
 * ************************************************************************
 */
package com.bosch.pt.iot.smartsite.test

import com.bosch.pt.iot.smartsite.company.model.Company
import com.bosch.pt.iot.smartsite.company.model.CompanyBuilder
import com.bosch.pt.iot.smartsite.company.model.Employee
import com.bosch.pt.iot.smartsite.company.model.EmployeeBuilder
import com.bosch.pt.iot.smartsite.company.model.EmployeeBuilder.Companion.employee
import com.bosch.pt.iot.smartsite.company.model.EmployeeRoleEnum
import com.bosch.pt.iot.smartsite.project.participant.shared.model.Participant
import com.bosch.pt.iot.smartsite.project.participant.shared.model.ParticipantRoleEnum
import com.bosch.pt.iot.smartsite.project.project.shared.model.ParticipantBuilder
import com.bosch.pt.iot.smartsite.project.project.shared.model.ParticipantBuilder.Companion.participant
import com.bosch.pt.iot.smartsite.project.project.shared.model.Project
import com.bosch.pt.iot.smartsite.project.project.shared.model.ProjectBuilder
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraft
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftBuilder
import com.bosch.pt.iot.smartsite.project.projectcraft.shared.model.ProjectCraftBuilder.Companion.projectCraft
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskBuilder
import com.bosch.pt.iot.smartsite.project.task.shared.model.TaskBuilder.Companion.task
import com.bosch.pt.iot.smartsite.user.model.User
import com.bosch.pt.iot.smartsite.user.model.UserBuilder
import java.net.MalformedURLException
import java.net.URL
import java.util.Base64
import java.util.UUID.randomUUID
import org.apache.commons.lang3.RandomStringUtils
import org.apache.commons.lang3.RandomUtils
import org.springframework.mock.web.MockMultipartFile

object RandomData {

  private const val RANDOM_STRING_LENGTH = 10

  @JvmStatic
  fun task(project: Project?, assignee: Participant?, projectCraft: ProjectCraft?): TaskBuilder =
      task()
          .withIdentifier(TaskId())
          .withProjectCraft(projectCraft)
          .withProject(project)
          .withAssignee(assignee)

  @JvmStatic
  fun projectCraft(project: Project): ProjectCraftBuilder = projectCraft().withProject(project)

  @JvmStatic
  fun participant(
      employee: Employee,
      project: Project?,
      role: ParticipantRoleEnum?
  ): ParticipantBuilder =
      participant()
          .withCompany(employee.company)
          .withUser(employee.user)
          .withRole(role)
          .withProject(project)

  @JvmStatic
  fun project(): ProjectBuilder =
      ProjectBuilder.project().withTitle(RandomStringUtils.randomAlphanumeric(RANDOM_STRING_LENGTH))

  fun employee(user: User?, company: Company?, role: EmployeeRoleEnum?): EmployeeBuilder =
      employee().withIdentifier(randomUUID()).withUser(user).withCompany(company).withRole(role!!)

  fun company(): CompanyBuilder =
      CompanyBuilder.company()
          .withIdentifier(randomUUID())
          .withName(RandomStringUtils.random(RANDOM_STRING_LENGTH))

  fun user(): UserBuilder =
      UserBuilder.user()
          .withUserId(RandomStringUtils.randomAlphanumeric(RANDOM_STRING_LENGTH))
          .withEmail(RandomStringUtils.randomAlphanumeric(RANDOM_STRING_LENGTH) + "@example.com")

  /** create a small 2x2 pixel PNG (yellow with 0.5 opacity) */
  fun multiPartFile(): MockMultipartFile =
      MockMultipartFile("file", "image2x2.png", "image/png", multiPartFileBytes())

  fun multiPartFileBytes(): ByteArray =
      Base64.getDecoder()
          .decode(
              "iVBORw0KGgoAAAANSUhEUgAAAAIAAAACCAYAAABytg0k" +
                  "AAAAEklEQVR42mP8/5+hngEIGGEMADlqBP1mY/qhAAAAAElFTkSuQmCC")

  fun url(): URL =
      try {
        URL("https://localhost:8080/" + RandomUtils.nextInt(1000, 9999))
      } catch (e: MalformedURLException) {
        throw IllegalStateException(e)
      }
}
