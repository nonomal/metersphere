package io.metersphere.api.job;

import io.metersphere.api.domain.ApiScenarioReport;
import io.metersphere.api.dto.ApiScenarioParamConfig;
import io.metersphere.api.dto.ApiScenarioParseTmpParam;
import io.metersphere.api.dto.debug.ApiResourceRunRequest;
import io.metersphere.api.dto.request.MsScenario;
import io.metersphere.api.dto.scenario.ApiScenarioDetail;
import io.metersphere.api.dto.scenario.ApiScenarioParseParam;
import io.metersphere.api.service.ApiCommonService;
import io.metersphere.api.service.ApiExecuteService;
import io.metersphere.api.service.scenario.ApiScenarioRunService;
import io.metersphere.project.domain.Project;
import io.metersphere.project.mapper.ProjectMapper;
import io.metersphere.sdk.constants.*;
import io.metersphere.sdk.dto.api.task.ApiRunModeConfigDTO;
import io.metersphere.sdk.dto.api.task.TaskInfo;
import io.metersphere.sdk.dto.api.task.TaskItem;
import io.metersphere.sdk.dto.api.task.TaskRequestDTO;
import io.metersphere.sdk.util.CommonBeanFactory;
import io.metersphere.sdk.util.JSON;
import io.metersphere.sdk.util.LogUtils;
import io.metersphere.system.domain.ExecTask;
import io.metersphere.system.domain.ExecTaskItem;
import io.metersphere.system.schedule.BaseScheduleJob;
import io.metersphere.system.service.BaseTaskHubService;
import io.metersphere.system.uid.IDGenerator;
import org.apache.commons.lang3.StringUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.TriggerKey;

public class ApiScenarioScheduleJob extends BaseScheduleJob {
    @Override
    protected void businessExecute(JobExecutionContext context) {
        ApiExecuteService apiExecuteService = CommonBeanFactory.getBean(ApiExecuteService.class);
        ApiScenarioRunService apiScenarioRunService = CommonBeanFactory.getBean(ApiScenarioRunService.class);
        ApiCommonService apiCommonService = CommonBeanFactory.getBean(ApiCommonService.class);
        ProjectMapper projectMapper = CommonBeanFactory.getBean(ProjectMapper.class);
        BaseTaskHubService baseTaskHubService = CommonBeanFactory.getBean(BaseTaskHubService.class);
        ApiRunModeConfigDTO apiRunModeConfigDTO = JSON.parseObject(context.getJobDetail().getJobDataMap().get("config").toString(), ApiRunModeConfigDTO.class);

        ApiScenarioDetail apiScenarioDetail = apiScenarioRunService.getForRun(resourceId);
        if (apiScenarioDetail == null) {
            LogUtils.info("当前定时任务的场景已删除 {}", resourceId);
            return;
        }
        MsScenario msScenario = apiScenarioRunService.getMsScenario(apiScenarioDetail);
        ApiScenarioParseParam parseParam = apiScenarioRunService.getApiScenarioParseParam(apiScenarioDetail);
        parseParam.setEnvironmentId(apiRunModeConfigDTO.getEnvironmentId());
        parseParam.setGrouped(apiRunModeConfigDTO.getGrouped());

        if (StringUtils.isBlank(apiRunModeConfigDTO.getEnvironmentId())) {
            parseParam.setEnvironmentId(apiScenarioDetail.getEnvironmentId());
            parseParam.setGrouped(apiScenarioDetail.getGrouped());
        }

        if (StringUtils.isBlank(apiRunModeConfigDTO.getPoolId())) {
            apiRunModeConfigDTO.setPoolId(apiExecuteService.getProjectApiResourcePoolId(apiScenarioDetail.getProjectId()));
        }

        msScenario.setResourceId(apiScenarioDetail.getId());
        // 解析生成场景树，并保存临时变量
        ApiScenarioParseTmpParam tmpParam = apiScenarioRunService.parse(msScenario, apiScenarioDetail.getSteps(), parseParam);

        ApiResourceRunRequest runRequest = apiScenarioRunService.getApiResourceRunRequest(msScenario, tmpParam);

        Project project = projectMapper.selectByPrimaryKey(apiScenarioDetail.getProjectId());
        ExecTask execTask = apiCommonService.newExecTask(project.getId(), userId);
        execTask.setCaseCount(1L);
        execTask.setTaskName(apiScenarioDetail.getName());
        execTask.setOrganizationId(project.getOrganizationId());
        execTask.setTriggerMode(TaskTriggerMode.SCHEDULE.name());
        execTask.setTaskType(ExecTaskType.API_SCENARIO.name());

        ExecTaskItem execTaskItem = apiCommonService.newExecTaskItem(execTask.getId(), project.getId(), userId);
        execTaskItem.setOrganizationId(project.getOrganizationId());
        execTaskItem.setResourceType(ApiExecuteResourceType.API_SCENARIO.name());
        execTaskItem.setResourceId(apiScenarioDetail.getId());
        execTaskItem.setResourceName(apiScenarioDetail.getName());
        baseTaskHubService.insertExecTaskAndDetail(execTask, execTaskItem);

        TaskRequestDTO taskRequest = apiScenarioRunService.getTaskRequest(IDGenerator.nextStr(), apiScenarioDetail.getId(), apiScenarioDetail.getProjectId(), ApiExecuteRunMode.SCHEDULE.name());
        TaskInfo taskInfo = taskRequest.getTaskInfo();
        TaskItem taskItem = taskRequest.getTaskItem();
        taskInfo.getRunModeConfig().setPoolId(apiRunModeConfigDTO.getPoolId());
        taskInfo.setTaskId(execTask.getId());
        taskInfo.setSaveResult(true);
        taskInfo.setRealTime(false);
        taskInfo.setUserId(userId);
        taskInfo.getRunModeConfig().setEnvironmentId(parseParam.getEnvironmentId());
        taskItem.setRequestCount(tmpParam.getRequestCount().get());
        taskItem.setId(execTaskItem.getId());

        ApiScenarioParamConfig parseConfig = apiScenarioRunService.getApiScenarioParamConfig(msScenario.getProjectId(), parseParam, tmpParam.getScenarioParseEnvInfo());
        parseConfig.setTaskItemId(taskItem.getId());

        // 初始化报告
        ApiScenarioReport scenarioReport = apiScenarioRunService.getScenarioReport(userId);
        scenarioReport.setId(taskItem.getReportId());
        scenarioReport.setTriggerMode(TaskTriggerMode.SCHEDULE.name());
        scenarioReport.setRunMode(ApiBatchRunMode.PARALLEL.name());
        scenarioReport.setPoolId(apiRunModeConfigDTO.getPoolId());
        scenarioReport.setEnvironmentId(parseParam.getEnvironmentId());
        scenarioReport.setWaitingTime(apiScenarioRunService.getGlobalWaitTime(parseParam.getScenarioConfig()));

        apiScenarioRunService.initApiScenarioReport(taskItem.getId(), apiScenarioDetail, scenarioReport);

        // 初始化报告步骤
        apiScenarioRunService.initScenarioReportSteps(apiScenarioDetail.getSteps(), taskItem.getReportId());

        apiExecuteService.execute(runRequest, taskRequest, parseConfig);
    }


    public static JobKey getJobKey(String scenarioId) {
        return new JobKey(scenarioId, ApiScenarioScheduleJob.class.getName());
    }

    public static TriggerKey getTriggerKey(String scenarioId) {
        return new TriggerKey(scenarioId, ApiScenarioScheduleJob.class.getName());
    }
}
