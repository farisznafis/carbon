import { useEffect } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { useForm } from "react-hook-form";
import { SolarPowerEquipmentForm } from "./equipment-forms/SolarPowerEquipmentForm";
import type { SolarPowerFormData } from "@/lib/equipmentValidations";
import { FormPageWrapper } from "@/components/layout/FormPageWrapper/FormPageWrapper";
import { Form } from "@/components/ui/form";
import { Button } from "@/components/ui/button";
import { useSitesQuery } from "@/hooks/sites/useSitesQuery";
//import { BatteryEquipmentForm } from "@/components/BatteryEquipmentForm";

const EMPTY_SITE_OPTIONS: never[] = [];

export const EquipmentPage = () => {
  const { type } = useParams<{ type: string }>();
  const navigate = useNavigate();
  const form = useForm<SolarPowerFormData>();
  const sitesQuery = useSitesQuery();
  const sites = sitesQuery.data?.sites ?? EMPTY_SITE_OPTIONS;
  const siteOptionsError = sitesQuery.isError
    ? sitesQuery.error instanceof Error
      ? sitesQuery.error.message
      : "拠点情報を取得できませんでした。"
    : null;

  useEffect(() => {
    if (!form.getValues("siteId") && sites[0]) {
      form.setValue("siteId", sites[0].siteId);
    }
  }, [form, sites]);

  const handleSave = (data: SolarPowerFormData) => {
    // Navigate back to Step1 with the new equipment in state
    navigate("/equipmentStep", { state: { newEquipment: data } });
  };

  return (
    <FormPageWrapper
      pageName="設備登録"
      title="設備登録"
      onBack={() => navigate(-1)}
      backText="戻る"
      contentMaxWidth="max-w-3xl"
    >
      <Form {...form}>
        <form
          onSubmit={form.handleSubmit(handleSave)}
          className="space-y-4"
        >
          {type === "solar" && (
            <SolarPowerEquipmentForm
              form={form}
              sites={sites}
              isSiteOptionsLoading={sitesQuery.isLoading}
              siteOptionsError={siteOptionsError}
            />
          )}

          <div className="flex justify-center border-t pt-4">
            <Button type="submit" className="w-full max-w-lg">
              保存
            </Button>
          </div>
        </form>
      </Form>
    </FormPageWrapper>
  );
};

export default EquipmentPage;
