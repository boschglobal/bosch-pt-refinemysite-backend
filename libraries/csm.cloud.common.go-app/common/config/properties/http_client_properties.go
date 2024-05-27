package properties

type HttpClientProperties struct {
	RequestHostRewrites []RewriteProperties
}

type RewriteProperties struct {
	From string `validate:"required"`
	To   string `validate:"required"`
}
