import { useEffect, useMemo, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import type { Site } from '@/api/site';
import type { UserList } from '@/api/users';
import { updateSite, validateUpdateSite } from '@/api/site';
import { SiteForm, SiteFormData } from '@/features/master/forms/SiteForm';
import { SiteConfirmation } from './SiteConfirmation';
import { toast } from '@/hooks/use-toast';
import { logger } from '@/lib/logger';
import { useDashboardStore } from '@/stores/dashboardStore';
import { useUsersQuery } from '@/hooks/users/useUsersQuery';
import { PageSkeleton } from '@/components/feedback/loading';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { paths } from '@/routes/paths';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';

/** Mirrors the backend rule: a site's PIC is the master, an unassigned user, or a user of this site. */
const canBeSitePic = (user: UserList, siteId: string) =>
  user.role !== 'admin' &&
  (user.role === 'master' || !user.siteId || user.siteId === siteId);

const resolveSiteAssigneeId = (site: Site, users: UserList[] = []) => {
  if (site.picId) return site.picId;

  // Fallback for sites without picUserId: names are not unique, so only accept one clear match.
  const candidates = users.filter(
    (user) =>
      canBeSitePic(user, site.siteId) &&
      user.firstName === site.picFirstName &&
      user.lastName === site.picLastName &&
      (!site.picEmail || user.email === site.picEmail),
  );

  return candidates.length === 1 ? candidates[0].id : '';
};

const mapSiteToFormData = (
  site: Site,
  users?: UserList[],
): SiteFormData => ({
  siteName: site.siteName,
  siteNameKana: '',
  postalCode: site.postalCode,
  prefectureName: site.prefectureName,
  cityName: site.cityName,
  townName: '',
  blockName: site.blockName,
  addressDetail: site.addressDetail ?? site.buildingName ?? '',
  picFirstName: site.picFirstName,
  picFirstNameKana: '',
  picLastName: site.picLastName,
  picLastNameKana: '',
  picEmail: site.picEmail,
  picPhoneNumber: site.picPhoneNumber ?? '',
  picPosition: site.picPosition,
  userId: resolveSiteAssigneeId(site, users),
});

function SiteEdit() {
  const navigate = useNavigate();
  const [step, setStep] = useState<'form' | 'confirm'>('form');

  const { siteId } = useParams<{ siteId: string }>();
  const sites = useDashboardStore((state) => state.sites) ?? [];
  const initialSite = sites.find((site) => site.siteId === siteId);
  const editSite = useDashboardStore((state) => state.editSite);
  const setUsers = useDashboardStore((state) => state.setUsers);
  const storedUsers = useDashboardStore((state) => state.users);
  const usersQuery = useUsersQuery();
  const users = useMemo(
    () => (storedUsers.length > 0 ? storedUsers : usersQuery.data?.users ?? []),
    [storedUsers, usersQuery.data?.users],
  );
  const initialFormData = useMemo(
    () => (initialSite ? mapSiteToFormData(initialSite, users) : undefined),
    [initialSite, users],
  );

  const [formData, setFormData] = useState<SiteFormData | undefined>(
    undefined,
  );

  useEffect(() => {
    if (initialFormData) {
      setFormData((current) => {
        if (!current) return initialFormData;

        if (!current.userId && initialFormData.userId) {
          return {
            ...current,
            userId: initialFormData.userId,
          };
        }

        return current;
      });
    }
  }, [initialFormData]);

  useEffect(() => {
    if (usersQuery.data?.users) {
      setUsers(usersQuery.data.users);
    }
  }, [setUsers, usersQuery.data?.users]);

  const selectableUsers = useMemo(
    () =>
      users.filter(
        (user) =>
          user.role !== 'admin' &&
          ((siteId && canBeSitePic(user, siteId)) || user.id === formData?.userId),
      ),
    [users, siteId, formData?.userId],
  );

  usePageBreadcrumb([
    { label: '拠点', to: `${paths.masterDashboard}?tab=sites` },
    ...(siteId
      ? [{ label: initialSite?.siteName || '拠点', to: paths.siteDashboard(siteId) }]
      : []),
    step === 'confirm'
      ? { label: '編集', onClick: () => setStep('form') }
      : { label: '編集' },
    ...(step === 'confirm' ? [{ label: '確認' }] : []),
  ]);

  if (!siteId) {
    return <PageSkeleton />;
  }

  if (!initialSite) {
    return <PageSkeleton />;
  }

  const handleFormSubmit = async (values: SiteFormData) => {
    if (!siteId) return;

    const picuserId = values.userId || null;

    try {
      await validateUpdateSite(siteId, values, picuserId);
      setFormData(values);
      setStep('confirm');
    } catch (error) {
      logger.error('Site update validation error:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '拠点更新内容の検証に失敗しました',
        variant: 'destructive',
      });
      throw error;
    }
  };

  const handleFinalSubmit = async () => {
    if (!siteId || !formData) return;

    const picuserId = formData.userId || null;
    const selectedUser = formData.userId
      ? users.find((user) => user.id === formData.userId)
      : null;

    try {
      const result = await updateSite(siteId, formData, picuserId);

      const updatedSite: Site = {
        ...initialSite,
        siteName: formData.siteName,
        postalCode: formData.postalCode,
        prefectureName: formData.prefectureName,
        cityName: formData.cityName,
        blockName: formData.blockName,
        addressDetail: formData.addressDetail,
        buildingName: formData.addressDetail,
        // The response is the source of truth; null there means the site has no PIC.
        picId:
          result.site?.picUserId !== undefined
            ? (result.site.picUserId ?? '')
            : (formData.userId ?? initialSite.picId),
        picFirstName: selectedUser?.firstName ?? initialSite.picFirstName,
        picLastName: selectedUser?.lastName ?? initialSite.picLastName,
        picEmail: selectedUser?.email ?? initialSite.picEmail,
        picPosition: selectedUser?.position ?? initialSite.picPosition,
        picPhoneNumber: selectedUser?.phoneNumber ?? initialSite.picPhoneNumber,
        address:
          result.site?.address ??
          [
            formData.prefectureName,
            formData.cityName,
            formData.blockName,
            formData.addressDetail,
          ]
            .filter(Boolean)
            .join(''),
      };

      editSite(siteId, updatedSite);

      toast({
        title: '更新完了',
        description: '拠点を更新しました',
      });

      navigate('/master/dashboard');
    } catch (error) {
      logger.error('SiteEdit handleSubmit error:', error);
      toast({
        title: 'エラー',
        description:
          error instanceof Error ? error.message : '拠点更新に失敗しました',
        variant: 'destructive',
      });
    }
  };

  const handleBack = () => {
    setStep('form');
  };

  return (
    <>
      {step === 'form' && (
        <FormStandalonePage>
          <FormPageContent>
            <FormPageTitle>拠点編集</FormPageTitle>

            <SiteForm
              mode="edit"
              originalData={initialFormData}
              onChange={setFormData}
              onNext={handleFormSubmit}
              defaultValues={formData || initialFormData}
              userOptions={selectableUsers}
              isUserOptionsLoading={usersQuery.isLoading}
              userOptionsError={
                usersQuery.isError ? '責任者候補を取得できませんでした' : null
              }
              userOptionsHelperText="マスター、拠点未所属の社員、この拠点の社員が表示されます。"
              embedded
            />
          </FormPageContent>
        </FormStandalonePage>
      )}

      {step === 'confirm' && (
        <SiteConfirmation
          mode="edit"
          data={formData}
          originalData={initialFormData}
          onBack={handleBack}
          onSubmit={handleFinalSubmit}
        />
      )}
    </>
  );
}

export default SiteEdit;
