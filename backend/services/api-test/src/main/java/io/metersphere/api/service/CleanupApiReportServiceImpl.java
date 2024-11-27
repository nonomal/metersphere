package io.metersphere.api.service;

import io.metersphere.api.domain.*;
import io.metersphere.api.mapper.*;
import io.metersphere.sdk.constants.ProjectApplicationType;
import io.metersphere.sdk.util.LogUtils;
import io.metersphere.system.service.BaseCleanUpReport;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static io.metersphere.sdk.util.ShareUtil.getCleanDate;

@Component
@Transactional(rollbackFor = Exception.class)
public class CleanupApiReportServiceImpl implements BaseCleanUpReport {

    @Resource
    private ApiReportMapper apiReportMapper;
    @Resource
    private ExtApiReportMapper extApiReportMapper;
    @Resource
    private ApiReportStepMapper apiReportStepMapper;
    @Resource
    private ApiReportDetailMapper apiReportDetailMapper;
    @Resource
    private ApiReportLogMapper apiReportLogMapper;
    @Resource
    private ApiScenarioReportMapper apiScenarioReportMapper;
    @Resource
    private ExtApiScenarioReportMapper extApiScenarioReportMapper;
    @Resource
    private ApiScenarioReportStepMapper apiScenarioReportStepMapper;
    @Resource
    private ApiScenarioReportDetailMapper apiScenarioReportDetailMapper;
    @Resource
    private ApiScenarioReportLogMapper apiScenarioReportLogMapper;
    @Resource
    private ApiScenarioReportDetailBlobMapper apiScenarioReportDetailBlobMapper;
    @Resource
    private ApiReportRelateTaskMapper apiReportRelateTaskMapper;

    @Override
    public void cleanReport(Map<String, String> map, String projectId) {
        LogUtils.info("清理当前项目[" + projectId + "]相关接口测试报告");
        String expr = map.get(ProjectApplicationType.API.API_CLEAN_REPORT.name());
        long timeMills = getCleanDate(expr);
        int apiReportCount = extApiReportMapper.countApiReportByTime(timeMills, projectId);
        while (apiReportCount > 0) {
            List<String> ids = extApiReportMapper.selectApiReportByProjectIdAndTime(timeMills, projectId);
            ApiReportExample reportExample = new ApiReportExample();
            reportExample.createCriteria().andIdIn(ids);
            ApiReport report = new ApiReport();
            report.setDeleted(true);
            apiReportMapper.updateByExampleSelective(report, reportExample);
            // 任务执行结果存在报告，明细做保留
            List<String> taskReportIds = getTaskReportIds(ids);
            ids.removeAll(taskReportIds);
            if (CollectionUtils.isNotEmpty(ids)) {
                deleteApiReport(ids);
            }
            apiReportCount = extApiReportMapper.countApiReportByTime(timeMills, projectId);
        }
        int scenarioReportCount = extApiScenarioReportMapper.countScenarioReportByTime(timeMills, projectId);
        while (scenarioReportCount > 0) {
            List<String> ids = extApiScenarioReportMapper.selectApiReportByProjectIdAndTime(timeMills, projectId);
            ApiScenarioReportExample reportExample = new ApiScenarioReportExample();
            reportExample.createCriteria().andIdIn(ids);
            ApiScenarioReport report = new ApiScenarioReport();
            report.setDeleted(true);
            apiScenarioReportMapper.updateByExampleSelective(report, reportExample);
            // 任务执行结果存在报告，明细做保留
            List<String> taskReportIds = getTaskReportIds(ids);
            ids.removeAll(taskReportIds);
            if (CollectionUtils.isNotEmpty(ids)) {
                deleteScenarioReport(ids);
            }
            scenarioReportCount = extApiScenarioReportMapper.countScenarioReportByTime(timeMills, projectId);
        }
    }

    /**
     * 获取任务报告ID
     *
     * @param reportIds 报告ID集合
     * @return 任务报告ID集合
     */
    private List<String> getTaskReportIds(List<String> reportIds) {
        ApiReportRelateTaskExample example = new ApiReportRelateTaskExample();
        example.createCriteria().andReportIdIn(reportIds);
        List<ApiReportRelateTask> relateTasks = apiReportRelateTaskMapper.selectByExample(example);
        return relateTasks.stream().map(ApiReportRelateTask::getReportId).toList();
    }

    private void deleteApiReport(List<String> ids) {
        ApiReportStepExample stepExample = new ApiReportStepExample();
        stepExample.createCriteria().andReportIdIn(ids);
        apiReportStepMapper.deleteByExample(stepExample);
        ApiReportDetailExample detailExample = new ApiReportDetailExample();
        detailExample.createCriteria().andReportIdIn(ids);
        apiReportDetailMapper.deleteByExample(detailExample);
        ApiReportLogExample logExample = new ApiReportLogExample();
        logExample.createCriteria().andReportIdIn(ids);
        apiReportLogMapper.deleteByExample(logExample);
    }

    private void deleteScenarioReport(List<String> ids) {
        ApiScenarioReportStepExample stepExample = new ApiScenarioReportStepExample();
        stepExample.createCriteria().andReportIdIn(ids);
        apiScenarioReportStepMapper.deleteByExample(stepExample);
        ApiScenarioReportDetailExample detailExample = new ApiScenarioReportDetailExample();
        detailExample.createCriteria().andReportIdIn(ids);
        apiScenarioReportDetailMapper.deleteByExample(detailExample);
        ApiScenarioReportDetailBlobExample blobExample = new ApiScenarioReportDetailBlobExample();
        blobExample.createCriteria().andReportIdIn(ids);
        apiScenarioReportDetailBlobMapper.deleteByExample(blobExample);
        ApiScenarioReportLogExample logExample = new ApiScenarioReportLogExample();
        logExample.createCriteria().andReportIdIn(ids);
        apiScenarioReportLogMapper.deleteByExample(logExample);
    }

}
