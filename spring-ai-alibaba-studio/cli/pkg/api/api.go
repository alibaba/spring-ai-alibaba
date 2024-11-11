package api

import "fmt"

const (
	DefaultBaseURL = "http://localhost:8080"
	CommonPrefix   = "/studio/api"
	successCode    = 200
)

type Resp[T any] struct {
	Code int    `json:"code"`
	Msg  string `json:"msg"`
	Data T      `json:"data"`
}

func ValidateResp[T any](resp *Resp[T]) error {
	if resp.Code != successCode {
		return fmt.Errorf("code: %d, msg: %s", resp.Code, resp.Msg)
	}
	return nil
}
