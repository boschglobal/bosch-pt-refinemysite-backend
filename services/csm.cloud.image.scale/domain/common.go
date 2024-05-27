package domain

type StringMessageKey struct {
	Identifier string `json:"identifier"`
}

func NewStringMessageKey(identifier string) StringMessageKey {
	return StringMessageKey{
		Identifier: identifier,
	}
}

type MessageKey struct {
	RootContextIdentifier string              `json:"rootContextIdentifier"`
	AggregateIdentifier   AggregateIdentifier `json:"aggregateIdentifier"`
}

type AggregateIdentifier struct {
	Identifier string `json:"identifier"`
	Version    int64  `json:"version"`
	Type       string `json:"type"`
}
