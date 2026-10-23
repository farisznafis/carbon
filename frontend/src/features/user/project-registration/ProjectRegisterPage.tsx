import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useDashboardStore } from "@/stores/dashboardStore";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PageSkeleton, SkeletonCard } from "@/components/feedback/loading";
import {
  FormActions,
  FormPageHeader,
  FormPageShell,
  WizardProgress,
} from "@/components/forms/form-layout";
import { FileSignature } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { 
  getProjectDetails,
  getProjectMemberTermsInfo,
  type ProjectMemberTermsInfo,
} from "@/api/projects";
import {
  joinSiteProject,
  uploadSiteProjectEquipmentFile,
  validateSiteProjectConfirmationItems,
  validateSiteProjectFinalAgreement,
  validateSiteProjectSustainabilityCommitment,
} from "@/api/site";
import { ApiError } from "@/api/_shared/errors";
import { DynamicAgreementsStep, type DynamicAgreementsStepRef } from "@/features/user/project-registration/components/DynamicAgreementsStep";
import { ContractStep, type ContractStepRef, type ContractStepFormValues } from "@/features/user/project-registration/components/ContractStep";
import { FinalTermsStep, type FinalTermsRef, type FinalTermsValues } from "@/features/user/project-registration/components/FinalTermsStep";
import { CompletionStep } from "@/features/user/project-registration/components/CompletionStep";
import { ProjectRegistrationReviewStep } from "@/features/user/project-registration/components/ProjectRegistrationReviewStep";
import {
  EquipmentRegistrationStep,
  type EquipmentRegistrationStepRef,
} from "@/features/user/project-registration/components/EquipmentRegistrationStep";
import { usePageBreadcrumb } from "@/components/navigation/pageBreadcrumbContext";
import { useSiteBreadcrumbRoot } from "@/features/user/navigation/useSiteBreadcrumbRoot";
import type {
  Equipment,
  SolarEquipment,
} from "@/features/user/project-registration/components/types";
import type { AgreementFormValues } from "@/features/user/project-registration/components/agreementsConfig";
import { paths } from "@/routes/paths";
import {
  isFromMasterDashboard,
  masterDashboardProjectMembershipState,
} from "@/features/user/project-registration/projectMembershipNavigation";
import {
  mapConfirmationItemsValidationRequest,
  mapFinalAgreementRequest,
  mapSustainabilityCommitmentRequest,
  mapProjectJoinRequest,
} from "@/features/user/project-registration/projectJoinMapper";
import {
  getCreatedSiteId,
  getPendingSolarEquipment,
} from "@/features/user/project-registration/equipmentConfirmationState";
import { getSolarFormSelectedFiles } from "@/features/user/project-registration/equipmentValidationMapper";
import { createSiteRegistrationReturnState } from "@/features/master/navigation/siteRegistrationFlow";
import type { SolarPowerFormData } from "@/lib/equipmentValidations";
import { useSitesQuery } from "@/hooks/sites/useSitesQuery";
import {
  createProjectRegistrationDraftKey,
  deleteProjectRegistrationDraft,
  loadProjectRegistrationDraft,
  PROJECT_REGISTRATION_DRAFT_VERSION,
  saveProjectRegistrationDraft,
  type ProjectRegistrationDraft,
} from "@/features/user/project-registration/projectRegistrationDraftStorage";

// ─── Constants ───────────────────────────────────────────────────────────────

const TOTAL_FORM_STEPS = 5;
const EQUIPMENT_STEP = 1;
const CONFIRMATION_ITEMS_STEP = 2;
const SUSTAINABILITY_STEP = 3;
const FINAL_AGREEMENT_STEP = 4;
const REVIEW_STEP = 5;
const COMPLETION_STEP = TOTAL_FORM_STEPS + 1;

const PROJECT_REGISTRATION_STEPS = [
  { number: EQUIPMENT_STEP, title: "設備登録" },
  { number: CONFIRMATION_ITEMS_STEP, title: "同意事項" },
  { number: SUSTAINABILITY_STEP, title: "持続可能性" },
  { number: FINAL_AGREEMENT_STEP, title: "入会規約" },
  { number: REVIEW_STEP, title: "確認" },
];

const EMPTY_CONTRACT_FORM_VALUES: ContractStepFormValues = {
  businessName: '',
  representativePosition: '',
  representativeName: '',
  contractAgreed: false,
};

