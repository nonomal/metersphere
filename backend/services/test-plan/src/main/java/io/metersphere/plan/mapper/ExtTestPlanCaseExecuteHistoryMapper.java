package io.metersphere.plan.mapper;

import io.metersphere.plan.dto.request.TestPlanCaseExecHistoryRequest;
import io.metersphere.plan.dto.response.TestPlanCaseExecHistoryResponse;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ExtTestPlanCaseExecuteHistoryMapper {

    void updateDeleted(@Param("testPlanCaseIds") List<String> testPlanCaseIds, @Param("deleted") boolean deleted);

    List<TestPlanCaseExecHistoryResponse> getCaseExecHistory(@Param("request") TestPlanCaseExecHistoryRequest request);
}
