#  *************************************************************************
#
#   Copyright:       Robert Bosch Power Tools GmbH, 2020
#
#   *************************************************************************

import json

with open('import-daycard.json') as json_file:
    input = json.load(json_file)
    tasks = []
    for p in input:
        if p['taskId'].startswith("project1") or p['taskId'].startswith("project3"):
            p['createWithUserId'] = "daniel"
        if p['taskId'].startswith("project2") or p['taskId'].startswith("project4"):
            p['createWithUserId'] = "anne"
        tasks.append(p)
    json = json.dumps(tasks, indent=4)
    print(json)

f = open("import-daycard-new.json", "a")
f.write(json)
f.close()