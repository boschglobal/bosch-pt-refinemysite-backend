# csm.cloud.image.scale

The image scale service can be used to generate preview images for files uploaded to the azure blob storage.
The service handles `FileCreatedEvent` kafka events (sent by the csm.cloud.storage.event.core service).
Images are downloaded from the quarantine blob storage, scaled down using **libvips** and uploaded
to the target blob storages (currently user or project). The original image files are deleted in the quarantine
blob storage after scaling them down and copying them over to the target blob storage.

The service currently has two resize configurations:

- Small (crop to 250x250px, section is chosen by libvips)
- Preview (maximum 1920px on the longer side, keeping the aspect ratio)

The resize configurations are applied to the different file types as follows, the original file is kept as it is:

| Type               | Small | Preview | Original |
|--------------------|-------|---------|----------|
| Profile Picture    | yes   | no      | yes      |
| Project Picture    | yes   | yes     | yes      |
| Task Attachment    | yes   | yes     | yes      |
| Topic Attachment   | yes   | yes     | yes      |
| Message Attachment | yes   | yes     | yes      |

## install dependencies

The service requires libvips, librdkafka and pkg-config to be installed on the system to run.
On macOS, it can be installed using brew [[1](https://formulae.brew.sh/formula/vips),
[2](https://formulae.brew.sh/formula/librdkafka),
[3](https://formulae.brew.sh/formula/pkg-config)].
There are also pre-compiled packages for Ubuntu [[1](https://github.com/libvips/libvips/wiki/Build-for-Ubuntu),
[2](https://packages.ubuntu.com/search?keywords=librdkafka-dev)]
and other distributions.

## working with go applications

- Check the [Go Installation Guide](https://bosch-pt.atlassian.net/wiki/x/LICNlgI)
  in confluence to figure out how to install Go.

- Check the [Golang Cheat Sheet](https://bosch-pt.atlassian.net/wiki/x/-4CMmgI)
  in confluence to learn how to work with Go.

- This application uses private dependencies (go modules located in private Azure DevOps repositories).
  Check [Golang Azure DevOps Dependencies](https://bosch-pt.atlassian.net/wiki/x/44CSmgI)
  in confluence to set up support for using private dependencies on your system.

A quick introduction to go can be found [here](https://go.dev/tour/list).

## test

run `go test ./...`

## build

run `go build`

## build docker

run `scripts/build-docker.sh`
