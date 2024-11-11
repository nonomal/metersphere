package io.metersphere.api.service;

import io.metersphere.api.domain.ApiTestCase;
import io.metersphere.api.domain.ApiTestCaseRecord;
import io.metersphere.api.invoker.ApiExecuteCallbackServiceInvoker;
import io.metersphere.api.mapper.ApiTestCaseMapper;
import io.metersphere.api.service.definition.ApiTestCaseBatchRunService;
import io.metersphere.api.service.definition.ApiTestCaseRunService;
import io.metersphere.sdk.constants.ApiExecuteResourceType;
import io.metersphere.sdk.constants.ApiExecuteRunMode;
import io.metersphere.sdk.dto.api.task.GetRunScriptRequest;
import io.metersphere.sdk.dto.api.task.GetRunScriptResult;
import io.metersphere.sdk.dto.api.task.TaskItem;
import io.metersphere.sdk.dto.queue.ExecutionQueue;
import io.metersphere.sdk.dto.queue.ExecutionQueueDetail;
import io.metersphere.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @Author: jianxing
 * @CreateTime: 2024-02-06  20:47
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ApiCaseExecuteCallbackService implements ApiExecuteCallbackService {
    @Resource
    private ApiTestCaseRunService apiTestCaseRunService;
    @Resource
    private ApiTestCaseMapper apiTestCaseMapper;
    @Resource
    private ApiTestCaseBatchRunService apiTestCaseBatchRunService;


    public ApiCaseExecuteCallbackService() {
        ApiExecuteCallbackServiceInvoker.register(ApiExecuteResourceType.API_CASE, this);
    }

    /**
     * 解析并返回执行脚本
     */
    @Override
    public GetRunScriptResult getRunScript(GetRunScriptRequest request) {
        ApiTestCase apiTestCase = apiTestCaseMapper.selectByPrimaryKey(request.getTaskItem().getResourceId());
        String reportId = initReport(request, apiTestCase);
        GetRunScriptResult result = apiTestCaseRunService.getRunScript(request, apiTestCase);
        result.setReportId(reportId);
        return result;
    }

    @Override
    public String initReport(GetRunScriptRequest request) {
        ApiTestCase apiTestCase = apiTestCaseMapper.selectByPrimaryKey(request.getTaskItem().getResourceId());
        return initReport(request, apiTestCase);
    }

    private String initReport(GetRunScriptRequest request, ApiTestCase apiTestCase) {
        TaskItem taskItem = request.getTaskItem();
        String reportId = taskItem.getReportId();
        if (BooleanUtils.isTrue(request.getBatch()) && !request.getRunModeConfig().isIntegratedReport()) {
            // 批量执行，生成独立报告
            ApiTestCaseRecord apiTestCaseRecord = apiTestCaseBatchRunService.initApiReport(taskItem.getId(), request.getRunModeConfig(), apiTestCase, request.getUserId());
            reportId = apiTestCaseRecord.getApiReportId();
        } else if (BooleanUtils.isFalse(request.getBatch()) && !ApiExecuteRunMode.isDebug(request.getRunMode())) {
            // 单用例执行，非调试，生成报告
            if (StringUtils.isBlank(taskItem.getReportId())) {
                reportId = IDGenerator.nextStr();
            }
            apiTestCaseRunService.initApiReport(taskItem.getId(), apiTestCase, request);
        }
        return reportId;
    }

    /**
     * 串行时，执行下一个任务
     * @param queue
     * @param queueDetail
     */
    @Override
    public void executeNextTask(ExecutionQueue queue, ExecutionQueueDetail queueDetail) {
        apiTestCaseBatchRunService.executeNextTask(queue, queueDetail);
    }
}
