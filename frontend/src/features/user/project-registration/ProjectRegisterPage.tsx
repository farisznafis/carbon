import { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { useDashboardStore } from "@/stores/dashboardStore";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { ArrowRight } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { getProjectDetails } from "@/api/projects";
import { EquipmentStep } from "@/features/user/project-registration/components/EquipmentStep";
import { DynamicAgreementsStep, type DynamicAgreementsStepRef } from "@/features/user/project-registration/components/DynamicAgreementsStep";
import { ContractStep, type ContractStepRef, type ContractStepFormValues } from "@/features/user/project-registration/components/ContractStep";
import { FinalTermsStep, type FinalTermsRef, type FinalTermsValues } from "@/features/user/project-registration/components/FinalTermsStep";
import { CompletionStep } from "@/features/user/project-registration/components/CompletionStep";
import { FormPageWrapper } from "@/components/layout/FormPageWrapper/FormPageWrapper";
import type { Equipment } from "@/features/user/project-registration/components/types";
import type { AgreementFormValues } from "@/features/user/project-registration/components/agreementsConfig";

// ─── Constants ───────────────────────────────────────────────────────────────

const TOTAL_STEPS = 5;

// ─── Component ───────────────────────────────────────────────────────────────

const ProjectRegister = () => {
  const { id: projectId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast } = useToast();
  const location = useLocation();

  const siteId = location.state?.siteId;
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
    useRegisteredInfo: false,
    provideAdditionalInfo: false,
    transferEnvironmentalValue: false,
    notRegisteredElsewhere: false,
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
  const progress = (currentStep / TOTAL_STEPS) * 100;
  const selectedProject = projectId && currentProject?.id === projectId ? currentProject : null;
  const resolvedSiteId = siteId || user?.siteId || "";
  const isCompletionStep = currentStep === TOTAL_STEPS;

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

  const handleBack = useCallback(() => {
    if (currentStep === 1) {
      navigateToDashboard();
    } else {
      setCurrentStep((prev) => prev - 1);
    }
  }, [currentStep, navigateToDashboard]);

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
      if (stored) setEquipment(JSON.parse(stored));
    };

    loadEquipment();
    window.addEventListener('focus', loadEquipment);
    return () => window.removeEventListener('focus', loadEquipment);
  }, [projectId]);

  // ── Handlers ──────────────────────────────────────────────────────────────
  const handleRemoveEquipment = (id: string) => {
    const updated = equipment.filter((e) => e.id !== id);
    setEquipment(updated);
    localStorage.setItem(`project_${projectId}_equipment`, JSON.stringify(updated));
    toast({ title: "削除完了", description: "設備を削除しました" });
  };

  const validateStep = async (): Promise<boolean> => {
    switch (currentStep) {
      case 1: {
        if (equipment.length === 0) {
          toast({
            title: "入力エラー",
            description: "設備が登録されていません。設備を追加してください。",
            variant: "destructive",
          });
          return false;
        }

        for (const item of equipment) {
          if (item.formType === "solar") {
            if (!item.solarData?.equipmentOpDate) {
              toast({ title: "入力エラー", description: "太陽光設備の運転開始日を入力してください", variant: "destructive" });
              return false;
            }
            if (!item.solarData?.nominalMaxOutput) {
              toast({ title: "入力エラー", description: "太陽光設備の公称最大出力を入力してください", variant: "destructive" });
              return false;
            }
            if (!item.solarData?.totalInvestment) {
              toast({ title: "入力エラー", description: "総投資額を入力してください", variant: "destructive" });
              return false;
            }
          } else if (item.formType === "other") {
            const missing =
              !item.oldEquipmentName || !item.oldEquipmentId || !item.oldEquipmentType ||
              !item.newEquipmentName || !item.newEquipmentId || !item.newEquipmentType;
            if (missing) {
              toast({ title: "入力エラー", description: "全ての設備情報を入力してください", variant: "destructive" });
              return false;
            }
          }
        }
        return true;
      }

      case 2:
        if (agreementsStepRef.current) return await agreementsStepRef.current.triggerValidation();
        setAgreementErrors({});
        return false;

      case 3:
        if (contractStepRef.current) return await contractStepRef.current.triggerValidation();
        setContractErrors({});
        return false;

      case 4:
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
      await new Promise((resolve) => setTimeout(resolve, 1500));

      const formData = new FormData();
      formData.append('siteId', resolvedSiteId);
      formData.append('projectId', projectId || '');
      equipment.forEach((item, index) => {
        formData.append(`equipment[${index}]`, JSON.stringify(item));
      });
      formData.append('agreements', JSON.stringify(agreementFormValues));
      formData.append('contractAgreed', JSON.stringify(contractFormValues));
      formData.append('finalTermsAgreed', String(finalTermsValues.finalTermsAgreed));

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

      setCurrentStep(TOTAL_STEPS);
      toast({ title: "申請完了", description: "プロジェクト参加申請が完了しました" });
      setTimeout(() => navigateToDashboard(), 2000);
    } catch {
      toast({ title: "エラー", description: "申請の送信に失敗しました", variant: "destructive" });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleNext = async () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
    if (await validateStep()) {
      if (currentStep === 4) {
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
          <EquipmentStep
            equipment={equipment}
            onRemoveEquipment={handleRemoveEquipment}
          />
        );
      case 2:
        return (
          <DynamicAgreementsStep
            ref={agreementsStepRef}
            formValues={agreementFormValues}
            onValueChange={setAgreementFormValues}
            errors={agreementErrors}
          />
        );
      case 3:
        return (
          <ContractStep
            ref={contractStepRef}
            formValues={contractFormValues}
            onValueChange={setContractFormValues}
            errors={contractErrors}
          />
        );
      case 4:
        return (
          <FinalTermsStep
            ref={finalStepRef}
            formValues={finalTermsValues}
            onValueChange={setFinalTermsValues}
            errors={finalTermsErrors}
          />
        );
      case 5:
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
            <Button onClick={navigateToDashboard}>ダッシュボードに戻る</Button>
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
      backText={currentStep === 1 ? "ダッシュボードに戻る" : "前へ"}
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
                ステップ {currentStep} / {TOTAL_STEPS - 1}
              </span>
              <span className="font-medium">{Math.round(progress)}%</span>
            </div>
            <Progress value={progress} />
          </div>
        )}

        {/* Step content */}
        <Card className="border-hidden w-full mx-auto p-4">
          <CardContent className="pt-6">{renderStep()}</CardContent>
        </Card>

        {/* Next button — hidden on completion step */}
        {!isCompletionStep && (
          <div className="flex justify-end">
            <Button onClick={handleNext} disabled={isSubmitting}>
              {isSubmitting
                ? "送信中..."
                : currentStep === 4
                  ? "送信"
                  : "次へ"}
              {currentStep < 4 && <ArrowRight className="h-4 w-4 ml-2" />}
            </Button>
          </div>
        )}

      </div>
    </FormPageWrapper>
  );
};

export default ProjectRegister;