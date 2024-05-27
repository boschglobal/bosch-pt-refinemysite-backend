package domain

import (
	"bytes"
	"encoding/base64"
	"encoding/json"
	"github.com/Azure/azure-sdk-for-go/sdk/storage/azqueue"
	"github.com/rs/zerolog/log"
)

type AzureStorageMessage struct {
	azqueue.DequeuedMessage
}

func NewAzureStorageMessage(message azqueue.DequeuedMessage) AzureStorageMessage {
	return AzureStorageMessage{message}
}

/*
ToMalwareScannedEvent adapts an Azure storage queue message to a malware scanning result event
*/
func (this *AzureStorageMessage) ToMalwareScannedEvent() (*MalwareScannedEvent, error) {

	// Decode base64 encoded json message
	textDec, err := base64.StdEncoding.DecodeString(*this.MessageText)
	if err != nil {
		log.Error().Msg("Decoding message failed: " + err.Error())
		return nil, err
	}

	// Log message details
	log.Debug().Msg("Processing message with id " + *this.MessageID)
	log.Trace().Msg("Message content: " + string(textDec))

	// Instantiate blob event and deserialize values from json string into the GO-datatype
	var malwareScannedEvent = &MalwareScannedEvent{}

	decoder := json.NewDecoder(bytes.NewReader(textDec))
	decoder.DisallowUnknownFields()
	err = decoder.Decode(&malwareScannedEvent)
	if err != nil {
		log.Error().Msg("Decode message content into MalwareScannedEvent failed: " + err.Error())
		return nil, err
	}
	return malwareScannedEvent, err
}
