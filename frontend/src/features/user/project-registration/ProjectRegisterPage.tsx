import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useDashboardStore } from "@/stores/dashboardStore";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { PageSkeleton } from "@/components/feedback/loading";
import {
  FormPageHeader,
  FormPageShell,
  WizardProgress,
} from "@/components/forms/form-layout";
import { FileSignature } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { getProjectDetails } from "@/api/projects";
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
import {
  floatingPageBackButtonClassName,
  PageBackButton,
} from "@/components/navigation/PageBackButton";
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
import { useSustainabilityCommitmentDefaultQuery } from "@/hooks/sites/useSustainabilityCommitmentDefaultQuery";
import {
  createProjectRegistrationDraftKey,
  deleteProjectRegistrationDraft,
  loadProjectRegistrationDraft,
  PROJECT_REGISTRATION_DRAFT_VERSION,
  saveProjectRegistrationDraft,
  type ProjectRegistrationDraft,
} from "@/features/user/project-registration/projectRegistrationDraftStorage";
import {
  hasLegacyContractDraftValues,
  mapSustainabilityCommitmentDefault,
} from "@/features/user/project-registration/sustainabilityCommitmentDefaultMapper";

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

const confirmationItemFieldNames: Record<string, keyof AgreementFormValues> = {
  isAgreeDataUse: "useRegisteredInfo",
  isAgreeExtraData: "provideAdditionalInfo",
  isAgreeTransfer: "transferEnvironmentalValue",
  isNotRegistered: "notRegisteredElsewhere",
  isKeidanrenParticipation: "keidanrenParticipation",
  isEmitter: "specifiedEmitterStatus",
  emitterCode: "specifiedEmitterCode",
  isAdjustedEmissionsAdded: "emissionAdditionAgreement",
  isSubjectToPeriodicReport: "energyRationalizationCompliance",
  isPeriodicReportTypeOne: "energyLawType",
  energyBusinessCode: "energyLawBusinessNumber",
  factoryCode: "energyLawFactoryNumber",
};

const sustainabilityCommitmentFieldNames: Record<
  string,
  keyof ContractStepFormValues
> = {
  companyName: "businessName",
  repName: "representativeName",
  repRole: "representativePosition",
  hasConsent: "contractAgreed",
};

