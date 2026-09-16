import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";

interface OtherEquipmentData {
  oldEquipmentName: string;
  oldEquipmentId: string;
  oldEquipmentType: string;
  newEquipmentName: string;
  newEquipmentId: string;
  newEquipmentType: string;
}

interface OtherEquipmentFormProps {
  data: OtherEquipmentData;
  onChange: (data: OtherEquipmentData) => void;
}

export function OtherEquipmentForm({ data, onChange }: OtherEquipmentFormProps) {
  const updateField = (field: keyof OtherEquipmentData, value: string) => {
    onChange({ ...data, [field]: value });
  };

  return (
    <div className="space-y-6">
      <div className="space-y-4">
        <h3 className="font-semibold text-lg">旧設備情報</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="old-equipment-name">旧設備名 *</Label>
            <Input
              id="old-equipment-name"
              value={data.oldEquipmentName}
              onChange={(e) => updateField("oldEquipmentName", e.target.value)}
              placeholder="例：旧型照明器具"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="old-equipment-id">旧設備ID *</Label>
            <Input
              id="old-equipment-id"
              value={data.oldEquipmentId}
              onChange={(e) => updateField("oldEquipmentId", e.target.value)}
              placeholder="例：OLD-001"
            />
          </div>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="old-equipment-type">旧設備タイプ *</Label>
            <Input
              id="old-equipment-type"
              value={data.oldEquipmentType}
              onChange={(e) => updateField("oldEquipmentType", e.target.value)}
              placeholder="例：蛍光灯"
            />
          </div>
        </div>
      </div>

      <div className="space-y-4">
        <h3 className="font-semibold text-lg">新設備情報</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div className="space-y-2">
            <Label htmlFor="new-equipment-name">新設備名 *</Label>
            <Input
              id="new-equipment-name"
              value={data.newEquipmentName}
              onChange={(e) => updateField("newEquipmentName", e.target.value)}
              placeholder="例：LED照明器具"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="new-equipment-id">新設備ID *</Label>
            <Input
              id="new-equipment-id"
              value={data.newEquipmentId}
              onChange={(e) => updateField("newEquipmentId", e.target.value)}
              placeholder="例：NEW-001"
            />
          </div>
          <div className="space-y-2 md:col-span-2">
            <Label htmlFor="new-equipment-type">新設備タイプ *</Label>
            <Input
              id="new-equipment-type"
              value={data.newEquipmentType}
              onChange={(e) => updateField("newEquipmentType", e.target.value)}
              placeholder="例：LED"
            />
          </div>
        </div>
      </div>
    </div>
  );
}
