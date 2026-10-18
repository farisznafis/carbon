import { createContext, useContext, useState, ReactNode } from "react";

interface FormValues {
  firstName: string;
  lastName: string;
  email: string;
  address: string;
  city: string;
  termsAccepted: boolean;
}

interface FormContextType {
  formValues: FormValues;
  setFormValues: React.Dispatch<React.SetStateAction<FormValues>>;
}

const initialValues: FormValues = {
  firstName: "",
  lastName: "",
  email: "",
  address: "",
  city: "",
  termsAccepted: false,
};

const FormContext = createContext<FormContextType | undefined>(undefined);

export const FormProvider = ({ children }: { children: ReactNode }) => {
  const [formValues, setFormValues] = useState<FormValues>(initialValues);

  return (
    <FormContext.Provider value={{ formValues, setFormValues }}>
      {children}
    </FormContext.Provider>
  );
};

// Custom hook for easier usage
export const useForm = () => {
  const context = useContext(FormContext);
  if (!context) {
    throw new Error("useForm must be used within a FormProvider");
  }
  return context;
};
