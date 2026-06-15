import type { ChangeEvent } from "react";

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
    <input
      type="text"
      name="firstName"
      value={values.firstName}
      onChange={onChange}
      placeholder="First Name"
      className="border rounded px-3 py-2 w-full"
      required
    />
    <input
      type="text"
      name="lastName"
      value={values.lastName}
      onChange={onChange}
      placeholder="Last Name"
      className="border rounded px-3 py-2 w-full"
      required
    />
    <input
      type="email"
      name="email"
      value={values.email}
      onChange={onChange}
      placeholder="Email"
      className="border rounded px-3 py-2 w-full"
      required
    />
  </div>
);

export default Step1;
