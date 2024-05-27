package com.bosch.pt.iot.smartsite.mpxj

import com.bosch.pt.csm.cloud.common.extensions.toUUID
import com.bosch.pt.iot.smartsite.project.exporter.api.ProjectExportFormatEnum.MS_PROJECT_XML
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.jupiter.api.Test

/**
 * This class checks that IDs are written to / read from a MS Project xml file using mpxj as we
 * expect it.
 */
class MsProjectIdTest : MpxjIdBaseTest() {

  companion object {
    val parentGuid = "3e330b47-d7d8-45ba-9a1c-ab5628c3a344".toUUID()
    val child1Guid = "9a0bc821-e9d9-4c7c-a5d8-99cbecb43bb7".toUUID()
    val child2Guid = "0e9b1c93-b82c-4b54-8d23-c2af095a19ae".toUUID()
    val child3Guid = "f69d8db2-725c-4132-9176-814d522d70bf".toUUID()

    const val printXml = false
  }

  @Test
  fun `id in ms project with sync`() {
    testIds(
        printXml,
        MS_PROJECT_XML,
        true,
        DirectInsertNode(
            "Parent",
            5,
            6,
            parentGuid,
            listOf(
                DirectInsertNode(
                    "Child1",
                    100,
                    101,
                    child1Guid,
                    listOf(DirectInsertNode("Child3", 10000, 10001, child3Guid, emptyList()))),
                DirectInsertNode("Child2", 1000, 1001, child2Guid, emptyList()))),
        ValidationNode(
            "Parent",
            1,
            6,
            parentGuid,
            listOf(
                ValidationNode(
                    "Child1",
                    2,
                    101,
                    child1Guid,
                    listOf(ValidationNode("Child3", 3, 10001, child3Guid, emptyList()))),
                ValidationNode("Child2", 4, 1001, child2Guid, emptyList()))))
  }

  @Test
  fun `id in ms project with sync ids reversed`() {
    testIds(
        printXml,
        MS_PROJECT_XML,
        true,
        DirectInsertNode(
            "Parent",
            100,
            101,
            parentGuid,
            listOf(
                AfterDirectInsertNode(
                    "Child1",
                    5,
                    6,
                    child1Guid,
                    listOf(DirectInsertNode("Child3", 1000, 1001, child3Guid, emptyList()))),
                DirectInsertNode("Child2", 10000, 10001, child2Guid, emptyList()))),
        ValidationNode(
            "Parent",
            1,
            101,
            parentGuid,
            listOf(
                ValidationNode("Child2", 2, 10001, child2Guid, emptyList()),
                ValidationNode(
                    "Child1",
                    3,
                    6,
                    child1Guid,
                    listOf(ValidationNode("Child3", 4, 1001, child3Guid, emptyList()))))))
  }

  @Test
  fun `id in ms project without sync`() {
    testIds(
        printXml,
        MS_PROJECT_XML,
        false,
        DirectInsertNode(
            "Parent",
            5,
            6,
            parentGuid,
            listOf(
                DirectInsertNode(
                    "Child1",
                    100,
                    101,
                    child1Guid,
                    listOf(DirectInsertNode("Child3", 1000, 1001, child3Guid, emptyList()))),
                DirectInsertNode("Child2", 10000, 10001, child2Guid, emptyList()))),
        ValidationNode(
            "Parent",
            5,
            6,
            parentGuid,
            listOf(
                ValidationNode(
                    "Child1",
                    100,
                    101,
                    child1Guid,
                    listOf(ValidationNode("Child3", 1000, 1001, child3Guid, emptyList()))),
                ValidationNode("Child2", 10000, 10001, child2Guid, emptyList()))))
  }

  @Test
  fun `id in ms project without sync - changed order`() {
    testIds(
        printXml,
        MS_PROJECT_XML,
        false,
        DirectInsertNode(
            "Parent",
            5,
            6,
            parentGuid,
            listOf(
                AfterDirectInsertNode(
                    "Child1",
                    100,
                    101,
                    child1Guid,
                    listOf(DirectInsertNode("Child3", 1000, 1001, child3Guid, emptyList()))),
                DirectInsertNode("Child2", 10000, 10001, child2Guid, emptyList()))),
        ValidationNode(
            "Parent",
            5,
            6,
            parentGuid,
            listOf(
                ValidationNode(
                    "Child1",
                    100,
                    101,
                    child1Guid,
                    listOf(ValidationNode("Child3", 1000, 1001, child3Guid, emptyList()))),
                ValidationNode("Child2", 10000, 10001, child2Guid, emptyList()))))
  }

  // This is an invalid export as it breaks the structure
  @Test
  fun `id in ms project without sync reversed - invalid order`() {
    assertThatExceptionOfType(IllegalArgumentException::class.java)
        .isThrownBy {
          testIds(
              printXml,
              MS_PROJECT_XML,
              false,
              DirectInsertNode(
                  "Parent",
                  100,
                  101,
                  parentGuid,
                  listOf(
                      DirectInsertNode(
                          "Child1",
                          5,
                          6,
                          child1Guid,
                          listOf(
                              DirectInsertNode("Child3", 10000, 10001, child3Guid, emptyList()))),
                      DirectInsertNode("Child2", 1000, 1001, child2Guid, emptyList()))),
              ValidationNode("Parent", 100, 101, parentGuid, listOf()))
        }
        .withMessage("Root children size is: 2, expected: 1")
  }
}
