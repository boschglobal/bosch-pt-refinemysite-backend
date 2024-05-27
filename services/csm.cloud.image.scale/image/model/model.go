package model

type BoundedContext int32

const (
	PROJECT = 0
	USER    = 1
)

type Image interface {
	GetAggregateType() string

	GetBoundedContext() BoundedContext

	GetContentType() string

	GetFileName() string

	GetOwnerIdentifier() string

	GetOwnerType() string

	GetParentIdentifier() string

	GetRootContextIdentifier() string
}
