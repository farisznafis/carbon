// AddressForm.tsx
import { z } from "zod";
import { useState } from "react";
import { useForm } from "react-hook-form";
import { zodResolver } from "@hookform/resolvers/zod";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormMessage,
} from "@/components/ui/form";
import { RequiredLabel } from "@/components/forms/shared/RequiredLabel";

const schema = z.object({
  postcode: z
    .string()
    .min(7, "Postcode must be 7 digits")
    .max(7, "Postcode must be 7 digits"),
  pref: z.string().optional(),
  city: z.string().optional(),
  town: z.string().optional(),
});

type AddressFormProps = {
  defaultValues?: Partial<FormValues>;
};

type FormValues = z.infer<typeof schema>;

export function AddressForm({ defaultValues }: Readonly<AddressFormProps>) {
  const [apiError, setApiError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  
  const form = useForm<FormValues>({
    // resolver: zodResolver(schema),
    defaultValues: {
      postcode: "",
      pref: "",
      city: "",
      town: "",
      ...defaultValues,
    },
  });

  const handleSearch = async () => {
    setApiError(null);
    const values = form.getValues();
    const normalized = values.postcode.replace(/[^0-9]/g, "");

    if (normalized.length !== 7) {
      form.setError("postcode", {
        type: "manual",
        message: "Postcode must be 7 digits",
      });
      return;
    }

    setLoading(true);
    try { //API
      const res = await fetch(
        `https://zipcloud.ibsnet.co.jp/api/search?zipcode=${normalized}`
      );
      const data = await res.json();

      if (data.status !== 200 || !data.results || data.results.length === 0) {
        setApiError(data.message || "No address found.");
        return;
      }

      const r = data.results[0];
      form.setValue("pref", r.address1 || "");
      form.setValue("city", r.address2 || "");
      form.setValue("town", r.address3 || "");
    } catch {
      setApiError("Search failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  const onSubmit = () => {
    setApiError(null);
  };

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <div className="flex items-end gap-2">
          <FormField
            control={form.control}
            name="postcode"
            render={({ field }) => (
              <FormItem className="flex-1">
                <RequiredLabel>Postcode</RequiredLabel>
                <FormControl>
                  <Input placeholder="1000001" {...field} />
                </FormControl>
                <FormMessage />
              </FormItem>
            )}
          />
          <Button
            type="button"
            variant="outline"
            onClick={handleSearch}
            disabled={loading}
          >
            {loading ? "Searching..." : "Search"}
          </Button>
        </div>

        {apiError && (
          <p className="text-sm text-red-500">{apiError}</p>
        )}

        <FormField
          control={form.control}
          name="pref"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel required={false}>Prefecture</RequiredLabel>
              <FormControl>
                <Input {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="city"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel required={false}>City / Ward</RequiredLabel>
              <FormControl>
                <Input {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <FormField
          control={form.control}
          name="town"
          render={({ field }) => (
            <FormItem>
              <RequiredLabel required={false}>Town / Area</RequiredLabel>
              <FormControl>
                <Input {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />

        <Button type="submit">Submit</Button>
      </form>
    </Form>
  );
}
