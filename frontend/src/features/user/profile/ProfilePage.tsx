import { useNavigate } from 'react-router-dom';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { usePageBreadcrumb } from '@/components/navigation/pageBreadcrumbContext';
import { useAuth } from '@/contexts/AuthContext';
import { formatPersonName } from '@/features/master/components/masterDashboardUtils';
import { paths } from '@/routes/paths';
import { useProfileStore } from '@/stores/ProfileStore';
import { getHomePath } from '@/utils/getHomePath';
import { ProfileInfoCards } from './ProfileInfoCards';
import { ActionButton } from '@/components/actions';

/** Renders the signed-in user or master profile using the shared profile sections. */
const Profile = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { profile, company } = useProfileStore();
  const homePath = getHomePath(user);
  const formattedUserName = formatPersonName(
    profile?.lastName,
    profile?.firstName,
  );
  const accountName =
    formattedUserName === '-'
      ? profile?.username || user?.username || ''
      : formattedUserName;

  usePageBreadcrumb([
    { label: 'ダッシュボード', to: homePath },
    { label: accountName || 'プロファイル' },
  ]);

  /** Opens the existing password change flow from the profile page. */
  const handlePasswordChange = () => navigate(paths.passwordChange);

  /** Opens the shared user edit flow with the signed-in profile as its initial data. */
  const handleEdit = () => {
    if (!user || !profile) return;

    navigate(paths.userEdit(user.id), {
      state: {
        mode: 'profile',
        user: {
          id: user.id,
          username: profile.username ?? user.username ?? '',
          lastName: profile.lastName === '-' ? '' : profile.lastName,
          firstName: profile.firstName === '-' ? '' : profile.firstName,
          lastNameKana:
            profile.lastNameKana === '-' ? '' : profile.lastNameKana,
          firstNameKana:
            profile.firstNameKana === '-' ? '' : profile.firstNameKana,
          phoneNumber: profile.phoneNumber === '-' ? '' : profile.phoneNumber,
          position:
            profile.userPosition === '-' ? '' : profile.userPosition,
          email: profile.email === '-' ? user.email : profile.email,
          role: user.role ?? 'user',
          siteId: profile.siteId || user.siteId || '',
          siteName: profile.siteName ?? user.siteName,
        },
      },
    });
  };

  return (
    <FormStandalonePage>
      <FormPageContent>
        <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <FormPageTitle as="h2">プロファイル</FormPageTitle>
            <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
              登録されている個人情報と法人情報を確認します。
            </p>
          </div>
          <ActionButton kind="edit" onClick={handleEdit}>
            更新
          </ActionButton>
        </header>
        <ProfileInfoCards
          profile={profile}
          company={company}
          onPasswordChange={handlePasswordChange}
        />
      </FormPageContent>
    </FormStandalonePage>
  );
};

export default Profile;
