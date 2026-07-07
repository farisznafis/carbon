import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
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
import { PageBackButton } from '@/components/navigation/PageBackButton';
import { ProfileInfoCards } from '@/features/user/profile/ProfileInfoCards';
import { handleError } from '@/lib/errors';
import { paths } from '@/routes/paths';

export default function MasterUserProfilePage() {
  const { userId } = useParams();
  const navigate = useNavigate();
  const [profile, setProfile] = useState<ProfileViewModel | null>(null);
  const [company, setCompany] = useState<ProfileCompanyViewModel | null>(null);
  const [loading, setLoading] = useState(true);

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
            : 'プロフィールを取得できませんでした。',
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

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={() => navigate(paths.masterDashboard)}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <FormPageTitle>プロファイル</FormPageTitle>
        {loading ? (
          <div className="py-12 text-center text-sm text-muted-foreground">
            読み込み中...
          </div>
        ) : (
          <ProfileInfoCards profile={profile} company={company} />
        )}
      </FormPageContent>
    </FormStandalonePage>
  );
}
