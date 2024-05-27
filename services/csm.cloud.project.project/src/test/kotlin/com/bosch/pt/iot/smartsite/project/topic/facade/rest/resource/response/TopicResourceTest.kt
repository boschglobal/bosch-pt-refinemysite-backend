package com.bosch.pt.iot.smartsite.project.topic.facade.rest.resource.response

import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReference
import com.bosch.pt.csm.cloud.common.facade.rest.resource.response.ResourceReferenceWithPicture
import com.bosch.pt.iot.smartsite.project.task.domain.TaskId
import com.bosch.pt.iot.smartsite.project.topic.shared.model.TopicCriticalityEnum
import java.net.URI
import java.sql.Date
import java.time.LocalDate
import java.util.UUID.randomUUID
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TopicResourceTest {

  @Test
  fun `verify that createdBy comes with a picture`() {
    val createdBy = ResourceReference(randomUUID(), "test")
    val lastModifiedBy = ResourceReference(randomUUID(), "test")

    val resource =
        TopicResource(
            randomUUID(),
            0L,
            Date.valueOf(LocalDate.now()),
            createdBy,
            Date.valueOf(LocalDate.now()),
            lastModifiedBy,
            TaskId(),
            TopicCriticalityEnum.CRITICAL,
            "description",
            0L,
            URI.create("https://example.com"))

    assertThat(resource.createdBy).isOfAnyClassIn(ResourceReferenceWithPicture::class.java)
    assertThat(resource.lastModifiedBy).isOfAnyClassIn(ResourceReference::class.java)
  }
}
