package any

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestAnyPartitioner_Partition(t *testing.T) {
	cut := NewAnyPartitioner()
	partition := cut.Partition("nil", nil, nil, nil, nil)
	assert.Equal(t, int32(-1), partition)
}

func TestMurmur2Partitioner_PartitionShortString(t *testing.T) {
	partitionsPerTopic := make(map[string]int32)
	partitionsPerTopic["a"] = 3
	partitionsPerTopic["b"] = 2
	cut := NewMurmur2Partitioner(partitionsPerTopic)
	partition := cut.Partition("a", nil, []byte("shortString"), nil, nil)
	assert.Equal(t, int32(1821723083)%3, partition)

	partition = cut.Partition("b", nil, []byte("shortString"), nil, nil)
	assert.Equal(t, int32(1821723083)%2, partition)
}

func TestMurmur2Partitioner_PartitionLongString(t *testing.T) {
	longString := "this is a long string. this is a long string. this is a long string."

	partitionsPerTopic := make(map[string]int32)
	partitionsPerTopic["a"] = 2
	partitionsPerTopic["b"] = 5

	cut := NewMurmur2Partitioner(partitionsPerTopic)
	partition := cut.Partition("a", nil, []byte(longString), nil, nil)
	assert.Equal(t, int32(164789298)%2, partition)

	partition = cut.Partition("b", nil, []byte(longString), nil, nil)
	assert.Equal(t, int32(164789298)%5, partition)
}
