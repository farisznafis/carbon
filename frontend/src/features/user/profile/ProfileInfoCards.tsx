import { Building, Lock, User } from 'lucide-react';
import type { ProfileCompanyViewModel, ProfileViewModel } from '@/api/profile';
import { Button } from '@/components/ui/button';
import {
  Card,
  CardContent,
  CardDescription,
  CardFooter,
  CardHeader,
  CardTitle,
} from '@/components/ui/card';

type InfoItemProps = {
  label: string;
  value?: string | number | null;
};

type ProfileInfoCardsProps = {
  profile: ProfileViewModel | null;
  company: ProfileCompanyViewModel | null;
  onPasswordChange?: () => void;
};

function InfoItem({ label, value }: InfoItemProps) {
  return (
    <div className='min-w-0'>
      <p className='text-sm font-medium text-muted-foreground'>{label}</p>
      <p className='break-words text-base font-semibold'>
        {value == null || value === '' ? '-' : value}
      </p>
    </div>
  );
}

export function ProfileInfoCards({
  profile,
  company,
  onPasswordChange,
}: ProfileInfoCardsProps) {
  return (
    <div className='space-y-6'>
      <Card className='animate-slide-up rounded-2xl'>
        <CardHeader className='pb-4'>
          <CardTitle className='flex items-center gap-2'>
            <User className='h-5 w-5' />
            個人情報
          </CardTitle>
        </CardHeader>

        <CardContent className='grid grid-cols-1 gap-x-8 gap-y-6 sm:grid-cols-2'>
          <InfoItem
            label='企業ID'
            value={profile?.companyId}
          />
          <InfoItem
            label='ユーザ名'
            value={profile?.username}
          />
          <InfoItem
            label='氏名'
            value={profile ? `${profile.lastName} ${profile.firstName}` : '-'}
          />
          <InfoItem
            label='氏名（カナ）'
            value={
              profile
                ? `${profile.lastNameKana} ${profile.firstNameKana}`
                : '-'
            }
          />
          <InfoItem
            label='メールアドレス'
            value={profile?.email}
          />
          <InfoItem
            label='役職'
            value={profile?.userPosition}
          />
          <InfoItem
            label='電話番号'
            value={profile?.phoneNumber}
          />
        </CardContent>

        <CardFooter className='pt-2' />
      </Card>

      <Card className='animate-slide-up rounded-2xl'>
        <CardHeader className='pb-4'>
          <CardTitle className='flex items-center gap-2'>
            <Building className='h-5 w-5' />
            法人情報
          </CardTitle>
        </CardHeader>

        <CardContent className='grid grid-cols-1 gap-x-8 gap-y-6 sm:grid-cols-2'>
          <InfoItem
            label='法人番号'
            value={company?.corpNumber}
          />
          <InfoItem
            label='企業名'
            value={company?.nameJA}
          />
          <InfoItem
            label='企業名（カナ）'
            value={company?.nameFurigana}
          />
          <InfoItem
            label='企業名（英語）'
            value={company?.nameEN}
          />
          <InfoItem
            label='所在地'
            value={company?.address}
          />
          <InfoItem
            label='電話番号'
            value={company?.phoneNumber}
          />
          <InfoItem
            label='FAX番号'
            value={company?.fax}
          />
          <InfoItem
            label='メールアドレス'
            value={company?.email}
          />
        </CardContent>

        <CardFooter className='pt-2'>
          <CardDescription className='text-sm text-muted-foreground'>
            ※法人情報の更新をご希望の方は、お問い合わせください。
          </CardDescription>
        </CardFooter>
      </Card>

      {onPasswordChange && (
        <Card className='animate-slide-up rounded-2xl'>
          <CardHeader className='pb-4'>
            <CardTitle className='flex items-center gap-2'>
              <Lock className='h-5 w-5' />
              パスワード変更
            </CardTitle>
            <CardDescription>
              アカウントのセキュリティを保つためにパスワードを更新
            </CardDescription>
          </CardHeader>

          <CardContent>
            <Button
              onClick={onPasswordChange}
              className='w-full'
            >
              パスワードを変更
            </Button>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
