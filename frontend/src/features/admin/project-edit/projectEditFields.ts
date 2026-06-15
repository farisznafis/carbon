import type { FieldConfig } from '@/components/forms/shared/GenericFormFields/GenericFormFields';
import type { UpdateAdminProjectPayload } from '@/api/admin';
import type { FormValues } from '@/features/admin/project-registration/forms/projectFormTypes';
import {
  emissionFields,
  methodologyFields,
  picFields,
  projectFields,
  representativeFields,
} from '@/features/admin/project-registration/forms/projectBaseFields';

export type AdminProjectEditField = FieldConfig<UpdateAdminProjectPayload>;

const projectBaseToEditFieldName: Partial<Record<string, keyof UpdateAdminProjectPayload>> = {
  'projectBase.projectName': 'projectName',
  'projectBase.regionalCategory': 'regionalCategory',
  'projectBase.baselineEmissions': 'baselineEmissions',
  'projectBase.postProjectEmission': 'postProjectEmission',
  'projectBase.repFirstName': 'repFirstName',
  'projectBase.repFirstNameKana': 'repFirstNameKana',
  'projectBase.repLastName': 'repLastName',
  'projectBase.repLastNameKana': 'repLastNameKana',
  'projectBase.repEmail': 'repEmail',
  'projectBase.repPhoneNumber': 'repPhoneNumber',
  'projectBase.repPosition': 'repPosition',
  'projectBase.picFirstName': 'picFirstName',
  'projectBase.picFirstNameKana': 'picFirstNameKana',
  'projectBase.picLastName': 'picLastName',
  'projectBase.picLastNameKana': 'picLastNameKana',
  'projectBase.picEmail': 'picEmail',
  'projectBase.picPhoneNumber': 'picPhoneNumber',
  'projectBase.picPosition': 'picPosition',
  'projectBase.picDataManagement': 'dataManagementPic',
  'projectBase.methodologyVersion': 'methodologyVersion',
  'projectBase.methodCategory': 'methodCategory',
  'projectBase.methodNumber': 'methodNumber',
  'projectBase.methodName': 'methodName',
  'projectBase.methodMainEmission': 'methodMainEmission',
  'projectBase.methodCombiFeasibility': 'methodCombiFeasibility',
  'projectBase.methodDetails': 'methodDetails',
};

const toEditField = (field: FieldConfig<FormValues>): AdminProjectEditField | null => {
  const editName = projectBaseToEditFieldName[field.name as string];

  if (!editName) {
    return null;
  }

  return {
    name: editName,
    label: field.label,
    type: editName === 'methodDetails' ? 'textarea' : field.type,
    placeholder: field.placeholder,
    options: field.options,
  };
};

const toEditFields = (fields: FieldConfig<FormValues>[]) =>
  fields.flatMap((field) => {
    const editField = toEditField(field);
    return editField ? [editField] : [];
  });

const dataManagementField = picFields.find(
  (field) => field.name === 'projectBase.picDataManagement',
);

const picContactFields = picFields.filter(
  (field) => field.name !== 'projectBase.picDataManagement',
);

export const adminProjectEditBasicFields: AdminProjectEditField[] = toEditFields([
  ...projectFields,
  ...emissionFields,
  ...(dataManagementField ? [dataManagementField] : []),
]);

export const adminProjectEditRepresentativeFields: AdminProjectEditField[] =
  toEditFields(representativeFields);

export const adminProjectEditPicFields: AdminProjectEditField[] =
  toEditFields(picContactFields);

export const adminProjectEditMethodologyFields: AdminProjectEditField[] =
  toEditFields(methodologyFields);
