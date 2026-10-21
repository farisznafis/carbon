import { Building2, LockKeyhole, UserRound } from 'lucide-react';
import type { ProfileCompanyViewModel, ProfileViewModel } from '@/api/profile';
import { ProfileDetailField } from '@/components/data-display/ProfileDetailField';
import {
  FormCard,
  FormSection,
  FormSectionStack,
} from '@/components/forms/form-layout';
import { Button } from '@/components/ui/button';

type ProfileInfoCardsProps = {
  profile: ProfileViewModel | null;
  company: ProfileCompanyViewModel | null;
  onPasswordChange?: () => void;
};

/** Formats a profile name in Japanese family-name-first order. */
function formatProfileName(lastName?: string, firstName?: string): string {
  return [lastName, firstName]
    .filter((namePart) => namePart && namePart !== '-')
    .join(' ') || '-';
}

/** Converts the backend role code into the label shown to profile users. */
function formatRole(role?: string): string {
  const normalizedRole = role?.toLowerCase();

  if (normalizedRole === 'user') return '社員';
  if (normalizedRole === 'master') return 'マスター';
  if (normalizedRole === 'admin') return '管理者';

  return role || '-';
}

/** Renders personal, corporate, and optional password information using the shared profile layout. */
export function ProfileInfoCards({
  profile,
  company,
  onPasswordChange,
}: Readonly<ProfileInfoCardsProps>) {
  const isMaster = profile?.role?.toLowerCase() === 'master';
  const roleHelpText = isMaster
    ? 'すべての機能を利用・管理できる権限です。'
    : '基本機能を利用できる権限です。';

  return (
    <FormCard>
      <FormSectionStack spacing="relaxed">
        <FormSection
          icon={<UserRound className="h-5 w-5" aria-hidden="true" />}
          title="個人情報"
        >
          <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
            <ProfileDetailField label="企業ID" value={profile?.companyId} />
            <ProfileDetailField label="ユーザーネーム" value={profile?.username} />
            <ProfileDetailField
              label="氏名"
              value={formatProfileName(profile?.lastName, profile?.firstName)}
            />
            <ProfileDetailField
              label="氏名（カナ）"
              value={formatProfileName(
                profile?.lastNameKana,
                profile?.firstNameKana,
              )}
            />
            <ProfileDetailField label="メールアドレス" value={profile?.email} />
            <ProfileDetailField label="役職" value={profile?.userPosition} />
            <ProfileDetailField label="電話番号" value={profile?.phoneNumber} />
            {!isMaster ? (
              <ProfileDetailField label="拠点名" value={profile?.siteName} />
            ) : null}
            <ProfileDetailField
              label="権限"
              value={formatRole(profile?.role)}
              helpText={roleHelpText}
            />
          </dl>
        </FormSection>

        <FormSection
          icon={<Building2 className="h-5 w-5" aria-hidden="true" />}
          title="法人情報"
          description="法人情報の更新をご希望の方は、お問い合わせください。"
        >
          <dl className="grid gap-x-10 gap-y-6 md:grid-cols-2">
            <ProfileDetailField label="法人番号" value={company?.corpNumber} />
            <ProfileDetailField label="企業名" value={company?.nameJA} />
            <ProfileDetailField
              label="企業名（カナ）"
              value={company?.nameFurigana}
            />
            <ProfileDetailField label="企業名（英語）" value={company?.nameEN} />
            <ProfileDetailField label="所在地" value={company?.address} />
            <ProfileDetailField label="電話番号" value={company?.phoneNumber} />
            <ProfileDetailField label="FAX番号" value={company?.fax} />
            {/* <ProfileDetailField label="メールアドレス" value={company?.email} /> */}
          </dl>
        </FormSection>

        {onPasswordChange ? (
          <FormSection
            icon={<LockKeyhole className="h-5 w-5" aria-hidden="true" />}
            title="パスワード変更"
            description="アカウントのセキュリティを保つためにパスワードを更新できます。"
          >
            <Button type="button" className="w-full" onClick={onPasswordChange}>
              パスワードを変更
            </Button>
          </FormSection>
        ) : null}
      </FormSectionStack>
    </FormCard>
  );
}
