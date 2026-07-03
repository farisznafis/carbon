import {
  FormPageHeader,
  FormPageShell,
  WizardProgress,
} from '@/components/forms/form-layout';
import { Button } from '@/components/ui/button';
import { useSignup } from '@/contexts/SignupContext';
import { paths } from '@/routes/paths';
import { UserPlus } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import userPlusIcon from '@/assets/figma/signup/user-plus.svg';
import { SignupStep1 } from './signup/SignupStep1';
import { SignupStep2 } from './signup/SignupStep2';
import { SignupStep3 } from './signup/SignupStep3';
import { SignupStep4 } from './signup/SignupStep4';

const steps = [
  { number: 1, title: '法人・個人情報' },
  { number: 2, title: '方法論・設備情報' },
  { number: 3, title: '確認' },
  { number: 4, title: '完了' },
];

function SignupNavbar() {
  const navigate = useNavigate();

  return (
    <nav className="sticky top-0 z-50 border-b bg-card/50 backdrop-blur-sm">
      <div className="px-4 py-4">
        <div className="flex items-center justify-between">
          <button
            type="button"
            onClick={() => navigate(paths.home)}
            className="flex items-center space-x-2"
            aria-label="GX ホーム"
          >
            <div className="h-8 w-8 rounded-lg gradient-primary" />
            <span className="text-xl font-bold">GX</span>
          </button>

          <div className="flex items-center space-x-2">
            <Button
              type="button"
              size="sm"
              onClick={() => navigate(paths.signup)}
            >
              新規登録
            </Button>
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => navigate(paths.login)}
            >
              ログイン
            </Button>
          </div>
        </div>
      </div>
    </nav>
  );
}

function SignupHero() {
  return (
    <section className="flex w-full flex-col items-center gap-3 text-center">
      <div className="flex h-12 w-12 items-center justify-center rounded-[10px] bg-[#eef2ce]">
        <img src={userPlusIcon} alt="" className="h-4 w-4" aria-hidden="true" />
      </div>
      <div>
        <h1 className="text-[30px] font-bold leading-9 text-[#1f2628]">
          サインアップ
        </h1>
        <p className="mt-3 px-2 text-sm font-normal leading-5 text-black">
          排出量・カーボンクレジット・コンプライアンスを一つの画面で管理します。
        </p>
      </div>
    </section>
  );
}

function SignupProgress({ currentStep }: { currentStep: number }) {
  const activeStep = steps.find((step) => step.number === currentStep) ?? steps[0];

  return (
    <nav aria-label="登録ステップ" className="w-full px-5">
      <ol className="grid grid-cols-4 gap-1">
        {steps.map((step) => (
          <li key={step.number}>
            <div
              className={
                currentStep >= step.number
                  ? 'h-1 rounded-full bg-[#699267]'
                  : 'h-1 rounded-full bg-[#dde4dc]'
              }
            />
          </li>
        ))}
      </ol>
      <p className="mt-3 text-center text-sm leading-5 text-[#699267]">
        {activeStep.title}
      </p>
    </nav>
  );
}

const Signup = () => {
  const { currentStep } = useSignup();
  const stepContent = (
    <>
      {currentStep === 1 && <SignupStep1 />}
      {currentStep === 2 && <SignupStep2 />}
      {currentStep === 3 && <SignupStep3 />}
      {currentStep === 4 && <SignupStep4 />}
    </>
  );

  return (
    <>
      <main className="min-h-screen bg-[#f6f6f6] text-[#1f2628] sm:hidden">
        <div className="mx-auto min-h-screen w-full max-w-[393px] bg-[#f6f6f6]">
          <SignupNavbar />
          <div className="flex w-full flex-col items-center gap-6 pb-10 pt-6">
            <SignupHero />
            <SignupProgress currentStep={currentStep} />
            <div className="w-full animate-slide-up px-5">
              {stepContent}
            </div>
            {/* <footer className="w-full py-3 text-center text-sm leading-5 text-[#64748b]">
              © 2026 Knowledge Creation Technology
            </footer> */}
          </div>
        </div>
      </main>

      <FormPageShell className="hidden sm:block">
        <div className="space-y-8">
          <FormPageHeader
            icon={<UserPlus className="h-6 w-6" />}
            title="サインアップ"
            description="排出量・カーボンクレジット・コンプライアンスを一つの画面で管理します。"
          />

          <div className="space-y-6">
            <WizardProgress currentStep={currentStep} steps={steps} />

            <div className="animate-slide-up">
              {stepContent}
            </div>
          </div>
        </div>
      </FormPageShell>
    </>
  );
};

export default Signup;
