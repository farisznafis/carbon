import type { SolarPowerFormData } from '@/lib/equipmentValidations';
import type { SolarEquipment } from '@/features/user/project-registration/components/types';

export type EquipmentConfirmationState = {
  equipmentId: string;
  isEdit: boolean;
  formData: SolarPowerFormData;
  returnTo?: string;
  fromMasterDashboard: boolean;
};

export function getEquipmentConfirmationState(
  state: unknown,
): EquipmentConfirmationState | null {
  if (!state || typeof state !== 'object') return null;

  const candidate = state as Partial<EquipmentConfirmationState>;
  if (
    typeof candidate.equipmentId !== 'string' ||
    !candidate.formData ||
    typeof candidate.formData !== 'object'
  ) {
    return null;
  }

  return {
    equipmentId: candidate.equipmentId,
    isEdit: candidate.isEdit === true,
    formData: candidate.formData,
    returnTo:
      typeof candidate.returnTo === 'string' ? candidate.returnTo : undefined,
    fromMasterDashboard: candidate.fromMasterDashboard === true,
  };
}

export function getSolarEquipmentDraft(state: unknown): SolarPowerFormData | null {
  if (!state || typeof state !== 'object' || !('equipmentDraft' in state)) {
    return null;
  }

  const draft = (state as { equipmentDraft?: unknown }).equipmentDraft;
  return draft && typeof draft === 'object'
    ? (draft as SolarPowerFormData)
    : null;
}

export function getPendingSolarEquipment(state: unknown): SolarEquipment | null {
  if (!state || typeof state !== 'object' || !('pendingEquipment' in state)) {
    return null;
  }

  const equipment = (state as { pendingEquipment?: unknown }).pendingEquipment;
  if (!equipment || typeof equipment !== 'object') return null;

  const candidate = equipment as Partial<SolarEquipment>;
  return candidate.formType === 'solar' && candidate.solarData
    ? (candidate as SolarEquipment)
    : null;
}

/** Reads the site created while temporarily leaving project registration. */
export function getCreatedSiteId(state: unknown): string | null {
  if (!state || typeof state !== 'object' || !('createdSiteId' in state)) {
    return null;
  }

  const createdSiteId = (state as { createdSiteId?: unknown }).createdSiteId;
  return typeof createdSiteId === 'string' && createdSiteId
    ? createdSiteId
    : null;
}
