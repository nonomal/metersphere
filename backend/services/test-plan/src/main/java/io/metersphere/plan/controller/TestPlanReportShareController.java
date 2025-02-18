package io.metersphere.plan.controller;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.metersphere.bug.dto.response.BugDTO;
import io.metersphere.plan.dto.ReportDetailCasePageDTO;
import io.metersphere.plan.dto.TestPlanShareInfo;
import io.metersphere.plan.dto.request.TestPlanReportShareRequest;
import io.metersphere.plan.dto.response.TestPlanReportDetailResponse;
import io.metersphere.plan.dto.response.TestPlanShareResponse;
import io.metersphere.plan.service.TestPlanReportService;
import io.metersphere.plan.service.TestPlanReportShareService;
import io.metersphere.sdk.constants.PermissionConstants;
import io.metersphere.sdk.domain.ShareInfo;
import io.metersphere.system.dto.sdk.BasePageRequest;
import io.metersphere.system.security.CheckOwner;
import io.metersphere.system.utils.PageUtils;
import io.metersphere.system.utils.Pager;
import io.metersphere.system.utils.SessionUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/test-plan/report/share")
@Tag(name = "测试计划-分享")
public class TestPlanReportShareController {

	@Resource
	private TestPlanReportService testPlanReportService;
	@Resource
	private TestPlanReportShareService testPlanReportShareService;

	@PostMapping("/gen")
	@Operation(summary = "测试计划-报告-分享")
	@RequiresPermissions(PermissionConstants.TEST_PLAN_REPORT_READ_SHARE)
	@CheckOwner(resourceId = "#request.getProjectId()", resourceType = "project")
	public TestPlanShareInfo genReportShareInfo(@RequestBody TestPlanReportShareRequest request) {
		return testPlanReportShareService.gen(request, SessionUtils.getUserId());
	}

	@GetMapping("/get/{id}")
	@Operation(summary = "测试计划-报告-获取分享链接")
	public TestPlanShareResponse get(@PathVariable String id) {
		return testPlanReportShareService.get(id);
	}

	@GetMapping("/get-share-time/{id}")
	@Operation(summary = "测试计划-报告-获取分享链接的有效时间")
	public String getShareTime(@PathVariable String id) {
		return testPlanReportShareService.getShareTime(id);
	}

	// 分享报告详情开始

	@GetMapping("/get/{shareId}/{reportId}")
	@Operation(summary = "测试计划-报告分享-详情查看")
	@RequiresPermissions(PermissionConstants.TEST_PLAN_REPORT_READ)
	@CheckOwner(resourceId = "#id", resourceType = "test_plan_report")
	public TestPlanReportDetailResponse getDetail(@PathVariable String shareId, @PathVariable String reportId) {
		ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
		testPlanReportShareService.validateExpired(shareInfo);
		return testPlanReportService.getReport(reportId);
	}

	@PostMapping("/detail/bug/page/{shareId}/{reportId}")
	@Operation(summary = "测试计划-报告-详情-缺陷分页查询")
	@RequiresPermissions(PermissionConstants.TEST_PLAN_REPORT_READ)
	@CheckOwner(resourceId = "#reportId", resourceType = "test_plan_report")
	public Pager<List<BugDTO>> pageBug(@PathVariable String reportId, @PathVariable String shareId,
									   @Validated @RequestBody BasePageRequest request) {
		ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
		testPlanReportShareService.validateExpired(shareInfo);
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
				StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "tprb.bug_num, tprb.id desc");
		return PageUtils.setPageInfo(page, testPlanReportService.listReportDetailBugs(request, reportId));
	}

	@PostMapping("/detail/functional/case/page/{shareId}/{reportId}")
	@Operation(summary = "测试计划-报告-详情-功能用例分页查询")
	@RequiresPermissions(PermissionConstants.TEST_PLAN_REPORT_READ)
	@CheckOwner(resourceId = "#reportId", resourceType = "test_plan_report")
	public Pager<List<ReportDetailCasePageDTO>> pageFunctionalCase(@PathVariable String reportId, @PathVariable String shareId,
																   @Validated @RequestBody BasePageRequest request) {
		ShareInfo shareInfo = testPlanReportShareService.checkResource(shareId);
		testPlanReportShareService.validateExpired(shareInfo);
		Page<Object> page = PageHelper.startPage(request.getCurrent(), request.getPageSize(),
				StringUtils.isNotBlank(request.getSortString()) ? request.getSortString() : "tprfc.function_case_num, tprfc.id desc");
		return PageUtils.setPageInfo(page, testPlanReportService.listReportDetailFunctionalCases(request, reportId));
	}
}
