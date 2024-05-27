package domain

import (
	"encoding/base64"
	"encoding/json"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azqueue"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestToMalwareScannedEvent_FailsWithBase64Error(t *testing.T) {

	// Create message
	text := "NOT a base64 string - obviously"
	id := "id"
	message := NewAzureStorageMessage(azqueue.DequeuedMessage{
		MessageText: &text,
		MessageID:   &id,
	})

	// Adapt to malware scanned event
	malwareScannedEvent, err := message.ToMalwareScannedEvent()

	// Verify an error about the invalid base64 text is issued
	assert.IsType(t, base64.CorruptInputError(0), err)
	assert.Equal(t, "illegal base64 data at input byte 3", err.Error())
	assert.Nil(t, malwareScannedEvent)
}

func TestToMalwareScannedEvent_FailsWithJsonUnmarshallingError(t *testing.T) {

	// Create message
	text := "Tm90IGEgSlNPTiAtIG9idmlvdXNseQ=="
	id := "id"
	message := NewAzureStorageMessage(azqueue.DequeuedMessage{
		MessageText: &text,
		MessageID:   &id,
	})

	// adapt to malware scanned event
	malwareScannedEvent, err := message.ToMalwareScannedEvent()

	// Verify an error about faulty JSON syntax is issued
	assert.IsType(t, &json.SyntaxError{}, err)
	assert.Equal(t, "invalid character 'N' looking for beginning of value", err.Error())
	assert.Nil(t, malwareScannedEvent)
}
