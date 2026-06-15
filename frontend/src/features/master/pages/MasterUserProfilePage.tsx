import { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import {
  getUserProfileById,
  type ProfileCompanyViewModel,
  type ProfileViewModel,
} from '@/api/profile';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { handleError } from '@/lib/errors';
import { paths } from '@/routes/paths';
import { ProfileInfoCards } from '@/features/user/profile/ProfileInfoCards';

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
        handleError('ユーザーIDが見つかりません。');
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
    <FormPageWrapper
      pageName='ユーザープロフィール'
      title=''
      onBack={() => navigate(paths.masterDashboard)}
      backText='戻る'
      contentMaxWidth='max-w-xl'
      wrapInCard={false}
    >
      {loading ? (
        <div className='py-12 text-center text-sm text-muted-foreground'>
          読み込み中...
        </div>
      ) : (
        <ProfileInfoCards
          profile={profile}
          company={company}
        />
      )}
    </FormPageWrapper>
  );
}
