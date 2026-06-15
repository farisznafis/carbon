import { useParams, useNavigate } from "react-router-dom";
import { FormProvider, useForm } from "react-hook-form";
import { SolarPowerEquipmentForm } from "./equipment-forms/SolarPowerEquipmentForm";
import type { SolarPowerFormData } from "@/lib/equipmentValidations";
import { PageBackButton } from "@/components/navigation/PageBackButton";
//import { BatteryEquipmentForm } from "@/components/BatteryEquipmentForm";

export const EquipmentPage = () => {
  const { type } = useParams<{ type: string }>();
  const navigate = useNavigate();
  const form = useForm<SolarPowerFormData>();

  const handleSave = () => {
    const data = form.getValues();
    // Navigate back to Step1 with the new equipment in state
    navigate("/equipmentStep", { state: { newEquipment: data } });
  };

  return (
    <FormProvider {...form}>
    
    <div className="max-w-2xl mx-auto space-y-6">
      <PageBackButton
        onClick={() => navigate(-1)}
        label='戻る'
        className='mt-6 rounded-full'
      />
      {type === "solar" && <SolarPowerEquipmentForm form={form} />}

      <div className="flex justify-center">
        <button
          type="button"
          className="px-6 py-2 bg-green-500 text-white rounded"
          onClick={handleSave}
        >
          保存
        </button>
      </div>
    </div>

    </FormProvider>
  );
};

export default EquipmentPage;
