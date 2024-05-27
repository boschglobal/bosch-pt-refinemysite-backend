#  *************************************************************************
#
#   Copyright:       Robert Bosch Power Tools GmbH, 2020
#
#   *************************************************************************

import json

topicFile = open("import-topic.json", "r")
topicJson = json.load(topicFile)


def findTopic(name):
    for topic in topicJson:
        if topic['id'] == name:
            return topic


def determineProjectFromTaskId(taskId):
    if taskId.startswith("project1") or taskId.startswith("project3"):
        return "daniel"
    if taskId.startswith("project2") or taskId.startswith("project4"):
        return "anne"


with open('import-message.json') as json_file:
    inputJson = json.load(json_file)
    outputArray = []
    for entry in inputJson:
        topic = findTopic(entry['topicId'])
        entry['createWithUserId'] = determineProjectFromTaskId(topic['taskId'])
        outputArray.append(entry)
    json = json.dumps(outputArray, indent=4)
    print(json)

topicFile.close()

outputFile = open("import-message-new.json", "a")
outputFile.write(json)
outputFile.close()

