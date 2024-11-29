package com.alibaba.cloud.ai.api;

import com.alibaba.cloud.ai.common.R;
import com.alibaba.cloud.ai.model.Workflow;
import com.alibaba.cloud.ai.service.WorkflowDelegate;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@Tag(name = "workflow", description = "the workflow API")
public interface WorkflowAPI {

    default WorkflowDelegate getDelegate(){
        return new WorkflowDelegate(){};
    }


    default R<List<Workflow>> list(){
        return R.success(getDelegate().list());
    }

    default R<Workflow> get(String id){
        return R.success(getDelegate().get(id));
    }

}
