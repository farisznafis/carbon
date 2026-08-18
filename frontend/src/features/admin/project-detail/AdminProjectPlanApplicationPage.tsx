import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getAdminProjectDetail,
  getProjectPlanForm,
  type AdminProjectDetail,
  type ProjectPlanFormResponse,
} from '@/api/admin';
import { PageDropdown } from '@/components/forms/shared/PageDropdown';
import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { Button } from '@/components/ui/button';
import { Card, CardContent } from '@/components/ui/card';
import { logger } from '@/lib/logger';
import { paths } from '@/routes/paths';
import { steps } from '@/features/admin/project-registration/forms/projectSteps';
import type { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';

type ProjectPlanDisplayValues = {
  projectBase?: Record<string, unknown>;
  projectApplication?: Record<string, unknown>;
};

const visibleSteps = [
  {
    ...steps[0],
    title: 'プロジェクト作成',
  },
  {
    ...steps[1],
    title: 'プロジェクト申請',
  },
];
const maxStep = visibleSteps.length;

const projectBaseLabelOverrides: Record<string, string> = {
  'projectBase.projectName': 'プロジェクト名称',
  'projectBase.projectStatus': 'ステータス',
  'projectBase.regionalCategory': '地域区分',
  'projectBase.projectNumber': 'プロジェクト番号',
  'projectBase.companyName': '会社名',
  'projectBase.repLastName': '代表者 姓',
  'projectBase.repFirstName': '代表者 名',
  'projectBase.repLastNameKana': '代表者 姓（カナ）',
  'projectBase.repFirstNameKana': '代表者 名（カナ）',
  'projectBase.repEmail': '代表者メールアドレス',
  'projectBase.repPhoneNumber': '代表者電話番号',
  'projectBase.repPosition': '代表者役職',
  'projectBase.picFirstName': '責任者 名',
  'projectBase.picLastName': '責任者 姓',
  'projectBase.picFirstNameKana': '責任者 名（カナ）',
  'projectBase.picLastNameKana': '責任者 姓（カナ）',
  'projectBase.picEmail': '責任者メールアドレス',
  'projectBase.picPhoneNumber': '責任者電話番号',
  'projectBase.picPosition': '担当役職',
  'projectBase.picDataManagement': 'データ管理責任者',
  'projectBase.newMembersTotal': '新規社員数（件）',
  'projectBase.withdrawnMembers': '退会社員数（件）',
  'projectBase.cumulativeMembers': '認証対象となる累計社員数（件）',
  'projectBase.membersTotal': '延べ社員数（件）',
  'projectBase.emissionReduction': '削減量',
  'projectBase.baselineEmissions': 'ベースライン排出量',
  'projectBase.postProjectEmission': 'プロジェクト後排出量',
  'projectBase.methodologyVersion': '方法論バージョン',
  'projectBase.methodCategory': '方法論区分',
  'projectBase.methodNumber': '方法論番号',
  'projectBase.methodName': '方法論名称',
  'projectBase.methodMainEmission': '方法論主排出源活動',
  'projectBase.methodCombiFeasibility': '方法論組み合わせ可否',
  'projectBase.methodDetails': '方法論説明',
  'projectBase.operatorCompanyId': '運営会社ID',
};

const textFallback = (value: unknown) => (value == null ? '' : String(value));

const numberFallback = (value: unknown) => {
  if (value == null || value === '') return 0;

  const numberValue = typeof value === 'number' ? value : Number(value);
  return Number.isFinite(numberValue) ? numberValue : 0;
};

const firstPresent = (...values: unknown[]) =>
  values.find((value) => value != null && value !== '');

const isRecord = (value: unknown): value is Record<string, unknown> =>
  Boolean(value) && typeof value === 'object' && !Array.isArray(value);

const resolveApplicationData = (
  response: ProjectPlanFormResponse,
): Record<string, unknown> => {
  // The plan endpoint returns the page under `data`; the older `application`
  // and `projectApplication` shapes it used to fall back on were never sent.
  const value = response.data ?? {};

  return isRecord(value) ? value : {};
};

const mapDetailToProjectBase = (
  detail: AdminProjectDetail,
): Record<string, unknown> => ({
  projectName: textFallback(detail.projectName),
  projectNumber: textFallback(detail.projectNumber ?? detail.id),
  projectStatus: textFallback(detail.projectStatus),
  emissionReduction: numberFallback(detail.emissionReduction),
  baselineEmissions: numberFallback(
    firstPresent(detail.baselineEmissions, detail.baselineEmission),
  ),
  postProjectEmission: numberFallback(
    firstPresent(detail.postProjectEmission, detail.postProjectEmissions),
  ),
  repFirstName: textFallback(detail.repFirstName),
  repFirstNameKana: textFallback(detail.repFirstNameKana),
  repLastName: textFallback(detail.repLastName),
  repLastNameKana: textFallback(detail.repLastNameKana),
  repEmail: textFallback(detail.repEmail),
  repPhoneNumber: textFallback(detail.repPhoneNumber),
  repPosition: textFallback(detail.repPosition),
  picFirstName: textFallback(detail.picFirstName),
  picFirstNameKana: textFallback(detail.picFirstNameKana),
  picLastName: textFallback(detail.picLastName),
  picLastNameKana: textFallback(detail.picLastNameKana),
  picEmail: textFallback(detail.picEmail),
  picPhoneNumber: textFallback(detail.picPhoneNumber),
  picPosition: textFallback(detail.picPosition),
  picDataManagement: textFallback(detail.dataManagementPic),
  companyName: textFallback(detail.companyName),
  regionalCategory: textFallback(detail.regionalCategory),
  newMembersTotal: numberFallback(detail.newMembersTotal),
  withdrawnMembers: numberFallback(detail.withdrawnMembers),
  cumulativeMembers: numberFallback(detail.cumulativeMembers),
  membersTotal: numberFallback(detail.membersTotal),
  methodologyVersion: textFallback(detail.methodologyVersion),
  methodCategory: textFallback(detail.methodologyCategory),
  methodNumber: textFallback(detail.methodologyNumber),
  methodName: textFallback(detail.methodologyName),
  methodMainEmission: textFallback(detail.methodologyMainEmission),
  methodCombiFeasibility: Boolean(detail.methodCombiFeasibility),
  methodDetails: textFallback(detail.methodDetails),
  operatorCompanyId: textFallback(detail.operatorCompanyId),
});

const getNestedValue = (value: unknown, path: string) =>
  path.split('.').reduce<unknown>((current, key) => {
    if (!isRecord(current)) return undefined;
    return current[key];
  }, value);

const formatDisplayValue = (field: FieldConfig<FormValues>, value: unknown) => {
  if (value == null || value === '') return '-';

  const option = field.options?.find(
    (item) => String(item.value) === String(value),
  );
  if (option) {
    return option.label;
  }

  if (typeof value === 'boolean') {
    return value ? 'あり' : 'なし';
  }

  if (Array.isArray(value)) {
    return value.length > 0 ? value.join(', ') : '-';
  }

  return String(value);
};

function InformationGrid({
  fields,
  values,
}: {
  fields: FieldConfig<FormValues>[];
  values: ProjectPlanDisplayValues;
}) {
  return (
    <div className='grid grid-cols-1 gap-4 md:grid-cols-2'>
      {fields.map((field) => {
        const fieldName = String(field.name);
        const isFullWidthField =
          field.type === 'textarea' || fieldName === 'projectBase.methodDetails';

        return (
          <div
            key={fieldName}
            className={`rounded-md border bg-muted/20 p-4 ${
              isFullWidthField ? 'md:col-span-2' : ''
            }`}
          >
            <dt className='text-sm font-medium text-foreground'>
              {projectBaseLabelOverrides[fieldName] ?? field.label}
            </dt>
            <dd className='mt-2 whitespace-pre-wrap break-words text-base font-semibold'>
              {formatDisplayValue(field, getNestedValue(values, fieldName))}
            </dd>
          </div>
        );
      })}
    </div>
  );
}

export default function AdminProjectPlanApplicationPage() {
  const { projectId } = useParams();
  const navigate = useNavigate();
  const [step, setStep] = useState(1);
  const [loading, setLoading] = useState(true);
  const [values, setValues] = useState<ProjectPlanDisplayValues>({});
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let isMounted = true;

    const load = async () => {
      if (!projectId) {
        setError('projectId が見つかりません。');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        setError(null);

        const [projectDetail, applicationResponse] = await Promise.all([
          getAdminProjectDetail(projectId),
          getProjectPlanForm(projectId, 'application'),
        ]);

        if (!isMounted) return;

        setValues({
          projectBase: mapDetailToProjectBase(projectDetail),
          projectApplication: resolveApplicationData(applicationResponse),
        });
      } catch (e) {
        logger.error('Error loading project plan application:', e);

        if (!isMounted) return;

        setError(
          e instanceof Error
            ? e.message
            : 'プロジェクト計画書を取得できませんでした。',
        );
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    void load();

    return () => {
      isMounted = false;
    };
  }, [projectId]);

  const jumpToStep = (target: number | string) => {
    const nextStep = Number(target);
    setStep(Math.min(Math.max(nextStep, 1), maxStep));
  };

  const goBack = () => setStep((currentStep) => Math.max(currentStep - 1, 1));

  const goNext = () => setStep((currentStep) => Math.min(currentStep + 1, maxStep));

  const handleUpdateProject = () => {
    if (!projectId) return;

    navigate(`${paths.adminProjectEdit(projectId)}?page=${step}`);
  };

  const currentStep = visibleSteps.find((item) => item.id === step) ?? visibleSteps[0];

  if (loading) {
    return (
      <div className='flex min-h-screen items-center justify-center'>
        読み込み中...
      </div>
    );
  }

  return (
    <FormPageWrapper
      pageName='プロジェクト計画書'
      title=''
      onBack={() => navigate(-1)}
      // backText='戻る'
      contentMaxWidth='max-w-5xl'
      wrapInCard={false}
    >
      <Card className='border-none'>
        <CardContent className='space-y-6 pt-6'>
          {error && <div className='text-sm text-destructive'>{error}</div>}

          <div className='flex flex-col gap-3 md:flex-row md:items-end md:justify-between'>
            <PageDropdown
              steps={visibleSteps}
              value={step}
              onChange={jumpToStep}
              label='ページ'
              className='w-full md:w-72'
            />
          </div>

          <section className='space-y-4'>
            <h2 className='text-xl font-semibold'>{currentStep.title}</h2>
            <InformationGrid
              fields={currentStep.fields as FieldConfig<FormValues>[]}
              values={values}
            />
          </section>

          <div className='flex items-center justify-between gap-3 pt-4'>
            <div>
              {step > 1 && (
                <Button
                  type='button'
                  variant='outline'
                  onClick={goBack}
                >
                  戻る
                </Button>
              )}
            </div>

            <div className='flex items-center gap-3'>
              <Button
                type='button'
                variant='outline'
                onClick={handleUpdateProject}
                disabled={!projectId}
              >
                更新
              </Button>

              {step < maxStep && (
                <Button
                  type='button'
                  onClick={goNext}
                >
                  次へ
                </Button>
              )}
            </div>
          </div>
        </CardContent>
      </Card>
    </FormPageWrapper>
  );
}
