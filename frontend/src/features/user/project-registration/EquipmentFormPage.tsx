import { useEffect, useState } from "react";
import { useNavigate, useParams, useSearchParams } from "react-router-dom";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
//import { useUnsavedChangesWarning } from "@/hooks/useUnsavedChangesWarning";
import type { Equipment, EquipmentFormType } from "@/features/user/project-registration/components/types";
import { SolarPowerEquipmentForm } from "@/features/user/project-registration/components/equipment-forms/SolarPowerEquipmentForm";
import { useForm } from "react-hook-form";
import type { SolarPowerFormData, OtherEquipmentFormData } from "@/lib/equipmentValidations";
import { Form } from "@/components/ui/form";
import { FormPageWrapper } from "@/components/layout/FormPageWrapper/FormPageWrapper";

/**
 * HOW TO SWITCH BETWEEN FORM TYPES:
 *
 * 1. Change the default type logic below to switch the default form:
 *    - "solar" for solar power equipment form
 *    - "other" for other equipment form
 *
 * 2. To add a new form type in the future:
 *    - Add the new type to EquipmentFormType in types.ts (e.g., "wind" | "battery")
 *    - Create a new form component in equipment-forms/ folder (e.g., WindEquipmentForm.tsx)
 *    - Add the component import above
 *    - Add a new case in the render section below
 *    - Update the form type selection logic if needed
 */

const EquipmentForm = () => {
  const navigate = useNavigate();
  const { id: projectId } = useParams();
  const [searchParams] = useSearchParams();
  const equipmentId = searchParams.get("equipmentId");
  const { toast } = useToast();

  const [formType] = useState<EquipmentFormType>(() => {
    const type = searchParams.get("type");
    return type === "other" ? "other" : "solar";
  });

  // Initialize forms with zodResolver
  const solarForm = useForm<SolarPowerFormData>({
    // resolver: zodResolver(solarPowerSchema),
    mode: "onChange",
    defaultValues: {
      equipmentOpDate: "",
      operationStartDateFile: undefined,
      nominalMaxOutput: "",
      nominalMaxOutputFile: undefined,
      pcManufacturer: "",
      pcModel: "",
      pcQuantity: "",
      pcSerialNumber: "",
      pcStandbyPower: "",
      pcFile: undefined,
      hasBattery: "",
      batteryManufacturer: "",
      batteryModel: "",
      batteryCapacity: "",
      batteryFile: undefined,
      totalInvestment: "",
      totalInvestmentFile: undefined,
      hasSubsidy: "",
      receivedSubsidy: "",
      subsidyName: "",
      subsidyNameFile: undefined,
      subsidyAmount: "",
      prePurchaseUnitPrice: "",
      prePurchaseUnitPriceFile: undefined,
    },
  });

  const otherForm = useForm<OtherEquipmentFormData>({
    // resolver: zodResolver(otherEquipmentSchema),
    defaultValues: {
      oldEquipmentName: "",
      oldEquipmentId: "",
      oldEquipmentType: "",
      newEquipmentName: "",
      newEquipmentId: "",
      newEquipmentType: "",
    },
  });

  //useUnsavedChangesWarning({ hasUnsavedChanges });

  useEffect(() => {
    // Load existing equipment data if editing
    if (equipmentId) {
      const storedEquipment = localStorage.getItem(`project_${projectId}_equipment`);
      if (storedEquipment) {
        const equipmentList: Equipment[] = JSON.parse(storedEquipment);
        const equipment = equipmentList.find(e => e.id === equipmentId);
        if (equipment) {
          if (equipment.formType === "other" && equipment.oldEquipmentName) {
            otherForm.reset({
              oldEquipmentName: equipment.oldEquipmentName,
              oldEquipmentId: equipment.oldEquipmentId || "",
              oldEquipmentType: equipment.oldEquipmentType || "",
              newEquipmentName: equipment.newEquipmentName || "",
              newEquipmentId: equipment.newEquipmentId || "",
              newEquipmentType: equipment.newEquipmentType || "",
            });
          } else if (equipment.formType === "solar" && equipment.solarData) {
            solarForm.reset(equipment.solarData);
          }
        }
      }
    }
  }, [equipmentId, projectId, solarForm, otherForm]);

  const onSubmit = (data: SolarPowerFormData | OtherEquipmentFormData) => {
    const storedEquipment = localStorage.getItem(`project_${projectId}_equipment`);
    let equipmentList: Equipment[] = storedEquipment ? JSON.parse(storedEquipment) : [];

    const equipmentData = formType === "solar"
      ? ({
          id: equipmentId || `equipment-${Date.now()}`,
          formType: "solar",
          solarData: data as unknown as Equipment["solarData"],
        } as unknown as Equipment)
      : ({
          id: equipmentId || `equipment-${Date.now()}`,
          formType: "other",
          ...(data as OtherEquipmentFormData),
        } as unknown as Equipment);

    if (equipmentId) {
      equipmentList = equipmentList.map(e => e.id === equipmentId ? equipmentData : e);
    } else {
      equipmentList.push(equipmentData);
    }

    localStorage.setItem(`project_${projectId}_equipment`, JSON.stringify(equipmentList));

    toast({
      title: equipmentId ? "設備情報を更新しました" : "設備を追加しました",
      description: "設備の登録が完了しました。",
    });

    navigate(`/projects/${projectId}/register`);
  };

  const handleCancel = () => {
    navigate(`/projects/${projectId}/register`);
  };

  const pageTitle = equipmentId ? "設備編集" : "設備追加";
  const equipmentTypeLabel = formType === "solar" ? "太陽光発電設備" : "その他設備";

  return (
    <FormPageWrapper
      pageName={pageTitle}
      title={pageTitle}
      onBack={handleCancel}
      backText="戻る"
      contentMaxWidth="max-w-3xl"
    >
      <div className="space-y-6">
        <h3 className="text-xl font-semibold">
          {pageTitle} - {equipmentTypeLabel}
        </h3>

        {formType === "solar" ? (
          <Form {...solarForm}>
            <form onSubmit={solarForm.handleSubmit(onSubmit)} className="space-y-6">
              <SolarPowerEquipmentForm form={solarForm} />
              <div className="flex gap-4 border-t pt-4">
                <Button type="button" variant="outline" onClick={handleCancel} className="flex-1">
                  キャンセル
                </Button>
                <Button type="submit" className="flex-1">
                  保存
                </Button>
              </div>
            </form>
          </Form>
        ) : (
          <Form {...otherForm}>
            <form onSubmit={otherForm.handleSubmit(onSubmit)} className="space-y-6">
              {/* <OtherEquipmentForm form={otherForm} /> */}
              <div className="flex gap-4 border-t pt-4">
                <Button type="button" variant="outline" onClick={handleCancel} className="flex-1">
                  キャンセル
                </Button>
                <Button type="submit" className="flex-1">
                  保存
                </Button>
              </div>
            </form>
          </Form>
        )}
      </div>
    </FormPageWrapper>
  );
};

export default EquipmentForm;
