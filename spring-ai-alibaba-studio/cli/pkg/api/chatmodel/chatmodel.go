package chatmodel

import (
	"github.com/alibaba/spring-ai-alibaba/pkg/api"
	"github.com/go-resty/resty/v2"
)

type ChatModelAPI interface {
	ListChatModels(req *ListChatModelsReq) (ListChatModelsRsp, error)
	GetChatModel(req *GetChatModelReq) (*GetChatModelRsp, error)
}

type ChatModelAPIImpl struct {
	restClient *resty.Client
}

func NewChatModelAPI(baseURL string) ChatModelAPI {
	rc := resty.New()
	rc.SetBaseURL(baseURL + api.CommonPrefix)
	return &ChatModelAPIImpl{restClient: rc}
}

type ChatModel struct {
	Name      string `json:"name"`
	Model     string `json:"model"`
	ModelType string `json:"modelType"`
}

type ListChatModelsReq struct{}

type ListChatModelsRsp []*ChatModel

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

type GetChatModelReq struct {
	ModelName string
}

type GetChatModelRsp ChatModel

func (c *ChatModelAPIImpl) GetChatModel(req *GetChatModelReq) (*GetChatModelRsp, error) {
	path := "/chat-models/" + req.ModelName
	r := c.restClient.R()
	rsp := &api.Resp[*GetChatModelRsp]{}
	if _, err := r.SetResult(rsp).SetHeader("Accept", "application/json").Get(path); err != nil {
		return nil, err
	}
	if err := api.ValidateResp(rsp); err != nil {
		return nil, err
	}
	return rsp.Data, nil
}
