import { z } from "zod";
import { phoneNumberSchema, emailSchema } from "@/lib/validations";

export const companySchema = z.object({
  phoneNumber: phoneNumberSchema.optional(),
  fax: phoneNumberSchema.optional(),
  email: emailSchema.optional(),
});

export type CompanyFormValues = z.infer<typeof companySchema>;
