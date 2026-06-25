import {
  FormPageHeader,
  FormPageShell,
  WizardProgress,
} from '@/components/forms/form-layout';
import { useSignup } from '@/contexts/SignupContext';
import { UserPlus } from 'lucide-react';
import { SignupStep1 } from './signup/SignupStep1';
import { SignupStep2 } from './signup/SignupStep2';
import { SignupStep3 } from './signup/SignupStep3';
import { SignupStep4 } from './signup/SignupStep4';
import { cn } from '@/lib/utils';

const steps = [
  { number: 1, title: '法人・個人情報' },
  { number: 2, title: '方法論・設備情報' },
  { number: 3, title: '確認' },
  { number: 4, title: '完了' },
];

const Signup = () => {
  const { currentStep } = useSignup();

  return (
    <FormPageShell>
      <div className="space-y-8">
        <FormPageHeader
          icon={<UserPlus className="h-6 w-6" />}
          title="サインアップ"
          description="排出量・カーボンクレジット・コンプライアンスを一つの画面で管理します。"
        />

        <div className="space-y-6">
          <WizardProgress currentStep={currentStep} steps={steps} />

          <div className="animate-slide-up">
            {currentStep === 1 && <SignupStep1 />}
            {currentStep === 2 && <SignupStep2 />}
            {currentStep === 3 && <SignupStep3 />}
            {currentStep === 4 && <SignupStep4 />}
          </div>
        </div>
      </div>
    </FormPageShell>
  );
};

export default Signup;
