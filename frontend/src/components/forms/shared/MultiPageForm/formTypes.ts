// formTypes.ts
export type FormValues = {
  email: string;
  age?: number;
  country: string;
  birthday: string;
};

export const step1Fields = [
  { name: "email", label: "Email", type: "email" },
  { name: "age", label: "Age", type: "number" },
] as const;

export const step2Fields = [
  {
    name: "country",
    label: "Country",
    type: "select",
    options: [
      { label: "USA", value: "us" },
      { label: "Japan", value: "jp" },
    ],
  },
  { name: "birthday", label: "Birthday", type: "date" },
] as const;