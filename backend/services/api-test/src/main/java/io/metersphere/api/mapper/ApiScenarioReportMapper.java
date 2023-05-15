package io.metersphere.api.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import io.metersphere.api.domain.ApiScenarioReport;

/**
 * 场景报告数据库访问层
 *
 * @date : 2023-5-15
 */
@Mapper
public interface ApiScenarioReportMapper  extends BaseMapper<ApiScenarioReport> {
}