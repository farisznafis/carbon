import type { ChangeEvent } from "react";

interface StepProps {
  values: {
    address: string;
    city: string;
  };
  onChange: (e: ChangeEvent<HTMLInputElement>) => void;
}

const Step2 = ({ values, onChange }: StepProps) => (
  <div className="space-y-4">
    <input
      type="text"
      name="address"
      value={values.address}
      onChange={onChange}
      placeholder="Address"
      className="border rounded px-3 py-2 w-full"
      required
    />
    <input
      type="text"
      name="city"
      value={values.city}
      onChange={onChange}
      placeholder="City"
      className="border rounded px-3 py-2 w-full"
      required
    />
  </div>
);

export default Step2;
