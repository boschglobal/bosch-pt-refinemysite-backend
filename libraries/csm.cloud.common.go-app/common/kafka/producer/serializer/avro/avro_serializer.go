package avro

import (
	"dev.azure.com/pt-iot/smartsite/csm.cloud.common.go-app.git/common/kafka/producer/serializer"
	"encoding/binary"
	"encoding/json"
	"github.com/riferrei/srclient"
	"github.com/rs/zerolog/log"
)

type avroSerializer struct {
	schema *srclient.Schema
}

func NewAvroSerializer(schema *srclient.Schema) serializer.Serializer {
	return &avroSerializer{
		schema: schema,
	}
}

/*
Serialize attempts to serialize an avro representation using the schema for the avro object supplied.
Returns the avro as byte array including schema ID reference.
Implements serializer.Serializer interface.
*/
func (this *avroSerializer) Serialize(avro any) ([]byte, error) {

	// Convert the event to json
	value, err := json.Marshal(avro)
	if err != nil {
		log.Warn().Msg("Failed to marshal JSON for Avro event: " + err.Error())
		return nil, err
	}

	// Convert json to internal avro data structure
	native, _, err := this.schema.Codec().NativeFromTextual(value)
	if err != nil {
		log.Warn().Msg("Failed to encode NativeFromTextual for Avro event: " + err.Error())
		return nil, err
	}

	// Encode internal avro data structure to binary
	valueBytes, err := this.schema.Codec().BinaryFromNative(nil, native)
	if err != nil {
		log.Warn().Msg("Failed to encode BinaryFromNative for Avro event: " + err.Error())
		return nil, err
	}

	// Convert schema ID to byte slice
	schemaIDBytes := make([]byte, 4)
	binary.BigEndian.PutUint32(schemaIDBytes, uint32(this.schema.ID()))

	// Construct avro record in confluent wire format
	// https://docs.confluent.io/platform/current/schema-registry/serdes-develop/index.html#wire-format
	var recordValue []byte
	recordValue = append(recordValue, byte(0))          // magic byte: Confluent serialization format version; always 0.
	recordValue = append(recordValue, schemaIDBytes...) // 4-byte schema ID as returned by Schema Registry.
	recordValue = append(recordValue, valueBytes...)    // Serialized data for the specified schema format

	return recordValue, nil
}
