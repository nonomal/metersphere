import { MsTableColumnData } from '@/components/pure/ms-table/type';
import type { MsFileItem } from '@/components/pure/ms-upload/types';
import type { CaseLevel } from '@/components/business/ms-case-associate/types';

import { useI18n } from '@/hooks/useI18n';

import type { AssociatedList, CustomAttributes } from '@/models/caseManagement/featureCase';
import { StatusType } from '@/enums/caseEnum';

const { t } = useI18n();

export interface ReviewResult {
  key: keyof typeof StatusType;
  icon: string;
  statusText: string;
}

// 图标评审结果
export const statusIconMap = {
  UN_REVIEWED: {
    key: 'UN_REVIEWED',
    icon: StatusType.UN_REVIEWED,
    statusText: t('caseManagement.featureCase.notReviewed'),
    color: 'text-[var(--color-text-brand)]',
  },
  UNDER_REVIEWED: {
    key: 'UNDER_REVIEWED',
    icon: StatusType.UNDER_REVIEWED,
    statusText: t('caseManagement.featureCase.reviewing'),
    color: 'text-[rgb(var(--link-6))]',
  },
  PASS: {
    key: 'PASS',
    icon: StatusType.PASS,
    statusText: t('caseManagement.featureCase.passed'),
    color: '',
  },
  UN_PASS: {
    key: 'UN_PASS',
    icon: StatusType.UN_PASS,
    statusText: t('caseManagement.featureCase.notPass'),
    color: '',
  },
  RE_REVIEWED: {
    key: 'RE_REVIEWED',
    icon: StatusType.RE_REVIEWED,
    statusText: t('caseManagement.featureCase.retrial'),
    color: 'text-[rgb(var(--warning-6))]',
  },
};
// 图标执行结果
export const executionResultMap = {
  UN_EXECUTED: {
    key: 'UN_EXECUTED',
    icon: StatusType.UN_EXECUTED,
    statusText: t('caseManagement.featureCase.nonExecution'),
    color: 'text-[var(--color-text-brand)]',
  },
  PASSED: {
    key: 'PASSED',
    icon: StatusType.PASSED,
    statusText: t('caseManagement.featureCase.passed'),
    color: '',
  },
  SKIPPED: {
    key: 'SKIPPED',
    icon: StatusType.SKIPPED,
    statusText: t('caseManagement.featureCase.skip'),
    color: 'text-[rgb(var(--link-6))]',
  },
  BLOCKED: {
    key: 'BLOCKED',
    icon: StatusType.BLOCKED,
    statusText: t('caseManagement.featureCase.chokeUp'),
    color: 'text-[rgb(var(--warning-6))]',
  },
  FAILED: {
    key: 'FAILED',
    icon: StatusType.FAILED,
    statusText: t('caseManagement.featureCase.failure'),
    color: '',
  },
};

/** *
 *
 * @description 将文件信息转换为文件格式
 * @param fileInfo 文件file
 */

export function convertToFile(fileInfo: AssociatedList): MsFileItem {
  const gatewayAddress = `${window.location.protocol}//${window.location.hostname}:${window.location.port}`;
  const fileName = fileInfo.fileType ? `${fileInfo.name}.${fileInfo.fileType || ''}` : `${fileInfo.name}`;
  const type = fileName.split('.')[1];
  const file = new File([new Blob()], `${fileName}`, {
    type: `application/${type}`,
  });
  Object.defineProperty(file, 'size', { value: fileInfo.size });
  const { id, local, isUpdateFlag, associateId } = fileInfo;
  return {
    enable: fileInfo.enable || false,
    file,
    name: fileName,
    originalName: fileInfo.originalName,
    percent: 0,
    status: 'done',
    uid: id,
    url: `${gatewayAddress}/${fileInfo.filePath || ''}`,
    local: !!local,
    deleteContent: local ? '' : 'caseManagement.featureCase.cancelLink',
    isUpdateFlag,
    associateId,
  };
}

// 返回用例等级
export function getCaseLevels(customFields: CustomAttributes[]): CaseLevel {
  const caseLevelItem = (customFields || []).find((it: any) => it.internal && it.fieldName === '用例等级');
  return (
    (caseLevelItem?.options.find((it: any) => it.value === caseLevelItem.defaultValue)?.text as CaseLevel) ||
    ('P0' as CaseLevel)
  );
}

// 处理自定义字段
export function getTableFields(customFields: CustomAttributes[], itemDataIndex: MsTableColumnData) {
  const multipleExcludes = ['MULTIPLE_SELECT', 'CHECKBOX', 'MULTIPLE_MEMBER'];
  const selectExcludes = ['MEMBER', 'RADIO', 'SELECT'];

  const currentColumnData: CustomAttributes | undefined = (customFields || []).find(
    (item: any) => itemDataIndex.dataIndex === item.fieldId
  );

  if (currentColumnData) {
    // 处理多选项
    if (multipleExcludes.includes(currentColumnData.type)) {
      const selectValue = JSON.parse(currentColumnData.defaultValue);
      return (
        (currentColumnData.options || [])
          .filter((item: any) => selectValue.includes(item.value))
          .map((it: any) => it.text)
          .join(',') || '-'
      );
    }
    if (currentColumnData.type === 'MULTIPLE_INPUT') {
      // 处理标签形式
      return JSON.parse(currentColumnData.defaultValue).join('，') || '-';
    }
    if (selectExcludes.includes(currentColumnData.type)) {
      return (
        (currentColumnData.options || [])
          .filter((item: any) => currentColumnData.defaultValue === item.value)
          .map((it: any) => it.text)
          .join() || '-'
      );
    }
    return currentColumnData.defaultValue || '-';
  }
}
