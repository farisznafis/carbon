import { monitoringExplanationSchema } from '@/features/admin/project-registration/forms/AdminProjectApplicationForms/monitoring/MonitoringExplanation/monitoringExplanationSchema';

export const coefficientSchema = monitoringExplanationSchema.omit({
  monitoringPointDiagram: true,
});
