interface StepProps {
  values: {
    firstName: string;
    lastName: string;
    email: string;
    address: string;
    city: string;
    termsAccepted: boolean;
  };
}

const Step4 = ({ values }: StepProps) => (
  <div className="space-y-2">
    <p><strong>First Name:</strong> {values.firstName}</p>
    <p><strong>Last Name:</strong> {values.lastName}</p>
    <p><strong>Email:</strong> {values.email}</p>
    <p><strong>Address:</strong> {values.address}</p>
    <p><strong>City:</strong> {values.city}</p>
    <p><strong>Terms Accepted:</strong> {values.termsAccepted ? "Yes" : "No"}</p>
  </div>
);

export default Step4;
