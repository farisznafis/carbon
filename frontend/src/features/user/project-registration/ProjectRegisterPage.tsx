import { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useLocation, useNavigate, useParams } from "react-router-dom";
import { useDashboardStore } from "@/stores/dashboardStore";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { ArrowRight } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { getProjectDetails } from "@/api/projects";
import {
  joinSiteProject,
  uploadSiteProjectEquipmentFile,
  validateSiteProjectConfirmationItems,
} from "@/api/site";
import { ApiError } from "@/api/_shared/errors";
import { DynamicAgreementsStep, type DynamicAgreementsStepRef } from "@/features/user/project-registration/components/DynamicAgreementsStep";
import { ContractStep, type ContractStepRef, type ContractStepFormValues } from "@/features/user/project-registration/components/ContractStep";
import { FinalTermsStep, type FinalTermsRef, type FinalTermsValues } from "@/features/user/project-registration/components/FinalTermsStep";
import { CompletionStep } from "@/features/user/project-registration/components/CompletionStep";
import { FormPageWrapper } from "@/components/layout/FormPageWrapper/FormPageWrapper";
import type { Equipment } from "@/features/user/project-registration/components/types";
import type { AgreementFormValues } from "@/features/user/project-registration/components/agreementsConfig";
import { paths } from "@/routes/paths";
import {
  isFromMasterDashboard,
  masterDashboardProjectMembershipState,
} from "@/features/user/project-registration/projectMembershipNavigation";
import {
  mapConfirmationItemsValidationRequest,
  mapProjectJoinRequest,
} from "@/features/user/project-registration/projectJoinMapper";
import { getPendingSolarEquipment } from "@/features/user/project-registration/equipmentConfirmationState";
import { getSolarFormSelectedFiles } from "@/features/user/project-registration/equipmentValidationMapper";

// ─── Constants ───────────────────────────────────────────────────────────────

