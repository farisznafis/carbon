import { Link, useNavigate } from 'react-router-dom';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import {
  PageBackButton,
  floatingPageBackButtonClassName,
} from '@/components/navigation/PageBackButton';
import {
  Breadcrumb,
  BreadcrumbItem,
  BreadcrumbLink,
  BreadcrumbList,
  BreadcrumbPage,
  BreadcrumbSeparator,
} from '@/components/ui/breadcrumb';
import { Button } from '@/components/ui/button';
import { useAuth } from '@/contexts/AuthContext';
import { formatPersonName } from '@/features/master/components/masterDashboardUtils';
import { paths } from '@/routes/paths';
import { useProfileStore } from '@/stores/ProfileStore';
import { getHomePath } from '@/utils/getHomePath';
import { ProfileInfoCards } from './ProfileInfoCards';

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
      ? profile?.username || user?.username || 'ユーザー'
      : formattedUserName;

  /** Returns the signed-in account to its role-specific home page. */
  const handleBack = () => navigate(homePath);

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
      <PageBackButton
        onClick={handleBack}
        label=""
        aria-label="ホームに戻る"
        className={floatingPageBackButtonClassName}
      />

      <FormPageContent>
        <Breadcrumb>
          <BreadcrumbList className="flex-nowrap">
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to={homePath}>ダッシュボード</Link>
              </BreadcrumbLink>
            </BreadcrumbItem>
            <BreadcrumbSeparator />
            <BreadcrumbItem className="min-w-0">
              <BreadcrumbPage className="block max-w-48 truncate">
                {accountName}
              </BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>

        <header className="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div className="min-w-0">
            <FormPageTitle as="h2">プロファイル</FormPageTitle>
            <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
              登録されている個人情報と法人情報を確認します。
            </p>
          </div>
          <Button type="button" variant="outline" size="md" onClick={handleEdit}>
            更新
          </Button>
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
