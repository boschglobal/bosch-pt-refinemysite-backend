package domain

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestMalwareScannedEvent_ToBlobInfo(t *testing.T) {

	cut := MalwareScannedEvent{
		Subject: "storageAccounts/defendermalwaretest/containers/uploads/blobs/some/Screenshot 1 9.png",
	}

	blobInfo, err := cut.ToBlobInfo()

	// Verify the blob info has been parsed correctly
	assert.Nil(t, err)
	assert.Equal(t, "defendermalwaretest", blobInfo.StorageName)
	assert.Equal(t, "uploads", blobInfo.ContainerName)
	assert.Equal(t, "some", blobInfo.Path)
	assert.Equal(t, "Screenshot 1 9.png", blobInfo.FileName)

	assert.Equal(t, "some/Screenshot 1 9.png in storage account defendermalwaretest and container uploads", blobInfo.ToString())
}

func TestMalwareScannedEvent_ToBlobInfo_SubjectWithDeepPath(t *testing.T) {

	cut := MalwareScannedEvent{
		Subject: "storageAccounts/defendermalwaretest/containers/uploads/blobs/some/one/two/three/Screenshot 1 9.png",
	}

	blobInfo, err := cut.ToBlobInfo()

	// Verify the blob info has been parsed correctly
	assert.Nil(t, err)
	assert.Equal(t, "defendermalwaretest", blobInfo.StorageName)
	assert.Equal(t, "uploads", blobInfo.ContainerName)
	assert.Equal(t, "some/one/two/three", blobInfo.Path)
	assert.Equal(t, "Screenshot 1 9.png", blobInfo.FileName)

	assert.Equal(t, "some/one/two/three/Screenshot 1 9.png in storage account defendermalwaretest and container uploads", blobInfo.ToString())
}

func TestMalwareScannedEvent_ToBlobInfo_NoSubjectMatched(t *testing.T) {

	cut := MalwareScannedEvent{
		Subject: "storageAccounts/defendermalwaretest/containers/uploads/blobs/Screenshot 1 9.png",
	}

	blobInfo, err := cut.ToBlobInfo()

	// Verify an error is returned
	assert.Nil(t, blobInfo)
	assert.IsType(t, &SubjectMatchError{}, err)
	assert.Equal(t, "No match in subject of malwareScannedEvent storageAccounts/defendermalwaretest/containers/uploads/blobs/Screenshot 1 9.png", err.Error())
}
