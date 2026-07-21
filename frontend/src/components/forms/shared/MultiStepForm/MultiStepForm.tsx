import { useForm } from "@/contexts/FormContext";
import Step1 from "./steps/Step1";
import Step2 from "./steps/Step2";
import Step3 from "./steps/Step3";
import Step4 from "./steps/Step4";
import { ProgressBar } from "./ProgressBar";
import { useState } from "react";

export default function MultiStepForm() {
  const { formValues, setFormValues } = useForm();
  const [step, setStep] = useState(1);
  const totalSteps = 4;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, type, checked } = e.target;
    setFormValues((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value,
    }));
  };

  const nextStep = () => setStep((prev) => Math.min(prev + 1, totalSteps));
  const prevStep = () => setStep((prev) => Math.max(prev - 1, 1));

  const isStepValid = () => {
    switch (step) {
      case 1:
        return (
          formValues.firstName.trim() !== "" &&
          formValues.lastName.trim() !== "" &&
          formValues.email.trim() !== ""
        );
      case 2:
        return formValues.address.trim() !== "" && formValues.city.trim() !== "";
      case 3:
        return formValues.termsAccepted;
      case 4:
        return true;
      default:
        return false;
    }
  };

  return (
    <div className="max-w-md mx-auto p-6 border rounded-lg shadow space-y-4">
      <ProgressBar step={step} total={totalSteps} />

      {step === 1 && <Step1 values={formValues} onChange={handleChange} />}
      {step === 2 && <Step2 values={formValues} onChange={handleChange} />}
      {step === 3 && <Step3 values={formValues} onChange={handleChange} />}
      {step === 4 && <Step4 values={formValues} />}

      <div className="flex justify-between mt-4">
        {step > 1 && (
          <button
            onClick={prevStep}
            className="px-4 py-2 border rounded hover:bg-gray-100"
          >
            Back
          </button>
        )}
        {step < totalSteps ? (
          <button
            onClick={nextStep}
            disabled={!isStepValid()}
            className={`px-4 py-2 rounded text-white ${
              isStepValid() ? "bg-blue-500 hover:bg-blue-600" : "bg-gray-300 cursor-not-allowed"
            }`}
          >
            Next
          </button>
        ) : (
          <button
            onClick={() => alert("Form submitted!")}
            className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
          >
            Confirm & Submit
          </button>
        )}
      </div>
    </div>
  );
}