const confirmationItemFieldNames: Record<string, keyof AgreementFormValues> = {
  isAgreeDataUse: "useRegisteredInfo",
  "データ利用への同意": "useRegisteredInfo",
  isAgreeExtraData: "provideAdditionalInfo",
  "本入会届に記載された以外の情報の提供への同意":
    "provideAdditionalInfo",
  isAgreeTransfer: "transferEnvironmentalValue",
  "環境価値（J-クレジット）の譲渡への同意":
    "transferEnvironmentalValue",
  isNotRegistered: "notRegisteredElsewhere",
  "他のJ-クレジット制度への未登録確認": "notRegisteredElsewhere",
  isKeidanrenParticipation: "keidanrenParticipation",
  "経団連カーボンニュートラル行動計画への参加状況":
    "keidanrenParticipation",
  isEmitter: "specifiedEmitterStatus",
  emitterCode: "specifiedEmitterCode",
  "事業者コード（9桁）": "specifiedEmitterCode",
  isAdjustedEmissionsAdded: "emissionAdditionAgreement",
  "調整後排出量への加算": "emissionAdditionAgreement",
  isSubjectToPeriodicReport: "energyRationalizationCompliance",
  isPeriodicReportTypeOne: "energyLawType",
  "第一種・第二種区分": "energyLawType",
  energyBusinessCode: "energyLawBusinessNumber",
  "事業者番号": "energyLawBusinessNumber",
  factoryCode: "energyLawFactoryNumber",
  "工場等番号": "energyLawFactoryNumber",
};

const sustainabilityCommitmentFieldNames: Record<
  string,
  keyof ContractStepFormValues
> = {
  companyName: "businessName",
  "事業者名": "businessName",
  repName: "representativeName",
  "代表者名": "representativeName",
  repRole: "representativePosition",
  "代表者役職": "representativePosition",
  hasConsent: "contractAgreed",
  "同意する": "contractAgreed",
};

const finalAgreementFieldNames: Record<string, keyof FinalTermsValues> = {
  isAgreeTerms: "finalTermsAgreed",
  "同意します": "finalTermsAgreed",
};

const getValidationErrorMessage = (error: unknown): string => {
  if (typeof error === "string") return error;
  if (Array.isArray(error)) {
    return error
      .map(getValidationErrorMessage)
      .filter(Boolean)
      .join("\n");
  }
  if (error && typeof error === "object" && "message" in error) {
    return getValidationErrorMessage(error.message);
  }
  return "";
};

const mapConfirmationItemErrors = (
  errors: unknown,
): Record<string, string> => {
  if (!errors || typeof errors !== "object" || Array.isArray(errors)) return {};

  return Object.entries(errors).reduce<Record<string, string>>(
    (fieldErrors, [path, error]) => {
      const requestFieldName =
        path.split(/::|[.[\]]/).filter(Boolean).at(-1) ?? path;
      const formFieldName = confirmationItemFieldNames[requestFieldName];
      const message = getValidationErrorMessage(error);

      if (formFieldName && message) {
        fieldErrors[formFieldName] = message;
      }
      return fieldErrors;
    },
    {},
  );
};

const mapSustainabilityCommitmentErrors = (
  errors: unknown,
): Record<string, string> => {
  if (!errors || typeof errors !== "object" || Array.isArray(errors)) return {};

  return Object.entries(errors).reduce<Record<string, string>>(
    (fieldErrors, [path, error]) => {
      const requestFieldName =
        path.split(/::|[.[\]]/).filter(Boolean).at(-1) ?? path;
      const formFieldName =
        sustainabilityCommitmentFieldNames[requestFieldName];
      const message = getValidationErrorMessage(error);

      if (formFieldName && message) {
        fieldErrors[formFieldName] = message;
      }
      return fieldErrors;
    },
    {},
  );
};

const mapFinalAgreementErrors = (
  errors: unknown,
): Record<string, string> => {
  if (!errors || typeof errors !== "object" || Array.isArray(errors)) return {};

  return Object.entries(errors).reduce<Record<string, string>>(
    (fieldErrors, [path, error]) => {
      const requestFieldName =
        path.split(/::|[.[\]]/).filter(Boolean).at(-1) ?? path;
      const formFieldName = finalAgreementFieldNames[requestFieldName];
      const message = getValidationErrorMessage(error);

      if (formFieldName && message) {
        fieldErrors[formFieldName] = message;
      }
      return fieldErrors;
    },
    {},
  );
};

