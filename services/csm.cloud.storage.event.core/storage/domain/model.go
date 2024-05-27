package domain

import (
	"fmt"
	"regexp"
)

type MalwareScannedEvent struct {
	Id              string            `json:"id"`
	Subject         string            `json:"subject"`
	Data            MalwareScanResult `json:"data"`
	EventType       string            `json:"eventType"`
	DataVersion     string            `json:"dataVersion"`
	MetadataVersion string            `json:"metadataVersion"`
	EventTime       string            `json:"eventTime"`
	Topic           string            `json:"topic"`
}

type MalwareScanResult struct {
	CorrelationId       string            `json:"correlationId"`
	BlobUri             string            `json:"blobUri"`
	ETag                string            `json:"eTag"`
	ScanFinishedTimeUtc string            `json:"scanFinishedTimeUtc"`
	ScanResultType      string            `json:"scanResultType"`
	ScanResultDetails   ScanResultDetails `json:"scanResultDetails"`
}

type ScanResultDetails struct {
	MalwareNamesFound []string `json:"malwareNamesFound"`
	Sha256            string   `json:"sha256"`
}

type BlobInfo struct {
	StorageName   string
	ContainerName string
	Path          string
	FileName      string
}

func (this *BlobInfo) ToString() string {
	return fmt.Sprintf("%s/%s in storage account %s and container %s", this.Path, this.FileName, this.StorageName, this.ContainerName)
}

/*
ToBlobInfo parses container name, file name and path of the blob from the event's subject
*/
func (this *MalwareScannedEvent) ToBlobInfo() (*BlobInfo, error) {

	// Parse path and fileName out of subject: storageAccounts/<storageName>/containers/<containerName>/blobs/<path>/<fileName>
	pattern := regexp.MustCompile("storageAccounts/(?P<storageName>.*)/containers/(?P<container>.*)/blobs/(?P<path>.*)/(?P<fileName>.*)")
	matches := pattern.FindAllStringSubmatch(this.Subject, -1)

	// matches is a 2-dimensional array.
	// The first index refers to the amount of matches. In this case we always have 1 match (index 0).
	// The second index refers to the completely (or fully) matched string (index 0) and the defined groups,
	// 'storage' (index 1), 'container' (index 2) 'path' (index 3) and 'fileName' (index 4).
	if matches == nil {
		return nil, &SubjectMatchError{this.Subject}
	}

	return &BlobInfo{
		StorageName:   matches[0][1],
		ContainerName: matches[0][2],
		Path:          matches[0][3],
		FileName:      matches[0][4],
	}, nil
}
