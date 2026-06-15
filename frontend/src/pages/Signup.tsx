import { useSignup } from '@/contexts/SignupContext';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { SignupStep1 } from './signup/SignupStep1';
import { SignupStep2 } from './signup/SignupStep2';
import { SignupStep3 } from './signup/SignupStep3';
import { SignupStep4 } from './signup/SignupStep4';

const Signup = () => {
  const { currentStep } = useSignup();

  const steps = [
    { number: 1, title: '法人・個人情報' },
    { number: 2, title: '方法論・設備情報' },
    { number: 3, title: '確認' },
    { number: 4, title: '完了' },
  ];

  return (
    <div className="min-h-screen flex items-center justify-center px-4 py-12">
      <div className="w-full max-w-3xl">
        {/* Progress Steps */}
        <div className="mb-8">
          <div className="flex justify-between items-center">
            {steps.map((step, index) => (
              <div key={step.number} className="flex items-center flex-1">
                <div className="flex flex-col items-center">
                  <div
                    className={`h-10 w-10 rounded-full flex items-center justify-center font-semibold transition-colors ${
                      currentStep >= step.number
                        ? 'bg-primary text-primary-foreground'
                        : 'bg-muted text-muted-foreground'
                    }`}
                  >
                    {step.number}
                  </div>
                  <span className="text-xs mt-2 text-center hidden sm:block">
                    {step.title}
                  </span>
                </div>
                {index < steps.length - 1 && (
                  <div
                    className={`h-1 flex-1 mx-2 transition-colors ${
                      currentStep > step.number ? 'bg-primary' : 'bg-muted'
                    }`}
                  />
                )}
              </div>
            ))}
          </div>
        </div>

        {/* Step Content */}
        <Card className="animate-slide-up">
          {currentStep !== 4 && (
            <CardHeader>
              <CardTitle>アカウント作成</CardTitle>
              <CardDescription>
                ステップ {currentStep} / {steps.length - 1}
              </CardDescription>
            </CardHeader>
          )}
          <CardContent className="pt-6">
            {currentStep === 1 && <SignupStep1 />}
            {currentStep === 2 && <SignupStep2 />}
            {currentStep === 3 && <SignupStep3 />}
            {currentStep === 4 && <SignupStep4 />}
          </CardContent>
        </Card>
      </div>
    </div>
  );
};

export default Signup;
