package avro

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/test"
	"errors"
	"github.com/riferrei/srclient"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestAvroSerializer(t *testing.T) {

	// prepare
	valueSchema := createTestSchema()
	cut := NewAvroSerializer(valueSchema)

	// execute
	value, err := cut.Serialize(&FileCreatedEvent{
		Identifier: "hui",
	})

	// verify
	assert.Nil(t, err)
	expectedVal := []uint8{0, 0, 0, 0, 4, 6, 104, 117, 105, 0, 0, 0, 0}
	assert.Equal(t, expectedVal, value)
}

func TestAvroSerializer_FailingDueToValue(t *testing.T) {

	// prepare
	valueSchema := createTestSchema()
	cut := NewAvroSerializer(valueSchema)

	// execute
	value, err := cut.Serialize(&NonMatchingEvent{
		Wrong: "soWrong",
	})

	// verify
	assert.IsType(t, errors.New(""), err)
	assert.Nil(t, value)
}

func createTestSchema() *srclient.Schema {

	valueSchema := &srclient.Schema{}
	test.SetFieldValueForTesting(valueSchema, "schema", "{\"type\":\"record\",\"name\":\"FileCreatedEventAvro\","+
		"\"namespace\":\"com.bosch.pt.csm.cloud.storage.event.messages\",\"fields\":[{\"name\":\"identifier\","+
		"\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"},\"doc\":\"Identifier of the storage event\"},"+
		"{\"name\":\"path\",\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"filename\","+
		"\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"contentType\","+
		"\"type\":{\"type\":\"string\",\"avro.java.string\":\"String\"}},{\"name\":\"contentLength\","+
		"\"type\":\"long\"}]}")
	test.SetFieldValueForTesting(valueSchema, "id", 4)
	test.SetFieldValueForTesting(valueSchema, "version", 1)

	return valueSchema
}

type FileCreatedEvent struct {
	Identifier    string `json:"identifier"`
	Path          string `json:"path"`
	FileName      string `json:"filename"`
	ContentType   string `json:"contentType"`
	ContentLength int64  `json:"contentLength"`
}

type NonMatchingEvent struct {
	Wrong string `json:"justWrong"`
}
