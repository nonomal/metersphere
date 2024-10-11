package io.metersphere.api.service.definition;

import io.metersphere.api.domain.ApiDocShare;
import io.metersphere.api.dto.definition.ApiDocShareDTO;
import io.metersphere.api.dto.definition.request.ApiDocShareEditRequest;
import io.metersphere.api.dto.definition.request.ApiDocSharePageRequest;
import io.metersphere.api.mapper.ApiDocShareMapper;
import io.metersphere.api.mapper.ExtApiDefinitionMapper;
import io.metersphere.api.mapper.ExtApiDocShareMapper;
import io.metersphere.sdk.constants.MsAssertionCondition;
import io.metersphere.sdk.exception.MSException;
import io.metersphere.sdk.util.BeanUtils;
import io.metersphere.sdk.util.Translator;
import io.metersphere.system.uid.IDGenerator;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(rollbackFor = Exception.class)
public class ApiDocShareService {

	@Resource
	private ExtApiDefinitionMapper extApiDefinitionMapper;
	@Resource
	private ApiDocShareMapper apiDocShareMapper;
	@Resource
	private ExtApiDocShareMapper extApiDocShareMapper;

	public static final String RANGE_ALL = "ALL";

	public List<ApiDocShareDTO> list(ApiDocSharePageRequest request) {
		List<ApiDocShareDTO> list = extApiDocShareMapper.list(request);
		return buildApiShareExtra(list);
	}

	public ApiDocShare create(ApiDocShareEditRequest request, String currentUser) {
		ApiDocShare docShare = new ApiDocShare();
		BeanUtils.copyBean(docShare, request);
		docShare.setId(IDGenerator.nextStr());
		docShare.setCreateUser(currentUser);
		docShare.setCreateTime(System.currentTimeMillis());
		apiDocShareMapper.insert(docShare);
		return docShare;
	}

	public ApiDocShare update(ApiDocShareEditRequest request) {
		checkExit(request.getId());
		ApiDocShare docShare = new ApiDocShare();
		BeanUtils.copyBean(docShare, request);
		apiDocShareMapper.updateByPrimaryKeySelective(docShare);
		return docShare;
	}

	public void delete(String id) {
		checkExit(id);
		apiDocShareMapper.deleteByPrimaryKey(id);
	}

	/**
	 * 构建分享额外信息
	 * @param docShares 分享列表
	 * @return 分享列表
	 */
	public List<ApiDocShareDTO> buildApiShareExtra(List<ApiDocShareDTO> docShares) {
		docShares.forEach(docShare -> {
			docShare.setDeadline(calculateDeadline(docShare.getInvalidTime(), docShare.getInvalidUnit(), docShare.getCreateTime()));
			docShare.setInvalid(docShare.getDeadline() != null && docShare.getDeadline() < System.currentTimeMillis());
			docShare.setApiShareNum(countApiShare(docShare));
		});
		return docShares;
	}

	/**
	 * 统计接口访范围分享接口数量
	 * @param docShare 接口分享
	 * @return 数量
	 */
	public Integer countApiShare(ApiDocShareDTO docShare) {
		StringBuilder condition = new StringBuilder();
		if (!StringUtils.equals(docShare.getApiRange(), RANGE_ALL) && !StringUtils.isBlank(docShare.getRangeMatchVal())) {
			switch (docShare.getApiRange()) {
				case "MODULE" -> condition.append("module_id = '").append(docShare.getRangeMatchVal()).append("'");
				case "PATH" -> {
					if (StringUtils.equals(docShare.getRangeMatchSymbol(), MsAssertionCondition.EQUALS.name())) {
						condition.append("path = '").append(docShare.getRangeMatchVal()).append("'");
					} else {
						condition.append("path like \"%").append(docShare.getRangeMatchVal()).append("%\"");
					}
				}
				case "TAG" -> {
					condition.append("(1=2 ");
					String[] tags = StringUtils.split(docShare.getRangeMatchVal(), ",");
					for (String tag : tags) {
						condition.append("OR JSON_CONTAINS(tags, JSON_ARRAY(\"").append(tag).append("\"))");
					}
					condition.append(")");
				}
				default -> {
				}
			}
		}
		return extApiDefinitionMapper.countByShareParam(docShare.getProjectId(), condition.toString()).intValue();
	}

	/**
	 * 计算截止时间
	 * @param val 时间值
	 * @param unit 时间单位
	 * @param stareTime 起始时间
	 * @return 截止时间
	 */
	private Long calculateDeadline(Integer val, String unit, Long stareTime) {
		if (val == null) {
			return null;
		}
		return switch (unit) {
			case "HOUR" -> stareTime + val * 60 * 60 * 1000L;
			case "DAY" -> stareTime + val * 24 * 60 * 60 * 1000L;
			case "MONTH" -> stareTime + val * 30 * 24 * 60 * 60 * 1000L;
			case "YEAR" -> stareTime + val * 365 * 24 * 60 * 60 * 1000L;
			default -> null;
		};
	}

	/**
	 * 是否存在
	 * @param id 分享ID
	 */
	private void checkExit(String id) {
		ApiDocShare docShare = apiDocShareMapper.selectByPrimaryKey(id);
		if (docShare == null) {
			throw new MSException(Translator.get("api_doc_share.not_exist"));
		}
	}
}
