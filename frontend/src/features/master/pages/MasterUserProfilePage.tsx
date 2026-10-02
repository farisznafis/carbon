import { useEffect, useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  getUserProfileById,
  type ProfileCompanyViewModel,
  type ProfileViewModel,
} from '@/api/profile';
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
import { formatPersonName } from '@/features/master/components/masterDashboardUtils';
import { ProfileInfoCards } from '@/features/user/profile/ProfileInfoCards';
import { handleError } from '@/lib/errors';
import { paths } from '@/routes/paths';
import { ActionButton } from '@/components/actions';

/** Converts a profile display value into a value that is safe for an editable input. */
function getEditableProfileValue(value?: string): string {
  return value && value !== '-' ? value : '';
}

export default function MasterUserProfilePage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<ProfileViewModel | null>(null);
  const [company, setCompany] = useState<ProfileCompanyViewModel | null>(null);
  const [loading, setLoading] = useState(true);
  const formattedUserName = formatPersonName(
    profile?.lastName,
    profile?.firstName,
  );
  const accountName =
    formattedUserName === '-'
      ? profile?.username || ''
      : formattedUserName;

  useEffect(() => {
    let isMounted = true;

    const loadProfile = async () => {
      if (!userId) {
        handleError('社員IDが見つかりません。');
        setLoading(false);
        return;
      }

      try {
        setLoading(true);
        const data = await getUserProfileById(userId);

        if (!isMounted) return;

        setProfile(data);
        setCompany(data.company);
      } catch (error) {
        if (!isMounted) return;
        handleError(
          error instanceof Error
            ? error.message
            : 'プロファイルを取得できませんでした。',
        );
        setProfile(null);
        setCompany(null);
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    void loadProfile();

    return () => {
      isMounted = false;
    };
  }, [userId]);

  /** Returns to the employee list on the master dashboard. */
  const handleBack = () => navigate(paths.masterDashboard);

  /** Opens the shared employee edit flow from the profile being displayed. */
  const handleEdit = () => {
    if (!userId || !profile) return;

    const profilePath = paths.masterUserProfile(userId);

    navigate(paths.userEdit(userId), {
      state: {
        returnTo: profilePath,
        user: {
          id: userId,
          username: getEditableProfileValue(profile.username),
          lastName: getEditableProfileValue(profile.lastName),
          firstName: getEditableProfileValue(profile.firstName),
          lastNameKana: getEditableProfileValue(profile.lastNameKana),
          firstNameKana: getEditableProfileValue(profile.firstNameKana),
          phoneNumber: getEditableProfileValue(profile.phoneNumber),
          position: getEditableProfileValue(profile.userPosition),
          email: getEditableProfileValue(profile.email),
          role: profile.role === 'master' ? 'master' : 'user',
          siteId: profile.siteId || '',
          siteName: profile.siteName,
        },
      },
    });
  };

  /** Opens password change for the employee currently displayed. */
  const handlePasswordChange = () => {
    if (!userId) return;
    navigate(paths.passwordChangeForUser(userId));
  };

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={handleBack}
        label=""
        aria-label="ダッシュボードに戻る"
        className={floatingPageBackButtonClassName}
      />

      <FormPageContent>
        <Breadcrumb>
          <BreadcrumbList className="flex-nowrap">
            <BreadcrumbItem>
              <BreadcrumbLink asChild>
                <Link to={paths.masterDashboard}>ダッシュボード</Link>
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
          <ActionButton
            kind="edit"
            onClick={handleEdit}
            disabled={!profile}
          >
            更新
          </ActionButton>
        </header>

        {loading ? (
          <div className="py-12 text-center text-sm text-foreground">
            読み込み中...
          </div>
        ) : (
          <ProfileInfoCards
            profile={profile}
            company={company}
            onPasswordChange={handlePasswordChange}
          />
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
