package domain

type FileCreatedEvent struct {
	Identifier    string `json:"identifier"`
	Path          string `json:"path"`
	FileName      string `json:"filename"`
	ContentType   string `json:"contentType"`
	ContentLength int64  `json:"contentLength"`
}
