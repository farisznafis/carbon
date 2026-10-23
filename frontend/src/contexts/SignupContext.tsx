import { createContext, useContext, useState, ReactNode } from 'react';

export interface SignupData {
  corporateNumber: string;
  companyName: string;
  companyNameKana: string;
  companyNameEnglish: string;
  corporateEmail: string;
  corporatePhoneNumber: string;
  postalCode: string;
  prefecture: string;
  municipality: string;
  buildingName: string;
  blocknum: string;
  lastName: string;
  firstName: string;
  lastNameKana: string;
  firstNameKana: string;
  position: string;
  phoneNumber: string;
  faxNumber: string;
  email: string;
  methodology: string[];
  activityHas: 'yes' | 'no';
  activityInformation: string;
}

export type SignupFieldErrors = Partial<Record<keyof SignupData, string>>;

interface SignupContextType {
  signupData: SignupData;
  updateSignupData: (data: Partial<SignupData>) => void;
  currentStep: number;
  setCurrentStep: (step: number) => void;
  /** Errors from the final 登録, shown on the step that owns each field. */
  fieldErrors: SignupFieldErrors;
  setFieldErrors: (errors: SignupFieldErrors) => void;
  resetSignup: () => void;
}

const initialData: SignupData = {
  corporateNumber: '',
  companyName: '',
  companyNameKana: '',
  companyNameEnglish: '',
  corporateEmail: '',
  corporatePhoneNumber: '',
  postalCode: '',
  prefecture: '',
  municipality: '',
  buildingName: '',
  blocknum: '',
  lastName: '',
  firstName: '',
  lastNameKana: '',
  firstNameKana: '',
  position: '',
  phoneNumber: '',
  faxNumber: '',
  email: '',
  methodology: [],
  activityHas: 'yes',
  activityInformation: '',
};

const SignupContext = createContext<SignupContextType | undefined>(undefined);

export const SignupProvider = ({ children }: { children: ReactNode }) => {
  const [signupData, setSignupData] = useState<SignupData>(initialData);
  const [currentStep, setCurrentStep] = useState(1);
  const [fieldErrors, setFieldErrors] = useState<SignupFieldErrors>({});

  const updateSignupData = (data: Partial<SignupData>) => {
    setSignupData((prev) => ({ ...prev, ...data }));
    // The errors were about the old values; a step that passes again must not show them.
    setFieldErrors({});
  };

  const resetSignup = () => {
    setSignupData(initialData);
    setCurrentStep(1);
    setFieldErrors({});
  };

  return (
    <SignupContext.Provider
      value={{
        signupData,
        updateSignupData,
        currentStep,
        setCurrentStep,
        fieldErrors,
        setFieldErrors,
        resetSignup,
      }}
    >
      {children}
    </SignupContext.Provider>
  );
};

export const useSignup = () => {
  const context = useContext(SignupContext);
  if (context === undefined) {
    throw new Error('useSignup must be used within a SignupProvider');
  }
  return context;
};
