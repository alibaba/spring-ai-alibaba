package chatmodel

import (
	"github.com/alibaba/spring-ai-alibaba/pkg/api"
	"github.com/go-resty/resty/v2"
)

type ChatModel struct {
	Name      string `json:"name"`
	Model     string `json:"model"`
	ModelType string `json:"modelType"`
}

type ListChatModelsReq struct{}

type ListChatModelsRspItem struct {
	Name      string `json:"name"`
	Model     string `json:"model"`
	ModelType string `json:"modelType"`
}

type ListChatModelsRsp []*ListChatModelsRspItem

type ChatModelAPI interface {
	ListChatModels(req *ListChatModelsReq) (ListChatModelsRsp, error)
}

type ChatModelAPIImpl struct {
	restClient *resty.Client
}

func NewChatModelAPI(baseURL string) ChatModelAPI {
	rc := resty.New()
	rc.SetBaseURL(baseURL + api.CommonPrefix)
	return &ChatModelAPIImpl{restClient: rc}
}

func (c *ChatModelAPIImpl) ListChatModels(req *ListChatModelsReq) (ListChatModelsRsp, error) {
	const path = "/chat-models"
	r := c.restClient.R()
	rsp := &api.Resp[ListChatModelsRsp]{}
	if _, err := r.SetResult(rsp).Get(path); err != nil {
		return nil, err
	}
	if err := api.ValidateResp(rsp); err != nil {
		return nil, err
	}
	return rsp.Data, nil
}
