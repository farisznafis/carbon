import type { ChangeEvent } from "react";
import { Input } from '@/components/ui/input';

interface StepProps {
  values: {
    firstName: string;
    lastName: string;
    email: string;
  };
  onChange: (e: ChangeEvent<HTMLInputElement>) => void;
}

const Step1 = ({ values, onChange }: StepProps) => (
  <div className="space-y-4">
    <Input
      type="text"
      name="firstName"
      value={values.firstName}
      onChange={onChange}
      placeholder="First Name"
      required
    />
    <Input
      type="text"
      name="lastName"
      value={values.lastName}
      onChange={onChange}
      placeholder="Last Name"
      required
    />
    <Input
      type="email"
      name="email"
      value={values.email}
      onChange={onChange}
      placeholder="Email"
      required
    />
  </div>
);

export default Step1;
