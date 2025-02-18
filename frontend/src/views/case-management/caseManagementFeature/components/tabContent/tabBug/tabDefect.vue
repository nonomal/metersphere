<template>
  <div>
    <div class="flex items-center justify-between">
      <div v-if="showType === 'link'" class="flex">
        <a-tooltip v-if="!total">
          <template #content>
            {{ t('caseManagement.featureCase.noAssociatedDefect') }}
            <span v-permission="['PROJECT_BUG:READ+ADD']" class="text-[rgb(var(--primary-4))]" @click="createDefect">{{
              t('caseManagement.featureCase.createDefect')
            }}</span>
          </template>
          <a-button
            v-permission="['FUNCTIONAL_CASE:READ+UPDATE']"
            :disabled="total ? false : true"
            class="mr-3"
            type="primary"
            @click="linkDefect"
          >
            {{ t('caseManagement.featureCase.linkDefect') }}
          </a-button>
        </a-tooltip>
        <a-button
          v-if="hasAnyPermission(['FUNCTIONAL_CASE:READ+UPDATE']) && total"
          :disabled="total ? false : true"
          class="mr-3"
          type="primary"
          @click="linkDefect"
        >
          {{ t('caseManagement.featureCase.linkDefect') }}
        </a-button>
        <a-button v-permission="['PROJECT_BUG:READ+ADD']" type="outline" @click="createDefect"
          >{{ t('caseManagement.featureCase.createDefect') }}
        </a-button>
      </div>
      <div v-else v-permission="['FUNCTIONAL_CASE:READ+UPDATE']" class="font-medium">{{
        t('caseManagement.featureCase.testPlanLinkList')
      }}</div>
      <div class="mb-4">
        <a-radio-group v-model:model-value="showType" type="button" class="file-show-type ml-[4px]">
          <a-radio value="link" class="show-type-icon p-[2px]">{{
            t('caseManagement.featureCase.directLink')
          }}</a-radio>
          <a-radio value="testPlan" class="show-type-icon p-[2px]">{{
            t('caseManagement.featureCase.testPlan')
          }}</a-radio>
        </a-radio-group>
        <a-input-search
          v-model:model-value="keyword"
          :placeholder="t('caseManagement.featureCase.searchByName')"
          allow-clear
          class="mx-[8px] w-[240px]"
          @search="getFetch"
          @press-enter="getFetch"
          @clear="resetFetch"
          @input="changeHandler"
        ></a-input-search>
      </div>
    </div>
    <BugList
      v-if="showType === 'link'"
      ref="bugTableListRef"
      v-model:keyword="keyword"
      :case-id="props.caseId"
      :bug-total="total"
      :bug-columns="columns"
      :load-bug-list-api="getLinkedCaseBugList"
      :load-params="{
        caseId: props.caseId,
      }"
      @link="linkDefect"
      @new="createDefect"
      @cancel-link="cancelLink"
    />
    <ms-base-table v-else v-bind="testPlanPropsRes" ref="planTableRef" v-on="testPlanTableEvent">
      <template #name="{ record }">
        <div class="flex flex-nowrap items-center">
          <div class="one-line-text">{{ characterLimit(record.name) }}</div>
          <a-popover title="" position="right" style="width: 480px">
            <div class="ml-1 text-[rgb(var(--primary-5))]">{{ t('caseManagement.featureCase.preview') }}</div>
            <template #content>
              <div v-dompurify-html="record.content" class="markdown-body" style="margin-left: 48px"> </div>
            </template>
          </a-popover>
        </div>
      </template>
      <template #handleUserName="{ record }">
        <a-tooltip :content="record.handleUserName">
          <div class="one-line-text max-w-[200px]">{{ characterLimit(record.handleUserName) || '-' }}</div>
        </a-tooltip>
      </template>
      <template #testPlanName="{ record }">
        <a-button type="text" class="px-0" @click="goToPlan(record)">{{ record.testPlanName }}</a-button>
      </template>

      <template #severityFilter="{ columnConfig }">
        <TableFilter
          v-model:visible="severityFilterVisible"
          v-model:status-filters="severityFilterValue"
          :title="(columnConfig.title as string)"
          :list="severityFilterOptions"
          value-key="value"
          @search="getFetch()"
        >
          <template #item="{ item }">
            {{ item.text }}
          </template>
        </TableFilter>
      </template>
      <template #operation="{ record }">
        <MsButton @click="cancelLink(record.id)">{{ t('caseManagement.featureCase.cancelLink') }}</MsButton>
      </template>
      <template v-if="(keyword || '').trim() === ''" #empty>
        <div class="flex w-full items-center justify-center text-[var(--color-text-4)]">
          {{ t('caseManagement.caseReview.tableNoDataNoPermission') }}
        </div>
      </template>
    </ms-base-table>
    <AddDefectDrawer
      v-model:visible="showDrawer"
      :case-id="props.caseId"
      :extra-params="{ caseId: props.caseId }"
      @success="getFetch()"
    />
    <LinkDefectDrawer
      v-model:visible="showLinkDrawer"
      :case-id="props.caseId"
      :drawer-loading="drawerLoading"
      @save="saveHandler"
    />
  </div>
