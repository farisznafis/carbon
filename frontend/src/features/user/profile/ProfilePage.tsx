import { useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { getProfile, type ProfileViewModel } from '@/api/profile';
import {
  FormPageContent,
  FormPageTitle,
  FormStandalonePage,
} from '@/components/forms/form-layout';
import { PageBackButton } from '@/components/navigation/PageBackButton';
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
          err.message || 'プロファイルの読み込み中にエラーが発生しました',
        );
      });

    return () => {
      isMounted = false;
    };
  }, [setProfile, clearProfile]);

  return (
    <FormStandalonePage>
      <PageBackButton
        onClick={() => navigate(getHomePath(user))}
        label=""
        className="absolute left-4 top-10 h-12 w-12 rounded-full p-0 sm:left-10 lg:left-16"
      />

      <FormPageContent>
        <FormPageTitle>プロファイル</FormPageTitle>
        <ProfileInfoCards
          profile={profile}
          company={company}
          onPasswordChange={() => navigate('/password-change')}
        />
      </FormPageContent>
    </FormStandalonePage>
  );
};

export default Profile;
