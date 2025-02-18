package io.metersphere.plan.service;

import io.metersphere.plan.domain.TestPlanReport;
import io.metersphere.plan.mapper.ExtTestPlanReportMapper;
import io.metersphere.plan.mapper.TestPlanReportMapper;
import io.metersphere.project.domain.Project;
import io.metersphere.project.mapper.ProjectMapper;
import io.metersphere.sdk.constants.HttpMethodConstants;
import io.metersphere.sdk.util.JSON;
import io.metersphere.system.log.aspect.OperationLogAspect;
import io.metersphere.system.log.constants.OperationLogModule;
import io.metersphere.system.log.constants.OperationLogType;
import io.metersphere.system.log.dto.LogDTO;
import io.metersphere.system.log.service.OperationLogService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TestPlanReportLogService {

    @Resource
    private ProjectMapper projectMapper;
    @Resource
    private OperationLogService operationLogService;
    @Resource
    private ExtTestPlanReportMapper extTestPlanReportMapper;
    @Resource
    private TestPlanReportMapper testPlanReportMapper;


    public LogDTO deleteLog(String id) {
        TestPlanReport report = testPlanReportMapper.selectByPrimaryKey(id);
        Project project = projectMapper.selectByPrimaryKey(report.getProjectId());
        LogDTO dto = new LogDTO(
                report.getProjectId(),
                project.getOrganizationId(),
                report.getId(),
                null,
                OperationLogType.DELETE.name(),
                OperationLogModule.TEST_PLAN_REPORT,
                report.getName());

        dto.setPath(OperationLogAspect.getPath());
        dto.setMethod(HttpMethodConstants.GET.name());
        dto.setOriginalValue(JSON.toJSONBytes(report));
        return dto;
    }

    public LogDTO updateLog(String id) {
        TestPlanReport report = testPlanReportMapper.selectByPrimaryKey(id);
        Project project = projectMapper.selectByPrimaryKey(report.getProjectId());
        LogDTO dto = new LogDTO(
                report.getProjectId(),
                project.getOrganizationId(),
                report.getId(),
                null,
                OperationLogType.UPDATE.name(),
                OperationLogModule.TEST_PLAN_REPORT,
                report.getName());

        dto.setPath(OperationLogAspect.getPath());
        dto.setMethod(HttpMethodConstants.GET.name());
        dto.setOriginalValue(JSON.toJSONBytes(report));
        return dto;
    }

    public void batchDeleteLog(List<String> ids, String userId, String projectId) {
        Project project = projectMapper.selectByPrimaryKey(projectId);
        List<TestPlanReport> reports = extTestPlanReportMapper.selectReportByIds(ids);
        List<LogDTO> logs = new ArrayList<>();
        reports.forEach(report -> {
            LogDTO dto = new LogDTO(
                    projectId,
                    project.getOrganizationId(),
                    report.getId(),
                    userId,
                    OperationLogType.DELETE.name(),
                    OperationLogModule.TEST_PLAN_REPORT,
                    report.getName());

            dto.setPath(OperationLogAspect.getPath());
            dto.setMethod(HttpMethodConstants.POST.name());
            dto.setOriginalValue(JSON.toJSONBytes(report));
            logs.add(dto);
        });
        operationLogService.batchAdd(logs);
    }
}