</template>

<script setup lang="ts">
  /**
   * @description 用例管理-详情抽屉-tab-缺陷
   */
  import { ref } from 'vue';
  import { useRoute, useRouter } from 'vue-router';
  import { Message } from '@arco-design/web-vue';

  import MsButton from '@/components/pure/ms-button/index.vue';
  import MsBaseTable from '@/components/pure/ms-table/base-table.vue';
  import type { MsTableColumn } from '@/components/pure/ms-table/type';
  import useTable from '@/components/pure/ms-table/useTable';
  import AddDefectDrawer from './addDefectDrawer.vue';
  import BugList from './bugList.vue';
  import LinkDefectDrawer from './linkDefectDrawer.vue';
  import TableFilter from '@/views/case-management/caseManagementFeature/components/tableFilter.vue';

  import { getBugList, getCustomOptionHeader } from '@/api/modules/bug-management';
  import {
    associatedDebug,
    cancelAssociatedDebug,
    getLinkedCaseBugList,
  } from '@/api/modules/case-management/featureCase';
  import { useI18n } from '@/hooks/useI18n';
  import { useAppStore } from '@/store';
  import useFeatureCaseStore from '@/store/modules/case/featureCase';
  import { characterLimit } from '@/utils';
  import { hasAnyPermission } from '@/utils/permission';

  import { BugListItem, BugOptionItem } from '@/models/bug-management';
  import type { TableQueryParams } from '@/models/common';
  import { TestPlanRouteEnum } from '@/enums/routeEnum';

  import { makeColumns } from '@/views/case-management/caseManagementFeature/components/utils';

  const featureCaseStore = useFeatureCaseStore();

  const appStore = useAppStore();
  const { t } = useI18n();

  const props = defineProps<{
    caseId: string;
  }>();

  const showType = ref('link');

  const keyword = ref<string>('');
  const handleUserFilterValue = ref<string[]>([]);
  const handleUserFilterOptions = ref<BugOptionItem[]>([]);
  const statusFilterValue = ref<string[]>([]);
  const statusFilterOptions = ref<BugOptionItem[]>([]);
  const severityFilterOptions = ref<BugOptionItem[]>([]);
  const severityFilterVisible = ref(false);
  const severityFilterValue = ref<string[]>([]);
  const severityColumnId = ref('');

  const router = useRouter();
  const route = useRoute();

  const columns = ref<MsTableColumn>([
    {
      title: 'caseManagement.featureCase.tableColumnID',
      dataIndex: 'num',
      width: 100,
      showInTable: true,
      showTooltip: true,
      showDrag: false,
      fixed: 'left',
    },
    {
      title: 'caseManagement.featureCase.defectName',
      slotName: 'name',
      dataIndex: 'name',
      showInTable: true,
      showTooltip: false,
      width: 250,
      ellipsis: true,
      showDrag: false,
    },
    {
      title: 'caseManagement.featureCase.defectState',
      slotName: 'statusName',
      dataIndex: 'status',
      filterConfig: {
        options: [],
        labelKey: 'text',
      },
      showInTable: true,
      width: 150,
      ellipsis: true,
      showDrag: false,
    },
    {
      title: 'caseManagement.featureCase.updateUser',
      slotName: 'handleUserName',
      dataIndex: 'handleUser',
      filterConfig: {
        options: [],
        labelKey: 'text',
      },
      showInTable: true,
      width: 200,
      ellipsis: true,
    },
    {
      title: 'caseManagement.featureCase.defectSource',
      slotName: 'source',
      dataIndex: 'source',
      showInTable: true,
      showTooltip: true,
      width: 100,
      ellipsis: true,
      showDrag: false,
    },
    {
      title: 'caseManagement.featureCase.tableColumnActions',
      slotName: 'operation',
      dataIndex: 'operation',
      fixed: 'right',
      width: 100,
      showInTable: true,
      showDrag: false,
    },
  ]);

  const testPlanColumns: MsTableColumn = [
    {
      title: 'caseManagement.featureCase.tableColumnID',
      dataIndex: 'num',
      width: 100,
      showInTable: true,
      showTooltip: true,
      ellipsis: true,
      showDrag: false,
    },
    {
      title: 'caseManagement.featureCase.defectName',
      slotName: 'name',
      dataIndex: 'name',
      showInTable: true,
      showTooltip: true,
      width: 250,
      ellipsis: true,
      showDrag: false,
    },
    {
      title: 'caseManagement.featureCase.planName',
      slotName: 'testPlanName',
      dataIndex: 'testPlanName',
      showInTable: true,
      showTooltip: true,
      width: 200,
      ellipsis: true,
      showDrag: false,
    },
    {
      title: 'caseManagement.featureCase.defectState',
      slotName: 'defectState',
      dataIndex: 'defectState',
      showInTable: true,
      showTooltip: true,
      width: 150,
      ellipsis: true,
      showDrag: false,
    },
    {
      title: 'caseManagement.featureCase.updateUser',
      slotName: 'handleUserName',
      dataIndex: 'handleUser',
      filterConfig: {
        options: [],
        labelKey: 'text',
      },
      showInTable: true,
      showTooltip: true,
      width: 200,
      ellipsis: true,
    },
  ];

  const {
    propsRes: testPlanPropsRes,
    propsEvent: testPlanTableEvent,
    loadList: testPlanLinkList,
    setLoadListParams: setTestPlanListParams,
  } = useTable(getLinkedCaseBugList, {
    columns: testPlanColumns,
    heightUsed: 354,
    enableDrag: true,
  });

  function initTableParams() {
    const filterParams: Record<string, any> = {
      status: statusFilterValue.value,
      handleUser: handleUserFilterValue.value,
    };
    // TODO 不知道干啥的 要和后台同学确认一下
    filterParams[severityColumnId.value] = severityFilterValue.value;
    return {
      keyword: keyword.value,
      testPlanCaseId: props.caseId,
      projectId: appStore.currentProjectId,
      condition: {
        keyword: keyword.value,
        filter: testPlanPropsRes.value.filter,
      },
    };
  }
  const bugTableListRef = ref();
  const planTableRef = ref();

  async function initFilterOptions() {
    if (hasAnyPermission(['PROJECT_BUG:READ'])) {
      const res = await getCustomOptionHeader(appStore.currentProjectId);
      handleUserFilterOptions.value = res.handleUserOption;
      statusFilterOptions.value = res.statusOption;
      const optionsMap: Record<string, any> = {
        status: statusFilterOptions.value,
        handleUser: handleUserFilterOptions.value,
      };
      if (showType.value === 'link') {
        columns.value = makeColumns(optionsMap, columns.value);
      } else {
        const planColumnList = makeColumns(optionsMap, testPlanColumns);
        planTableRef.value.initColumn(planColumnList);
      }
    }
  }

  async function getFetch() {
    if (!hasAnyPermission(['FUNCTIONAL_CASE:READ', 'FUNCTIONAL_CASE:READ+UPDATE', 'FUNCTIONAL_CASE:READ+DELETE'])) {
      return;
    }
    if (showType.value === 'link') {
      bugTableListRef.value?.searchData(keyword.value);
    } else {
      setTestPlanListParams(initTableParams());
      await testPlanLinkList();
    }
    featureCaseStore.getCaseCounts(props.caseId);
  }
  async function resetFetch() {
    if (showType.value === 'link') {
      bugTableListRef.value?.searchData(keyword.value);
    } else {
      setTestPlanListParams({ keyword: '', projectId: appStore.currentProjectId, testPlanCaseId: props.caseId });
      await testPlanLinkList();
    }
    featureCaseStore.getCaseCounts(props.caseId);
  }
  const cancelLoading = ref<boolean>(false);
  // 取消关联
  async function cancelLink(id: string) {
    cancelLoading.value = true;
    try {
      if (showType.value === 'link') {
        await cancelAssociatedDebug(id);
        Message.success(t('caseManagement.featureCase.cancelLinkSuccess'));
        getFetch();
      }
    } catch (error) {
      console.log(error);
    } finally {
      cancelLoading.value = false;
    }
  }

  const showDrawer = ref<boolean>(false);
  function createDefect() {
    showDrawer.value = true;
  }

  const showLinkDrawer = ref<boolean>(false);

  function linkDefect() {
    showLinkDrawer.value = true;
  }

  const drawerLoading = ref<boolean>(false);
  async function saveHandler(params: TableQueryParams) {
    try {
      drawerLoading.value = true;
      await associatedDebug(params);
      Message.success(t('caseManagement.featureCase.associatedSuccess'));
      getFetch();
      showLinkDrawer.value = false;
    } catch (error) {
      console.log(error);
    } finally {
      drawerLoading.value = false;
    }
  }

  watch(
    () => showType.value,
    (val) => {
      if (val) {
        initFilterOptions();
        getFetch();
      }
    }
  );

  const total = ref<number>(0);
  async function initBugList() {
    if (!hasAnyPermission(['PROJECT_BUG:READ'])) {
      return;
    }
    const res = await getBugList({
      current: 1,
      pageSize: 10,
      sort: {},
      filter: {},
      keyword: '',
      combine: {},
      searchMode: 'AND',
      projectId: appStore.currentProjectId,
    });
    total.value = res.total;
  }

  function changeHandler() {
    if (keyword.value.trim().length === 0) {
      getFetch();
    }
  }

  // 去测试计划页面
  function goToPlan(record: BugListItem) {
    router.push({
      name: TestPlanRouteEnum.TEST_PLAN_INDEX_DETAIL,
      query: {
        ...route.query,
        id: record.testPlanId,
      },
      state: {
        params: JSON.stringify(setTestPlanListParams()),
      },
    });
  }

  watch(
    () => props.caseId,
    (val) => {
      if (val) {
        getFetch();
      }
    }
  );

  onBeforeMount(() => {
    initFilterOptions();
    getFetch();
    initBugList();
  });
</script>

<style scoped></style>