const TOTAL_FORM_STEPS = 3;
const FINAL_FORM_STEP = 3;
const COMPLETION_STEP = 4;

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
    () => getPendingSolarEquipment(location.state),
    [location.state],
  );
  const { user } = useAuth();
  const { toast } = useToast();
  const currentProject = useDashboardStore((state) => state.currentProject);
  const setCurrentProject = useDashboardStore((state) => state.setCurrentProject);

  // ── Refs ──────────────────────────────────────────────────────────────────
  const agreementsStepRef = useRef<DynamicAgreementsStepRef>(null);
  const contractStepRef = useRef<ContractStepRef>(null);
  const finalStepRef = useRef<FinalTermsRef>(null);

  // ── State ─────────────────────────────────────────────────────────────────
  const [currentStep, setCurrentStep] = useState(1);
  const [equipment, setEquipment] = useState<Equipment[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isProjectLoading, setIsProjectLoading] = useState(false);
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

  const [finalTermsValues, setFinalTermsValues] = useState<Partial<FinalTermsValues>>({
    finalTermsAgreed: false,
  });

  const [agreementErrors, setAgreementErrors] = useState<Record<string, string>>({});
  const [contractErrors, setContractErrors] = useState<Record<string, string>>({});
  const [finalTermsErrors, setFinalTermsErrors] = useState<Record<string, string>>({});

  // ── Derived ───────────────────────────────────────────────────────────────
  const progress = (currentStep / TOTAL_FORM_STEPS) * 100;
  const selectedProject = projectId && currentProject?.id === projectId ? currentProject : null;
  const resolvedSiteId = siteId;
  const isCompletionStep = currentStep === COMPLETION_STEP;

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
    const loadEquipment = () => {
      const stored = localStorage.getItem(`project_${projectId}_equipment`);
      const storedEquipment: Equipment[] = stored ? JSON.parse(stored) : [];
      if (!pendingEquipment) {
        setEquipment(storedEquipment);
        return;
      }

      const equipmentExists = storedEquipment.some(
        (item) => item.id === pendingEquipment.id,
      );
      setEquipment(
        equipmentExists
          ? storedEquipment.map((item) =>
              item.id === pendingEquipment.id ? pendingEquipment : item,
            )
          : [...storedEquipment, pendingEquipment],
      );
    };

    loadEquipment();
    window.addEventListener('focus', loadEquipment);
    return () => window.removeEventListener('focus', loadEquipment);
  }, [pendingEquipment, projectId]);

  // ── Handlers ──────────────────────────────────────────────────────────────
  const validateStep = async (): Promise<boolean> => {
    switch (currentStep) {
      case 1:
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

      case 2:
        if (contractStepRef.current) return await contractStepRef.current.triggerValidation();
        setContractErrors({});
        return false;

      case 3:
        if (finalStepRef.current) return await finalStepRef.current.triggerValidation();
        setFinalTermsErrors({});
        return false;

      default:
        return true;
    }
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    try {
      const solarEquipment = equipment.find(
        (item): item is Extract<Equipment, { formType: 'solar' }> =>
          item.formType === 'solar',
      );
      if (!solarEquipment || !projectId) {
        throw new Error('登録する太陽光発電設備が見つかりません。');
      }

      const uploadedFiles =
        (solarEquipment.files?.length ?? 0) > 0
          ? solarEquipment.files
          : await Promise.all(
              getSolarFormSelectedFiles(solarEquipment.solarData).map(
                ({ fileType, file }) =>
                  uploadSiteProjectEquipmentFile(
                    solarEquipment.solarData.siteId,
                    projectId,
                    fileType,
                    file,
                  ),
              ),
            );
      const equipmentWithUploadedFiles = {
        ...solarEquipment,
        files: uploadedFiles,
      };
      const updatedEquipment = equipment.map((item) =>
        item.id === equipmentWithUploadedFiles.id
          ? equipmentWithUploadedFiles
          : item,
      );
      setEquipment(updatedEquipment);
      localStorage.setItem(
        `project_${projectId}_equipment`,
        JSON.stringify(updatedEquipment),
      );

      const joinPayload = mapProjectJoinRequest(
        equipmentWithUploadedFiles,
        agreementFormValues,
        contractFormValues,
        finalTermsValues,
      );
      await joinSiteProject(projectId, joinPayload);

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
      if (currentStep === FINAL_FORM_STEP) {
        await handleSubmit();
      } else {
        setCurrentStep((prev) => prev + 1);
      }
    }
  };

  // ── Render steps ──────────────────────────────────────────────────────────
  const renderStep = () => {
    switch (currentStep) {
      case 1:
        return (
          <DynamicAgreementsStep
            ref={agreementsStepRef}
            formValues={agreementFormValues}
            onValueChange={(values) => {
              setAgreementFormValues(values);
              if (Object.keys(agreementErrors).length > 0) {
                setAgreementErrors({});
              }
            }}
            errors={agreementErrors}
          />
        );
      case 2:
        return (
          <ContractStep
            ref={contractStepRef}
            formValues={contractFormValues}
            onValueChange={setContractFormValues}
            errors={contractErrors}
          />
        );
      case 3:
        return (
          <FinalTermsStep
            ref={finalStepRef}
            formValues={finalTermsValues}
            onValueChange={setFinalTermsValues}
            errors={finalTermsErrors}
          />
        );
      case 4:
        return <CompletionStep onNavigateToDashboard={navigateToDashboard} />;
      default:
        return null;
    }
  };

  // ── Loading / error states ─────────────────────────────────────────────────
  if (isProjectLoading) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-muted-foreground text-lg">プロジェクト情報を読み込み中...</p>
      </div>
    );
  }

  if (!selectedProject) {
    return (
      <div className="flex min-h-screen items-center justify-center p-6">
        <Card className="w-full max-w-md">
          <CardContent className="pt-6 text-center space-y-4">
            <p className="text-muted-foreground">
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
    <FormPageWrapper
      pageName="プロジェクト参加申請"
      title=""
      onBack={!isCompletionStep ? handleBack : undefined}
      backText={currentStep === 1 ? "入会届に戻る" : "前へ"}
      contentMaxWidth="max-w-4xl"
      wrapInCard={false}
    >
      <div className="space-y-6">

        {/* Project name */}
        <p className="text-center text-muted-foreground">
          プロジェクト名：{selectedProject.projectName}
        </p>

        {/* Progress bar — hidden on completion step */}
        {!isCompletionStep && (
          <div className="space-y-2">
            <div className="flex items-center justify-between text-sm">
              <span className="text-muted-foreground">
                ステップ {currentStep} / {TOTAL_FORM_STEPS}
              </span>
              <span className="font-medium">{Math.round(progress)}%</span>
            </div>
            <Progress value={progress} />
          </div>
        )}

        {/* Step content */}
        <div className="mx-auto w-full">{renderStep()}</div>

        {/* Next button — hidden on completion step */}
        {!isCompletionStep && (
          <div className="flex justify-end">
            <Button onClick={handleNext} disabled={isSubmitting}>
              {isSubmitting
                ? currentStep === 1
                  ? "確認中..."
                  : "送信中..."
                : currentStep === FINAL_FORM_STEP
                  ? "送信"
                  : "次へ"}
              {currentStep < FINAL_FORM_STEP && (
                <ArrowRight className="h-4 w-4 ml-2" />
              )}
            </Button>
          </div>
        )}

      </div>
    </FormPageWrapper>
  );
};

export default ProjectRegister;
