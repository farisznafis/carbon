import { MonitoringSolarForm } from "./MonitoringSolarForm";
import { emptyFormData } from "./emptyFormData";
import { getMonitoringSolarConfirmation } from "./solarConfirmation";

export const monitoringSolarFormDefinition = {
  Form: MonitoringSolarForm,
  emptyData: emptyFormData,
  getConfirmationItems: getMonitoringSolarConfirmation,
};
