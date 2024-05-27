package avro

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/serializer/avro"
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/test"
	"github.com/riferrei/srclient"
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestAvroTypeDeserializer_Deserialize(t *testing.T) {

	// prepare
	valueSchema := createTestSchema()
	cut := NewAvroTypeDeserializer[FileCreatedEvent](valueSchema)

	// execute
	binary := []uint8{0, 0, 0, 0, 4, 6, 104, 117, 105, 0, 0, 0, 0}
	value, err := cut.Deserialize(binary[5:])

	// verify
	assert.Nil(t, err)
	assert.IsType(t, FileCreatedEvent{}, value)
	assert.Equal(t, "hui", value.(FileCreatedEvent).Identifier)
}

func TestAvroTypeDeserializer_FailingInvalidData(t *testing.T) {

	// prepare
	valueSchema := createTestSchema()
	binary := []byte("invalid_data")

	cut := NewAvroTypeDeserializer[NonMatchingEvent](valueSchema)

	// execute
	value, err := cut.Deserialize(binary[5:])

	// verify
	assert.NotNil(t, err)
	assert.Regexp(t, "cannot decode binary record \"com.bosch.pt.csm.cloud.storage.event.messages.FileCreatedEventAvro\" field \".*\": cannot decode binary string: cannot decode binary bytes: negative size: -53", err.Error())
	assert.Nil(t, value)
}

func TestAvroTypeDeserializer_FailingUnexpectedAvro(t *testing.T) {

	// prepare
	valueSchema := createTestSchema()
	serializer := avro.NewAvroSerializer(valueSchema)
	binary, err := serializer.Serialize(&FileCreatedEvent{
		Identifier: "hui",
	})
	assert.Nil(t, err)

	cut := NewAvroTypeDeserializer[NonMatchingEvent](valueSchema)

	// execute
	value, err := cut.Deserialize(binary[5:])

	// verify
	assert.NotNil(t, err)
	assert.Regexp(t, "json: unknown field \".*\"", err.Error())
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
	Wrong string `json:"wrong"`
}
