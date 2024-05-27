package domain

import (
	"github.com/stretchr/testify/assert"
	"testing"
)

func TestNewStringMessageKey(t *testing.T) {
	key := "myKey"
	stringMessageKey := NewStringMessageKey(key)
	assert.Equal(t, key, stringMessageKey.Identifier)
}