// ─── Component ───────────────────────────────────────────────────────────────

const ProjectRegister = () => {
  const { siteId = "", projectId = "" } = useParams<{
    siteId: string;
    projectId: string;
  }>();
  const location = useLocation();
  const navigate = useNavigate();
  const enteredFromMasterDashboard = isFromMasterDashboard(location.state);
  const pendingEquipment = useMemo(
    () => {
      const pending = getPendingSolarEquipment(location.state);
      const createdSiteId = getCreatedSiteId(location.state);
      if (!pending || !createdSiteId) return pending;

      return {
        ...pending,
        solarData: {
          ...pending.solarData,
          siteId: createdSiteId,
        },
      };
    },
    [location.state],
  );
  const { user } = useAuth();
  const { toast } = useToast();
  const sitesQuery = useSitesQuery();
  const currentProject = useDashboardStore((state) => state.currentProject);
  const setCurrentProject = useDashboardStore((state) => state.setCurrentProject);

  // ── Refs ──────────────────────────────────────────────────────────────────
  const agreementsStepRef = useRef<DynamicAgreementsStepRef>(null);
  const contractStepRef = useRef<ContractStepRef>(null);
  const finalStepRef = useRef<FinalTermsRef>(null);
  const equipmentStepRef = useRef<EquipmentRegistrationStepRef>(null);
  const memberTermsRequestIdRef = useRef(0);

  // ── State ─────────────────────────────────────────────────────────────────
  const [currentStep, setCurrentStep] = useState(1);
  const [equipment, setEquipment] = useState<Equipment[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isProjectLoading, setIsProjectLoading] = useState(false);
  const [isDraftHydrated, setIsDraftHydrated] = useState(false);
  const [projectLoadError, setProjectLoadError] = useState<string | null>(null);
  const [memberTermsInfo, setMemberTermsInfo] =
    useState<ProjectMemberTermsInfo | null>(null);
  const [isMemberTermsLoading, setIsMemberTermsLoading] = useState(true);
  const [memberTermsLoadError, setMemberTermsLoadError] = useState<string | null>(
    null,
  );

  const [agreementFormValues, setAgreementFormValues] = useState<Partial<AgreementFormValues>>({
    useRegisteredInfo: null,
    provideAdditionalInfo: null,
    transferEnvironmentalValue: null,
    notRegisteredElsewhere: null,
    keidanrenParticipation: '',
    specifiedEmitterStatus: '',
    energyRationalizationCompliance: '',
  });

  const [contractFormValues, setContractFormValues] = useState<
    Partial<ContractStepFormValues>
  >({ ...EMPTY_CONTRACT_FORM_VALUES });

  const [finalTermsValues, setFinalTermsValues] = useState<Partial<FinalTermsValues>>({
    finalTermsAgreed: false,
  });

  const [agreementErrors, setAgreementErrors] = useState<Record<string, string>>({});
  const [contractErrors, setContractErrors] = useState<Record<string, string>>({});
  const [finalTermsErrors, setFinalTermsErrors] = useState<Record<string, string>>({});

  // ── Derived ───────────────────────────────────────────────────────────────
  const selectedProject = projectId && currentProject?.id === projectId ? currentProject : null;
  const resolvedSiteId = siteId;
  const isCompletionStep = currentStep === COMPLETION_STEP;
  const solarEquipments = useMemo(
    () =>
      equipment.filter(
        (item): item is SolarEquipment => item.formType === 'solar',
      ),
    [equipment],
  );
  const selectedEquipmentSiteId =
    solarEquipments.at(-1)?.solarData.siteId ?? '';
  const selectedEquipmentSiteName =
    sitesQuery.data?.sites.find(
      (site) => site.siteId === selectedEquipmentSiteId,
    )?.siteName ?? selectedEquipmentSiteId;
  const memberTermsOperatorName = useMemo(
    () =>
      memberTermsInfo
        ? [memberTermsInfo.operatorNameJa, memberTermsInfo.adminRepName]
            .map((name) => name.trim())
            .filter(Boolean)
            .join('・')
        : '',
    [memberTermsInfo],
  );
  const draftKey = useMemo(
    () => createProjectRegistrationDraftKey(user?.id ?? '', siteId, projectId),
    [projectId, siteId, user?.id],
  );

  // ── Navigation ────────────────────────────────────────────────────────────
  const navigateToDashboard = useCallback(() => {
    if (resolvedSiteId) {
      navigate(`/sites/${resolvedSiteId}/dashboard`);
    } else if (user?.role === 'master') {
      navigate("/master/dashboard");
    } else {
      navigate("/");
    }
  }, [navigate, user?.role, resolvedSiteId]);

  /** Returns Master registration to its sidebar tab and direct visits to the site page. */
  const navigateToMembership = useCallback(() => {
    if (enteredFromMasterDashboard) {
      navigate(paths.masterDashboardMembership);
      return;
    }

    if (resolvedSiteId) {
      navigate(paths.projectMembership(resolvedSiteId));
    } else {
      navigateToDashboard();
    }
  }, [
    enteredFromMasterDashboard,
    navigate,
    navigateToDashboard,
    resolvedSiteId,
  ]);

  const handleBack = useCallback(() => {
    if (currentStep === 1) {
      navigateToMembership();
    } else {
      setCurrentStep((prev) => prev - 1);
    }
  }, [currentStep, navigateToMembership]);

  /** Loads the project-specific text shared by the sustainability and final-terms steps. */
  const loadMemberTermsInfo = useCallback(async () => {
    const requestId = memberTermsRequestIdRef.current + 1;
    memberTermsRequestIdRef.current = requestId;
    setMemberTermsInfo(null);
    setMemberTermsLoadError(null);
    setIsMemberTermsLoading(true);

    if (!projectId) {
      setMemberTermsLoadError('入会規約情報が見つかりませんでした');
      setIsMemberTermsLoading(false);
      return;
    }

    try {
      const termsInfo = await getProjectMemberTermsInfo(projectId);
      if (memberTermsRequestIdRef.current !== requestId) return;
      setMemberTermsInfo(termsInfo);
    } catch (error) {
      if (memberTermsRequestIdRef.current !== requestId) return;
      setMemberTermsLoadError(
        error instanceof Error
          ? error.message
          : '入会規約情報の取得に失敗しました',
      );
    } finally {
      if (memberTermsRequestIdRef.current === requestId) {
        setIsMemberTermsLoading(false);
      }
    }
  }, [projectId]);

  /** Opens site registration and preserves the current equipment draft for return. */
  const handleAddSite = useCallback(
    (draft: SolarPowerFormData) => {
      const currentSolarEquipment = equipment.find(
        (item): item is SolarEquipment => item.formType === 'solar',
      );
      const pendingEquipmentDraft: SolarEquipment = {
        id: currentSolarEquipment?.id ?? `equipment-${Date.now()}`,
        formType: 'solar',
        solarData: draft,
        files: currentSolarEquipment?.files ?? [],
      };

      navigate(paths.masterSiteRegister, {
        state: createSiteRegistrationReturnState({
          path: paths.projectRegister(siteId, projectId),
          state: {
            ...(enteredFromMasterDashboard
              ? masterDashboardProjectMembershipState
              : {}),
            pendingEquipment: pendingEquipmentDraft,
          },
        }),
      });
    },
    [enteredFromMasterDashboard, equipment, navigate, projectId, siteId],
  );

  /** Keeps the currently mounted equipment form synchronized with the persistent draft. */
  const handleEquipmentDraftChange = useCallback(
    (solarData: SolarPowerFormData) => {
      setEquipment((currentEquipment) => {
        const currentSolarEquipment = currentEquipment.find(
          (item): item is SolarEquipment => item.formType === 'solar',
        );
        return [
          {
            id: currentSolarEquipment?.id ?? `equipment-${Date.now()}`,
            formType: 'solar',
            solarData,
            files: currentSolarEquipment?.files ?? [],
          },
        ];
      });
    },
    [],
  );

  // ── Effects ───────────────────────────────────────────────────────────────
  useEffect(() => {
    if (!projectId || selectedProject?.id) return;

    let isMounted = true;

    const loadProject = async () => {
      setIsProjectLoading(true);
      setProjectLoadError(null);

      try {
        const project = await getProjectDetails(projectId);
        if (!isMounted) return;
        setCurrentProject(project);
      } catch {
        if (!isMounted) return;
        setProjectLoadError("プロジェクト情報の取得に失敗しました");
        toast({
          title: "エラー",
          description: "プロジェクト情報の取得に失敗しました",
          variant: "destructive",
        });
      } finally {
        if (isMounted) setIsProjectLoading(false);
      }
    };

    loadProject();
    return () => { isMounted = false; };
  }, [projectId, selectedProject?.id, setCurrentProject, toast]);

  useEffect(() => {
    void loadMemberTermsInfo();

    return () => {
      memberTermsRequestIdRef.current += 1;
    };
  }, [loadMemberTermsInfo]);

  useEffect(() => {
    let isMounted = true;

    /** Restores a saved draft before rendering an editable registration step. */
    const hydrateDraft = async () => {
      let savedDraft: ProjectRegistrationDraft | null = null;
      try {
        savedDraft = await loadProjectRegistrationDraft(draftKey);
      } catch {
        // IndexedDB errors fall back to the previous equipment cache below.
      }

      try {
        if (!isMounted) return;

        if (savedDraft) {
          setCurrentStep(
            Math.min(Math.max(savedDraft.currentStep, EQUIPMENT_STEP), REVIEW_STEP),
          );
          setAgreementFormValues(savedDraft.agreementFormValues);
          setContractFormValues(savedDraft.contractFormValues);
          setFinalTermsValues(savedDraft.finalTermsValues);
        }

        if (pendingEquipment) {
          setEquipment([pendingEquipment]);
          return;
        }

        if (savedDraft?.equipment.length) {
          setEquipment(savedDraft.equipment);
          return;
        }

      const stored = localStorage.getItem(`project_${projectId}_equipment`);
      const storedEquipment: Equipment[] = stored ? JSON.parse(stored) : [];
      const latestEquipmentWithFiles = storedEquipment
        .filter(
          (item): item is Extract<Equipment, { formType: 'solar' }> =>
            item.formType === 'solar' && (item.files?.length ?? 0) > 0,
        )
        .at(-1);
      const activeEquipment = latestEquipmentWithFiles
        ? [latestEquipmentWithFiles]
        : storedEquipment.slice(-1);

      setEquipment(activeEquipment);
      if (latestEquipmentWithFiles) {
        localStorage.setItem(
          `project_${projectId}_equipment`,
          JSON.stringify(activeEquipment),
        );
      }
      } catch {
        // A malformed legacy cache must never block the registration form.
      } finally {
        if (isMounted) setIsDraftHydrated(true);
      }
    };

    void hydrateDraft();
    return () => {
      isMounted = false;
    };
  }, [draftKey, pendingEquipment, projectId]);

  useEffect(() => {
    if (!isDraftHydrated || isCompletionStep) return;

    const draft: ProjectRegistrationDraft = {
      version: PROJECT_REGISTRATION_DRAFT_VERSION,
      currentStep,
      equipment,
      agreementFormValues,
      contractFormValues,
      finalTermsValues,
      updatedAt: new Date().toISOString(),
    };

    void saveProjectRegistrationDraft(draftKey, draft).catch(() => {
      // Saving a draft is best-effort and must not interrupt form input.
    });
  }, [
    agreementFormValues,
    contractFormValues,
    currentStep,
    draftKey,
    equipment,
    finalTermsValues,
    isCompletionStep,
    isDraftHydrated,
  ]);

  // ── Handlers ──────────────────────────────────────────────────────────────
  const validateStep = async (): Promise<boolean> => {
    switch (currentStep) {
      case EQUIPMENT_STEP:
        if (!equipmentStepRef.current) return false;

        setIsSubmitting(true);
        try {
          const validatedEquipment =
            await equipmentStepRef.current.validate();
          setEquipment([validatedEquipment]);
          return true;
        } catch (error) {
          // Field errors are listed by the step's FormErrorSummary; only an unmapped error needs a toast.
          if (!equipmentStepRef.current?.hasFieldErrors()) {
            toast({
              title: "設備情報を確認できませんでした",
              description:
                error instanceof Error
                  ? error.message
                  : "設備情報の検証に失敗しました。",
              variant: "destructive",
            });
          }
          return false;
        } finally {
          setIsSubmitting(false);
        }

      case CONFIRMATION_ITEMS_STEP:
        if (!agreementsStepRef.current) return false;

        setIsSubmitting(true);
        setAgreementErrors({});
        try {
          const currentAgreementValues =
            agreementsStepRef.current.getValues();
          setAgreementFormValues(currentAgreementValues);

          await validateSiteProjectConfirmationItems(
            mapConfirmationItemsValidationRequest(currentAgreementValues, {
              projectId,
              processedBy: user?.id ?? "",
              companyId: user?.companyId ?? "",
            }),
          );
          return true;
        } catch (error) {
          const fieldErrors =
            error instanceof ApiError
              ? mapConfirmationItemErrors(error.payload?.errors)
              : {};
          setAgreementErrors(fieldErrors);
          if (Object.keys(fieldErrors).length === 0) {
            toast({
              title: "同意事項を確認できませんでした",
              description:
                error instanceof Error
                  ? error.message
                  : "同意事項の検証に失敗しました。",
              variant: "destructive",
            });
          }
          return false;
        } finally {
          setIsSubmitting(false);
        }

      case SUSTAINABILITY_STEP:
        if (!contractStepRef.current) return false;

        setIsSubmitting(true);
        setContractErrors({});
        try {
          const currentContractValues = contractStepRef.current.getValues();
          setContractFormValues(currentContractValues);

          await validateSiteProjectSustainabilityCommitment(
            mapSustainabilityCommitmentRequest(currentContractValues),
          );
          return true;
        } catch (error) {
          const fieldErrors =
            error instanceof ApiError
              ? mapSustainabilityCommitmentErrors(error.payload?.errors)
              : {};
          setContractErrors(fieldErrors);
          if (Object.keys(fieldErrors).length === 0) {
            toast({
              title: "持続可能性の同意内容を確認できませんでした",
              description:
                error instanceof Error
                  ? error.message
                  : "持続可能性約束の検証に失敗しました。",
              variant: "destructive",
            });
          }
          return false;
        } finally {
          setIsSubmitting(false);
        }

      case FINAL_AGREEMENT_STEP:
        if (!finalStepRef.current) return false;

        setIsSubmitting(true);
        setFinalTermsErrors({});
        try {
          const currentFinalTermsValues = finalStepRef.current.getValues();
          setFinalTermsValues(currentFinalTermsValues);

          await validateSiteProjectFinalAgreement(
            mapFinalAgreementRequest(currentFinalTermsValues),
          );
          return true;
        } catch (error) {
          const fieldErrors =
            error instanceof ApiError
              ? mapFinalAgreementErrors(error.payload?.errors)
              : {};
          setFinalTermsErrors(fieldErrors);
          if (Object.keys(fieldErrors).length === 0) {
            toast({
              title: "入会規約を確認できませんでした",
              description:
                error instanceof Error
                  ? error.message
                  : "入会規約の検証に失敗しました。",
              variant: "destructive",
            });
          }
          return false;
        } finally {
          setIsSubmitting(false);
        }

      default:
        return true;
    }
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    try {
      if (solarEquipments.length === 0 || !projectId) {
        throw new Error('登録する太陽光発電設備が見つかりません。');
      }

      const equipmentsWithUploadedFiles = await Promise.all(
        solarEquipments.map(async (solarEquipment) => {
          if ((solarEquipment.files?.length ?? 0) > 0) {
            return solarEquipment;
          }

          const selectedFiles = getSolarFormSelectedFiles(
            solarEquipment.solarData,
          );
          if (selectedFiles.length === 0) {
            throw new Error(
              '設備ファイル情報が見つかりません。設備情報を再度確認してください。',
            );
          }

          const uploadedFiles = await Promise.all(
            selectedFiles.map(
                ({ fileType, file }) =>
                  uploadSiteProjectEquipmentFile(
                    solarEquipment.solarData.siteId,
                    fileType,
                    file,
                  ),
            ),
          );

          return {
            ...solarEquipment,
            files: uploadedFiles,
          };
        }),
      );
      const uploadedEquipmentById = new Map(
        equipmentsWithUploadedFiles.map((item) => [item.id, item]),
      );
      const updatedEquipment = equipment.map((item) =>
        item.formType === 'solar'
          ? uploadedEquipmentById.get(item.id) ?? item
          : item,
      );
      setEquipment(updatedEquipment);
      localStorage.setItem(
        `project_${projectId}_equipment`,
        JSON.stringify(updatedEquipment),
      );

      const equipmentToRegister =
        equipmentsWithUploadedFiles[equipmentsWithUploadedFiles.length - 1];
      if (!equipmentToRegister) {
        throw new Error('登録する太陽光発電設備が見つかりません。');
      }

      const registrationPayload = mapProjectJoinRequest(
        equipmentToRegister,
        agreementFormValues,
        contractFormValues,
        finalTermsValues,
        {
          projectId,
          processedBy: user?.id ?? "",
          companyId: user?.companyId ?? "",
        },
      );
      await joinSiteProject(projectId, registrationPayload);
      void deleteProjectRegistrationDraft(draftKey).catch(() => {
        // A cleanup failure must not turn a completed backend request into an error.
      });

      const storedProjects = localStorage.getItem("appliedProjects");
      const appliedProjects = storedProjects ? JSON.parse(storedProjects) : [];
      appliedProjects.push({
        id: projectId,
        name: "Jクレジット制度 2024年度プロジェクト",
        description: "省エネルギー設備導入によるCO2削減プロジェクト",
        methodology: "EN-S-001",
        status: "pending",
        appliedDate: new Date().toISOString(),
        equipment: equipment.length,
        lastUpdate: new Date().toISOString().split("T")[0],
      });
      localStorage.setItem("appliedProjects", JSON.stringify(appliedProjects));
      localStorage.removeItem(`project_${projectId}_equipment`);

      setCurrentStep(COMPLETION_STEP);
      toast({ title: "申請完了", description: "プロジェクト参加申請が完了しました" });
      setTimeout(() => navigateToDashboard(), 2000);
    } catch (error) {
      toast({
        title: "エラー",
        description:
          error instanceof Error ? error.message : "申請の送信に失敗しました",
        variant: "destructive",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleNext = async () => {
    if (await validateStep()) {
      setCurrentStep((prev) => prev + 1);
      // Only a new step starts at the top; a failed one stays by its error summary.
      window.scrollTo({ top: 0, behavior: "smooth" });
    }
  };

  // ── Render steps ──────────────────────────────────────────────────────────
  const renderStep = () => {
    const requiresMemberTerms =
      currentStep === SUSTAINABILITY_STEP ||
      currentStep === FINAL_AGREEMENT_STEP;

    if (requiresMemberTerms && isMemberTermsLoading) {
      return <SkeletonCard label="入会規約情報を読み込んでいます" className="min-h-72" />;
    }

    if (requiresMemberTerms && (!memberTermsInfo || memberTermsLoadError)) {
      return (
        <Card className="rounded-lg border-border shadow-none">
          <CardContent className="space-y-5 p-6 text-center sm:p-10">
            <p className="text-destructive-strong">
              {memberTermsLoadError || '入会規約情報を取得できませんでした'}
            </p>
            <FormActions
              actions={[
                {
                  id: 'back',
                  label: '戻る',
                  variant: 'outline',
                  onClick: handleBack,
                },
                {
                  id: 'retry',
                  label: '再読み込み',
                  onClick: () => void loadMemberTermsInfo(),
                },
              ]}
            />
          </CardContent>
        </Card>
      );
    }

    switch (currentStep) {
      case EQUIPMENT_STEP:
        return (
          <EquipmentRegistrationStep
            ref={equipmentStepRef}
            isSubmitting={isSubmitting}
            onAddSite={handleAddSite}
            onBack={handleBack}
            onNext={handleNext}
            onValueChange={handleEquipmentDraftChange}
            initialEquipment={equipment.find(
              (item): item is Extract<Equipment, { formType: 'solar' }> =>
                item.formType === 'solar',
            )}
          />
        );
      case CONFIRMATION_ITEMS_STEP:
        return (
          <DynamicAgreementsStep
            ref={agreementsStepRef}
            projectName={selectedProject.projectName}
            formValues={agreementFormValues}
            isSubmitting={isSubmitting}
            onBack={handleBack}
            onNext={handleNext}
            onValueChange={(values) => {
              setAgreementFormValues(values);
              if (Object.keys(agreementErrors).length > 0) {
                setAgreementErrors({});
              }
            }}
            errors={agreementErrors}
          />
        );
      case SUSTAINABILITY_STEP:
        return (
          <ContractStep
            ref={contractStepRef}
            operatorName={memberTermsOperatorName}
            methodologyNumber={selectedProject.methodNumber}
            methodologyName={selectedProject.methodName}
            methodologyVersion={selectedProject.methodologyVersion}
            formValues={contractFormValues}
            isSubmitting={isSubmitting}
            onBack={handleBack}
            onNext={handleNext}
            onValueChange={(values) => {
              setContractFormValues(values);
              if (Object.keys(contractErrors).length > 0) {
                setContractErrors({});
              }
            }}
            errors={contractErrors}
          />
        );
      case FINAL_AGREEMENT_STEP:
        return (
          <FinalTermsStep
            ref={finalStepRef}
            projectName={memberTermsInfo.projectName}
            operatorName={memberTermsOperatorName}
            projectOverview={memberTermsInfo.projectOverview}
            usePurpose={memberTermsInfo.usePurpose}
            startedAt={memberTermsInfo.startedAt}
            formValues={finalTermsValues}
            isSubmitting={isSubmitting}
            onBack={handleBack}
            onNext={handleNext}
            onValueChange={(values) => {
              setFinalTermsValues(values);
              if (Object.keys(finalTermsErrors).length > 0) {
                setFinalTermsErrors({});
              }
            }}
            errors={finalTermsErrors}
          />
        );
      case REVIEW_STEP:
        return (
          <ProjectRegistrationReviewStep
            projectName={selectedProject.projectName}
            siteName={selectedEquipmentSiteName}
            equipments={solarEquipments}
            methodologyNumber={selectedProject.methodNumber}
            methodologyVersion={selectedProject.methodologyVersion}
            agreementValues={agreementFormValues}
            contractValues={contractFormValues}
            finalTermsValues={finalTermsValues}
            isSubmitting={isSubmitting}
            onBack={() => setCurrentStep(FINAL_AGREEMENT_STEP)}
            onSubmit={() => void handleSubmit()}
          />
        );
      default:
        return null;
    }
  };

  // Earlier wizard steps are reached with each step's own 戻る button.
  const siteBreadcrumbRoot = useSiteBreadcrumbRoot(resolvedSiteId);
  usePageBreadcrumb([
    ...(enteredFromMasterDashboard
      ? [{ label: 'プロジェクトの入会届', to: paths.masterDashboardMembership }]
      : [
          ...siteBreadcrumbRoot,
          ...(resolvedSiteId
            ? [{ label: 'プロジェクトへの入会', to: paths.projectMembership(resolvedSiteId) }]
            : []),
        ]),
    { label: selectedProject?.projectName || 'プロジェクト参加申請' },
  ]);

  // ── Loading / error states ─────────────────────────────────────────────────
  if (isProjectLoading) {
    return (
      <PageSkeleton label="プロジェクト情報を読み込んでいます" />
    );
  }

  if (!isDraftHydrated) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-foreground text-lg">入力内容を復元中...</p>
      </div>
    );
  }

  if (!selectedProject) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center space-y-4">
            <p className="text-foreground">
              {projectLoadError || "プロジェクト情報が見つかりません"}
            </p>
            <Button onClick={navigateToMembership}>入会届に戻る</Button>
          </CardContent>
        </Card>
      </div>
    );
  }

  // Renders the success screen as a centered completion card without the form header.
  if (isCompletionStep) {
    return (
      <FormPageShell className="flex min-h-[calc(100dvh-4rem-1px)] items-center py-8">
        <div className="w-full animate-slide-up">
          <CompletionStep onNavigateToDashboard={navigateToDashboard} />
        </div>
      </FormPageShell>
    );
  }

  // ── Main render ───────────────────────────────────────────────────────────
  return (
    <FormPageShell className="relative">
      <div className="space-y-8">
        <FormPageHeader
          icon={<FileSignature className="h-6 w-6" />}
          title="プロジェクト参加申請"
          description={`プロジェクト名：${selectedProject.projectName}`}
        />

        <div className="space-y-6">
          {!isCompletionStep && (
            <WizardProgress
              currentStep={currentStep}
              steps={PROJECT_REGISTRATION_STEPS}
            />
          )}

          <div className="mx-auto w-full animate-slide-up">{renderStep()}</div>

        </div>
      </div>
    </FormPageShell>
  );
};

export default ProjectRegister;
