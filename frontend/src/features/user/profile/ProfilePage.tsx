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
import { useAuth } from '@/contexts/AuthContext';
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

  /** Returns the signed-in account to its role-specific home page. */
  const handleBack = () => navigate(homePath);

  /** Opens the existing password change flow from the profile page. */
  const handlePasswordChange = () => navigate(paths.passwordChange);

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
            <BreadcrumbItem>
              <BreadcrumbPage>プロファイル</BreadcrumbPage>
            </BreadcrumbItem>
          </BreadcrumbList>
        </Breadcrumb>

        <header>
          <FormPageTitle as="h2">プロファイル</FormPageTitle>
          <p className="mt-2 text-sm leading-6 text-muted-foreground sm:text-base">
            登録されている個人情報と法人情報を確認します。
          </p>
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
