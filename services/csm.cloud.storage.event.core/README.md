# csm.cloud.storage.event.core

The ***Storage Event Service*** is a go application that listens to an Azure Storage Queue and publishes *File Created
Events* in Kafka for *Malware Scanned Events* received in *Azure Storage Queue* messages containing Malware Scanned
events of type `Microsoft.Security.MalwareScanningResult` as described in [Defender for Storage - Setting up response to
Malware
Scanning](https://learn.microsoft.com/en-us/azure/defender-for-cloud/defender-for-storage-configure-malware-scan#event-message-structure).

As a precondition to propagating a *File Created Event* to Kafka the service processes the *Malware Scanned Events*
received in *Azure Storage Queue*. In this processing the service checks for the right event type
`Microsoft.Security.MalwareScanningResult` and scan result type `No threats found`, `Malicious` or something unexpected
and ensures the **file size limit of 500MB** configured is not exceeded by the blob content.

The *Storage Event Service* does not download the blob content itself. It merely handles event information and metadata.

## working with go applications

- Check the [Go Installation Guide](https://bosch-pt.atlassian.net/wiki/x/LICNlgI) in confluence to figure out how to
  install Go.

- Check the [Golang Cheat Sheet](https://bosch-pt.atlassian.net/wiki/x/-4CMmgI) in confluence to learn how to work with
  Go.

- This application uses private dependencies (go modules located in private Azure DevOps repositories). Check [Golang
  Azure DevOps Dependencies](https://bosch-pt.atlassian.net/wiki/x/44CSmgI) in confluence to set up support for using
  private dependencies on your system.

A quick introduction to go can be found [here](https://go.dev/tour/list).

## test

run `go test ./...`

## build

run `go build`

## build docker

run `scripts/build-docker.sh`
