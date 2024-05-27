package domain

type ImageDeletedEvent struct {
	Identifier    string `json:"identifier"`
	Path          string `json:"path"`
	FileName      string `json:"filename"`
	ContentType   string `json:"contentType"`
	ContentLength int64  `json:"contentLength"`
}

func (e ImageDeletedEvent) GetIdentifier() string {
	return e.Identifier
}

type ImageScaledEvent struct {
	Identifier    string `json:"identifier"`
	Path          string `json:"path"`
	FileName      string `json:"filename"`
	ContentType   string `json:"contentType"`
	ContentLength int64  `json:"contentLength"`
}

func (e ImageScaledEvent) GetIdentifier() string {
	return e.Identifier
}
