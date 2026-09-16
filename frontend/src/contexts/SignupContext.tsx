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

interface SignupContextType {
  signupData: SignupData;
  updateSignupData: (data: Partial<SignupData>) => void;
  currentStep: number;
  setCurrentStep: (step: number) => void;
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

  const updateSignupData = (data: Partial<SignupData>) => {
    setSignupData((prev) => ({ ...prev, ...data }));
  };

  const resetSignup = () => {
    setSignupData(initialData);
    setCurrentStep(1);
  };

  return (
    <SignupContext.Provider
      value={{
        signupData,
        updateSignupData,
        currentStep,
        setCurrentStep,
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
