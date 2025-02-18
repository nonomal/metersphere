package io.metersphere.plan.service;

import io.metersphere.bug.domain.BugRelationCase;
import io.metersphere.bug.domain.BugRelationCaseExample;
import io.metersphere.bug.mapper.BugRelationCaseMapper;
import io.metersphere.bug.service.BugCommonService;
import io.metersphere.plan.dto.TestPlanBugCaseDTO;
import io.metersphere.plan.dto.request.TestPlanBugPageRequest;
import io.metersphere.plan.dto.response.TestPlanBugPageResponse;
import io.metersphere.plan.mapper.ExtTestPlanBugMapper;
import io.metersphere.plugin.platform.dto.SelectOption;
import io.metersphere.system.dto.sdk.OptionDTO;
import io.metersphere.system.mapper.BaseUserMapper;
import jakarta.annotation.Resource;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = Exception.class)
public class TestPlanBugService extends TestPlanResourceService {

    @Resource
    private BaseUserMapper baseUserMapper;
    @Resource
    private ExtTestPlanBugMapper extTestPlanBugMapper;
    @Resource
    private BugRelationCaseMapper bugRelationCaseMapper;
    @Resource
    private BugCommonService bugCommonService;

    public List<TestPlanBugPageResponse> page(TestPlanBugPageRequest request) {
        List<TestPlanBugPageResponse> bugList = extTestPlanBugMapper.list(request);
        if (CollectionUtils.isEmpty(bugList)) {
            return new ArrayList<>();
        }
        parseCustomField(bugList, request.getProjectId());
		return buildBugRelatedListExtraInfo(bugList, request.getPlanId());
    }

    @Override
    public int deleteBatchByTestPlanId(List<String> testPlanIdList) {
        BugRelationCaseExample example = new BugRelationCaseExample();
        example.createCriteria().andTestPlanIdIn(testPlanIdList);
        List<BugRelationCase> bugRelationCases = bugRelationCaseMapper.selectByExample(example);
        List<String> relateIdsByDirect = bugRelationCases.stream().filter(relatedCase -> StringUtils.isNotEmpty(relatedCase.getCaseId())).map(BugRelationCase::getId).toList();
        List<String> relateIdsByPlan = bugRelationCases.stream().filter(relatedCase -> StringUtils.isEmpty(relatedCase.getCaseId())).map(BugRelationCase::getId).toList();
        if (CollectionUtils.isNotEmpty(relateIdsByDirect)) {
            // 缺陷-用例, 存在直接关联
            BugRelationCaseExample updateExample = new BugRelationCaseExample();
            updateExample.createCriteria().andIdIn(relateIdsByDirect);
            BugRelationCase record = new BugRelationCase();
            record.setTestPlanId(StringUtils.EMPTY);
            record.setTestPlanCaseId(StringUtils.EMPTY);
            bugRelationCaseMapper.updateByExampleSelective(record, updateExample);
        }
        if (CollectionUtils.isNotEmpty(relateIdsByPlan)) {
            // 缺陷-用例, 计划关联
            BugRelationCaseExample deleteExample = new BugRelationCaseExample();
            deleteExample.createCriteria().andIdIn(relateIdsByPlan);
            bugRelationCaseMapper.deleteByExample(deleteExample);
        }
        return bugRelationCases.size();
    }


    @Override
    public long getNextOrder(String projectId) {
        return 0;
    }

    @Override
    public void updatePos(String id, long pos) {

    }

    @Override
    public void refreshPos(String testPlanId) {

    }

    /**
     * 处理自定义字段
     *
     * @param bugList 缺陷集合
     */
    private void parseCustomField(List<TestPlanBugPageResponse> bugList, String projectId) {
        // MS处理人会与第三方的值冲突, 分开查询
        List<SelectOption> headerOptions = bugCommonService.getHeaderHandlerOption(projectId);
        Map<String, String> headerHandleUserMap = headerOptions.stream().collect(Collectors.toMap(SelectOption::getValue, SelectOption::getText));
        List<SelectOption> localOptions = bugCommonService.getLocalHandlerOption(projectId);
        Map<String, String> localHandleUserMap = localOptions.stream().collect(Collectors.toMap(SelectOption::getValue, SelectOption::getText));

        Map<String, String> allStatusMap = bugCommonService.getAllStatusMap(projectId);
        bugList.forEach(bug -> {
            // 解析处理人, 状态
            bug.setHandleUser(headerHandleUserMap.containsKey(bug.getHandleUser()) ?
                    headerHandleUserMap.get(bug.getHandleUser()) : localHandleUserMap.get(bug.getHandleUser()));
            bug.setStatus(allStatusMap.get(bug.getStatus()));
        });
    }

    /**
     * 补充计划-缺陷列表额外信息
     * @param bugList 缺陷列表
     * @return 缺陷列表全部信息
     */
    private List<TestPlanBugPageResponse> buildBugRelatedListExtraInfo(List<TestPlanBugPageResponse> bugList, String planId) {
        // 获取用户集合
        List<String> userIds = new ArrayList<>(bugList.stream().map(TestPlanBugPageResponse::getCreateUser).distinct().toList());
        List<OptionDTO> userOptions = baseUserMapper.selectUserOptionByIds(userIds);
        Map<String, String> userMap = userOptions.stream().collect(Collectors.toMap(OptionDTO::getId, OptionDTO::getName));
        List<String> bugIds = bugList.stream().map(TestPlanBugPageResponse::getId).toList();
        List<TestPlanBugCaseDTO> bugRelatedCases = extTestPlanBugMapper.getBugRelatedCase(bugIds, planId);
        Map<String, List<TestPlanBugCaseDTO>> bugRelateCaseMap = bugRelatedCases.stream().collect(Collectors.groupingBy(TestPlanBugCaseDTO::getBugId));
        bugList.forEach(bug -> {
            bug.setRelateCase(bugRelateCaseMap.get(bug.getId()));
            bug.setCreateUser(userMap.get(bug.getCreateUser()));
        });
        return bugList;
    }
}
