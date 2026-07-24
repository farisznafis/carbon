import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Plus, Pencil, Trash2 } from "lucide-react";
import { useNavigate, useParams } from "react-router-dom";
import type { Equipment } from "./types";
import { paths } from "@/routes/paths";

interface EquipmentStepProps {
  equipment: Equipment[];
  onRemoveEquipment: (id: string) => void;
}

export function EquipmentStep({
  equipment,
  onRemoveEquipment,
}: EquipmentStepProps) {
  const navigate = useNavigate();
  const { siteId = "", projectId = "" } = useParams<{
    siteId: string;
    projectId: string;
  }>();

  const handleAddEquipment = () => {
    navigate(paths.projectEquipment(siteId, projectId), {
      state: { returnTo: paths.projectRegister(siteId, projectId) },
    });
  };

  const handleEditEquipment = (equipmentId: string) => {
    navigate(`${paths.projectEquipment(siteId, projectId)}?equipmentId=${equipmentId}`, {
      state: { returnTo: paths.projectRegister(siteId, projectId) },
    });
  };

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-2xl font-bold">設備一覧</h2>
        <p className="text-muted-foreground">
          プロジェクトに登録する設備を追加してください。最低1つの設備が必要です。
        </p>
      </div>

      {equipment.length === 0 ? (
        <Card>
          <CardContent className="pt-6 text-center">
            <p className="text-muted-foreground">
              設備が登録されていません。「設備追加」ボタンから設備を追加してください。
            </p>
          </CardContent>
        </Card>
      ) : (
        <div className="space-y-4">
          {equipment.map((item, index) => (
            <Card key={item.id}>
              <CardHeader>
                <div className="flex items-center justify-between">
                  <CardTitle className="text-lg">設備情報 {index + 1}</CardTitle>
                  <div className="flex gap-2">
                    <Button
                      type="button"
                      variant="outline"
                      size="icon"
                      aria-label={`設備情報 ${index + 1}を編集`}
                      onClick={() => handleEditEquipment(item.id)}
                    >
                      <Pencil className="h-4 w-4" />
                    </Button>
                    <Button
                      type="button"
                      variant="outline"
                      size="icon"
                      aria-label={`設備情報 ${index + 1}を削除`}
                      onClick={() => onRemoveEquipment(item.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              </CardHeader>
              <CardContent>
                {item.formType === "solar" ? (
                  <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
                    <div>
                      <p className="text-small text-muted-foreground">稼働開始日</p>
                      <p className="mt-1">{item.solarData.equipmentOpDate || "-"}</p>
                    </div>
                    <div>
                      <p className="text-small text-muted-foreground">公称最大出力</p>
                      <p className="mt-1">
                        {item.solarData.nominalMaxOutput
                          ? `${item.solarData.nominalMaxOutput} kW`
                          : "-"}
                      </p>
                    </div>
                    <div>
                      <p className="text-small text-muted-foreground">PCS</p>
                      <p className="mt-1">
                        {[item.solarData.pcsManufacturer, item.solarData.pcsModel]
                          .filter(Boolean)
                          .join(" ") || "-"}
                      </p>
                    </div>
                    <div>
                      <p className="text-small text-muted-foreground">
                        アップロード済みファイル
                      </p>
                      <p className="mt-1">{item.files.length}件</p>
                    </div>
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                    <div>
                      <p className="text-sm font-medium text-muted-foreground">旧設備</p>
                      <p className="text-sm">名前: {item.oldEquipmentName}</p>
                      <p className="text-sm">ID: {item.oldEquipmentId}</p>
                      <p className="text-sm">タイプ: {item.oldEquipmentType}</p>
                    </div>
                    <div>
                      <p className="text-sm font-medium text-muted-foreground">新設備</p>
                      <p className="text-sm">名前: {item.newEquipmentName}</p>
                      <p className="text-sm">ID: {item.newEquipmentId}</p>
                      <p className="text-sm">タイプ: {item.newEquipmentType}</p>
                    </div>
                  </div>
                )}
              </CardContent>
            </Card>
          ))}
        </div>
      )}

      <div className="flex justify-center">
        <Button
          type="button"
          onClick={handleAddEquipment}
          className="w-full sm:w-auto"
        >
          <Plus className="mr-2 h-4 w-4" />
          設備追加
        </Button>
      </div>
    </div>
  );
}
