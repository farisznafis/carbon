import type { ChangeEvent } from "react";
import { Input } from '@/components/ui/input';

interface StepProps {
  values: {
    address: string;
    city: string;
  };
  onChange: (e: ChangeEvent<HTMLInputElement>) => void;
}

const Step2 = ({ values, onChange }: StepProps) => (
  <div className="space-y-4">
    <Input
      type="text"
      name="address"
      value={values.address}
      onChange={onChange}
      placeholder="Address"
      required
    />
    <Input
      type="text"
      name="city"
      value={values.city}
      onChange={onChange}
      placeholder="City"
      required
    />
  </div>
);

export default Step2;
