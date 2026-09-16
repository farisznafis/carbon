import type { ChangeEvent } from "react";

interface StepProps {
  values: {
    termsAccepted: boolean;
  };
  onChange: (e: ChangeEvent<HTMLInputElement>) => void;
}

const Step3 = ({ values, onChange }: StepProps) => (
  <div className="flex items-center space-x-2">
    <input
      type="checkbox"
      name="termsAccepted"
      checked={values.termsAccepted}
      onChange={onChange}
      required
    />
    <label>I accept the terms and conditions</label>
  </div>
);

export default Step3;