const finalAgreementFieldNames: Record<string, keyof FinalTermsValues> = {
  isAgreeTerms: "finalTermsAgreed",
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
  const contractHasUserChangesRef = useRef(false);

  // ── State ─────────────────────────────────────────────────────────────────
  const [currentStep, setCurrentStep] = useState(1);
  const [equipment, setEquipment] = useState<Equipment[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isProjectLoading, setIsProjectLoading] = useState(false);
  const [isDraftHydrated, setIsDraftHydrated] = useState(false);
  const [projectLoadError, setProjectLoadError] = useState<string | null>(null);

  const [agreementFormValues, setAgreementFormValues] = useState<Partial<AgreementFormValues>>({
    useRegisteredInfo: null,
    provideAdditionalInfo: null,
    transferEnvironmentalValue: null,
    notRegisteredElsewhere: null,
    keidanrenParticipation: '',
    specifiedEmitterStatus: '',
    energyRationalizationCompliance: '',
  });

  const [contractFormValues, setContractFormValues] = useState<Partial<ContractStepFormValues>>({
    businessName: '',
    representativePosition: '',
    representativeName: '',
    contractAgreed: false,
  });
  const [contractHasUserChanges, setContractHasUserChanges] = useState(false);

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
  const draftKey = useMemo(
    () => createProjectRegistrationDraftKey(user?.id ?? '', siteId, projectId),
    [projectId, siteId, user?.id],
  );
  const sustainabilityDefaultQuery =
    useSustainabilityCommitmentDefaultQuery(
      selectedEquipmentSiteId,
      isDraftHydrated && !contractHasUserChanges,
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

  const navigateToMembership = useCallback(() => {
    if (resolvedSiteId) {
      navigate(paths.projectMembership(resolvedSiteId), {
        state: enteredFromMasterDashboard
          ? masterDashboardProjectMembershipState
          : undefined,
      });
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
          const restoredContractHasUserChanges =
            savedDraft.contractHasUserChanges ??
            hasLegacyContractDraftValues(savedDraft.contractFormValues);
          setCurrentStep(
            Math.min(Math.max(savedDraft.currentStep, EQUIPMENT_STEP), REVIEW_STEP),
          );
          setAgreementFormValues(savedDraft.agreementFormValues);
          setContractFormValues(savedDraft.contractFormValues);
          setContractHasUserChanges(restoredContractHasUserChanges);
          contractHasUserChangesRef.current = restoredContractHasUserChanges;
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
    if (
      !sustainabilityDefaultQuery.data ||
      contractHasUserChangesRef.current
    ) {
      return;
    }

    setContractFormValues(
      mapSustainabilityCommitmentDefault(sustainabilityDefaultQuery.data),
    );
  }, [sustainabilityDefaultQuery.data]);

  useEffect(() => {
    if (!isDraftHydrated || isCompletionStep) return;

    const draft: ProjectRegistrationDraft = {
      version: PROJECT_REGISTRATION_DRAFT_VERSION,
      currentStep,
      equipment,
      agreementFormValues,
      contractFormValues,
      contractHasUserChanges,
      finalTermsValues,
      updatedAt: new Date().toISOString(),
    };

    void saveProjectRegistrationDraft(draftKey, draft).catch(() => {
      // Saving a draft is best-effort and must not interrupt form input.
    });
  }, [
    agreementFormValues,
    contractFormValues,
    contractHasUserChanges,
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
          toast({
            title: "設備情報を確認できませんでした",
            description:
              error instanceof Error
                ? error.message
                : "設備情報の検証に失敗しました。",
            variant: "destructive",
          });
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
          toast({
            title: "同意事項を確認できませんでした",
            description:
              error instanceof Error
                ? error.message
                : "同意事項の検証に失敗しました。",
            variant: "destructive",
          });
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
          toast({
            title: "持続可能性の同意内容を確認できませんでした",
            description:
              error instanceof Error
                ? error.message
                : "持続可能性約束の検証に失敗しました。",
            variant: "destructive",
          });
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
          toast({
            title: "入会規約を確認できませんでした",
            description:
              error instanceof Error
                ? error.message
                : "入会規約の検証に失敗しました。",
            variant: "destructive",
          });
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
    window.scrollTo({ top: 0, behavior: "smooth" });
    if (await validateStep()) {
      setCurrentStep((prev) => prev + 1);
    }
  };

  // ── Render steps ──────────────────────────────────────────────────────────
  const renderStep = () => {
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
            formValues={contractFormValues}
            isSubmitting={isSubmitting}
            onBack={handleBack}
            onNext={handleNext}
            onValueChange={(values, metadata) => {
              setContractFormValues(values);
              if (metadata.isUserChange) {
                contractHasUserChangesRef.current = true;
                setContractHasUserChanges(true);
              }
              if (Object.keys(contractErrors).length > 0) {
                setContractErrors({});
              }
            }}
            errors={contractErrors}
            isDefaultLoading={
              !contractHasUserChanges && sustainabilityDefaultQuery.isFetching
            }
            defaultLoadError={
              !contractHasUserChanges && sustainabilityDefaultQuery.isError
                ? '登録済み情報を取得できませんでした。手動で入力するか、再試行してください。'
                : null
            }
            onRetryDefaults={() => {
              void sustainabilityDefaultQuery.refetch();
            }}
          />
        );
      case FINAL_AGREEMENT_STEP:
        return (
          <FinalTermsStep
            ref={finalStepRef}
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
      case COMPLETION_STEP:
        return <CompletionStep onNavigateToDashboard={navigateToDashboard} />;
      default:
        return null;
    }
  };

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

  // ── Main render ───────────────────────────────────────────────────────────
  return (
    <FormPageShell className="relative">
      <div className="space-y-8">
        {!isCompletionStep && (
          <PageBackButton
            onClick={handleBack}
            label=""
            aria-label={currentStep === EQUIPMENT_STEP ? "入会届に戻る" : "前へ"}
            className={floatingPageBackButtonClassName}
          />
        )}

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
