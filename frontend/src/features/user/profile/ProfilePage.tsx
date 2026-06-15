import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProfile, type ProfileViewModel } from '@/api/profile';
import { FormPageWrapper } from '@/components/layout/FormPageWrapper/FormPageWrapper';
import { useAuth } from '@/contexts/AuthContext';
import { handleError } from '@/lib/errors';
import { useProfileStore } from '@/stores/ProfileStore';
import { getHomePath } from '@/utils/getHomePath';
import { ProfileInfoCards } from './ProfileInfoCards';

const Profile = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const { profile, company, setProfile, clearProfile } = useProfileStore();

  useEffect(() => {
    let isMounted = true;

    clearProfile();
    localStorage.removeItem('profile');

    getProfile()
      .then((data: ProfileViewModel) => {
        if (!isMounted) return;
        setProfile(data);
        localStorage.setItem('profile', JSON.stringify(data));
      })
      .catch((err) => {
        if (!isMounted) return;
        clearProfile();
        handleError(
          err.message || 'プロフィールの読み込み中にエラーが発生しました',
        );
      });

    return () => {
      isMounted = false;
    };
  }, [setProfile, clearProfile]);

  return (
    <FormPageWrapper
      pageName='プロフィール情報'
      title=''
      onBack={() => navigate(getHomePath(user))}
      backText='戻る'
      contentMaxWidth='max-w-3xl'
      wrapInCard={false}
    >
      <ProfileInfoCards
        profile={profile}
        company={company}
        onPasswordChange={() => navigate('/password-change')}
      />
    </FormPageWrapper>
  );
};

export default Profile;
