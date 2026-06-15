import { useCallback, useEffect, useRef, useState } from "react";
import { useParams, useNavigate, useLocation } from "react-router-dom";
import { useDashboardStore } from "@/stores/dashboardStore";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Progress } from "@/components/ui/progress";
import { ArrowLeft, ArrowRight } from "lucide-react";
import { useAuth } from "@/contexts/AuthContext";
import { useToast } from "@/hooks/use-toast";
import { getProjectDetails } from "@/api/projects";
import { EquipmentStep } from "@/features/user/project-registration/components/EquipmentStep";
import { DynamicAgreementsStep, type DynamicAgreementsStepRef } from "@/features/user/project-registration/components/DynamicAgreementsStep";
import { ContractStep, ContractStepRef, ContractStepFormValues } from "@/features/user/project-registration/components/ContractStep";
import { FinalTermsStep, FinalTermsRef, FinalTermsValues } from "@/features/user/project-registration/components/FinalTermsStep";
import { CompletionStep } from "@/features/user/project-registration/components/CompletionStep";
import type { Equipment } from "@/features/user/project-registration/components/types";
import type { AgreementFormValues } from "@/features/user/project-registration/components/agreementsConfig";
import { PageBackButton } from "@/components/navigation/PageBackButton";

const ProjectRegister = () => {
  const { id: projectId } = useParams();
  const navigate = useNavigate();
  const { user } = useAuth();
  const { toast } = useToast();
  const location = useLocation();
  const siteId = location.state?.siteId;
  const currentProject = useDashboardStore(state => state.currentProject);
  const setCurrentProject = useDashboardStore(state => state.setCurrentProject);
  const agreementsStepRef = useRef<DynamicAgreementsStepRef>(null);
  const contractStepRef = useRef<ContractStepRef>(null);
  const finalStepRef = useRef<FinalTermsRef>(null);

  const [currentStep, setCurrentStep] = useState(1);
  const [equipment, setEquipment] = useState<Equipment[]>([]);
 

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
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isProjectLoading, setIsProjectLoading] = useState(false);
  const [projectLoadError, setProjectLoadError] = useState<string | null>(null);

  const totalSteps = 5;
  const progress = (currentStep / totalSteps) * 100;
  const selectedProject = projectId && currentProject?.id === projectId ? currentProject : null;
  const selectedProjectId = selectedProject?.id;
  const resolvedSiteId = siteId || user?.siteId || "";

  const navigateToDashboard = useCallback(() => {
    if (user?.role === 'master') {
      navigate("/master/dashboard");
    } else if (user?.siteId) {
      navigate(`/sites/${user.siteId}/dashboard`);
    } else {
      navigate("/");
    }
  }, [navigate, user?.role, user?.siteId]);

  useEffect(() => {
    if (!projectId || selectedProjectId) return;

    let isMounted = true;

    const loadProject = async () => {
      setIsProjectLoading(true);
      setProjectLoadError(null);

      try {
        const project = await getProjectDetails(projectId);
        if (!isMounted) return;
        setCurrentProject(project);
      } catch (error) {
        if (!isMounted) return;
        setProjectLoadError("プロジェクト情報の取得に失敗しました");
        toast({
          title: "エラー",
          description: "プロジェクト情報の取得に失敗しました",
          variant: "destructive",
        });
      } finally {
        if (isMounted) {
          setIsProjectLoading(false);
        }
      }
    };

    loadProject();

    return () => {
      isMounted = false;
    };
  }, [projectId, selectedProjectId, setCurrentProject, toast]);

  useEffect(() => {
    // Load equipment from localStorage when returning from equipment form
    const loadEquipment = () => {
      const storedEquipment = localStorage.getItem(`project_${projectId}_equipment`);
      if (storedEquipment) {
        setEquipment(JSON.parse(storedEquipment));
      }
    };

    loadEquipment();

    // Add event listener to reload equipment when window regains focus
    window.addEventListener('focus', loadEquipment);
    return () => window.removeEventListener('focus', loadEquipment);
  }, [projectId]);


  const handleRemoveEquipment = (id: string) => { //move this to file edit?
    const updatedEquipment = equipment.filter((e) => e.id !== id);
    setEquipment(updatedEquipment);
    // Also update localStorage
    localStorage.setItem(`project_${projectId}_equipment`, JSON.stringify(updatedEquipment));
    toast({
      title: "削除完了",
      description: "設備を削除しました",
    });
  };

  const validateStep = async () => { 
    const validateEquipment = (): boolean => {
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
          if (!item.solarData || !item.solarData.equipmentOpDate) {
            toast({
              title: "入力エラー",
              description: "太陽光設備の運転開始日を入力してください",
              variant: "destructive",
            });
            return false;
          }
          if (!item.solarData.nominalMaxOutput) {
            toast({
              title: "入力エラー",
              description: "太陽光設備の公称最大出力を入力してください",
              variant: "destructive",
            });
            return false;
          }
          if (!item.solarData.totalInvestment) {
            toast({
              title: "入力エラー",
              description: "総投資額を入力してください",
              variant: "destructive",
            });
            return false;
          }
        } else if (item.formType === "other") {
          const missing =
            !item.oldEquipmentName ||
            !item.oldEquipmentId ||
            !item.oldEquipmentType ||
            !item.newEquipmentName ||
            !item.newEquipmentId ||
            !item.newEquipmentType;
          if (missing) {
            toast({
              title: "入力エラー",
              description: "全ての設備情報を入力してください",
              variant: "destructive",
            });
            return false;
          }
        }
      }

      return true;
    };

    const validateAgreements = async (): Promise<boolean> => {
      if (agreementsStepRef.current) {
        return await agreementsStepRef.current.triggerValidation();
      }
      setAgreementErrors({});
      return false;
    };

    const validateContract = async (): Promise<boolean> => {
      if (contractStepRef.current) {
        return await contractStepRef.current.triggerValidation();
      }
      setContractErrors({});
      return false;
    };

    const validateFinalTerms = async (): Promise<boolean> => {
      if (finalStepRef.current) {
        return await finalStepRef.current.triggerValidation();
      }
      setFinalTermsErrors({});
      return false;
    };

    switch (currentStep) {
      case 1:
        return validateEquipment();

      case 2:
        return await validateAgreements();

      case 3:
        return validateContract();

      case 4:
        return validateFinalTerms();

      default:
        return true;
    }
  };

  const handleNext = async () => {
    window.scrollTo({ top: 0, behavior: "smooth" }); // scroll to top
    if (await validateStep()) {
      if (currentStep === 4) {
        handleSubmit();
      } else {
        setCurrentStep(currentStep + 1);
      }
    }
  };

  const handleSubmit = async () => {
    setIsSubmitting(true);
    try {
      // Mock API call
      
      await new Promise((resolve) => setTimeout(resolve, 1500));

      const formData = new FormData();
      formData.append('siteId', resolvedSiteId);
      formData.append('projectId', projectId || '');
      equipment.forEach((item, index) => { //fix equipment Data append
        formData.append(`equipment[${index}]`, JSON.stringify({
          ...item,
        }));
      });
      formData.append('agreements', JSON.stringify(agreementFormValues));
      formData.append('contractAgreed', JSON.stringify(contractFormValues));
      formData.append('finalTermsAgreed', String(finalTermsValues.finalTermsAgreed));
      // Save the applied project to localStorage with pending status
      //const projectId = globalThis.location.pathname.split("/")[2]; // Get project ID from URL
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

      setCurrentStep(5); // Show completion message

      toast({
        title: "申請完了",
        description: "プロジェクト参加申請が完了しました",
      });

      // Navigate to appropriate dashboard after 2 seconds
      setTimeout(() => {
        navigateToDashboard();
      }, 2000);
    } catch (error) {
      toast({
        title: "エラー",
        description: "申請の送信に失敗しました",
        variant: "destructive",
      });
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleNavigateToDashboard = () => {
    navigateToDashboard();
  };

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
        return (
          <CompletionStep
            onNavigateToDashboard={handleNavigateToDashboard}
          />
        );

      default:
        return null;
    }
  };

  if (isProjectLoading) {
    return (
      <div className="min-h-screen bg-background py-8 flex items-center justify-center">
        <p className="text-muted-foreground text-lg">プロジェクト情報を読み込み中...</p>
      </div>
    );
  }

  if (!selectedProject) {
    return (
      <div className="min-h-screen bg-background py-8 flex items-center justify-center">
        <div className="text-center space-y-4">
          <p className="text-muted-foreground text-lg">
            {projectLoadError || "プロジェクト情報が見つかりません"}
          </p>
          <Button onClick={navigateToDashboard}>
            ダッシュボードに戻る
          </Button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-background py-8">
      <div className="bg-primary/50 text-primary-foreground py-6 px-4 mb-4">
        <div className="max-w-md mx-auto">
          <h3 className="text-3xl font-bold text-center">プロジェクト参加申請</h3>
        </div>
      </div>
      <h4 className="text-center text-muted-foreground">プロジェクト名：{selectedProject.projectName}</h4>
      <div className="container mx-auto max-w-4xl p-6 space-y-6">
        {currentStep < 5 && (
          <>
            <PageBackButton
              onClick={() => (currentStep === 1 ? navigateToDashboard() : setCurrentStep(currentStep - 1))}
              disabled={isSubmitting}
              label={currentStep === 1 ? "ダッシュボードに戻る" : "前へ"}
              className='rounded-full'
            />
            <Button
              variant="ghost"
              onClick={() => (currentStep === 1 ? navigateToDashboard() : setCurrentStep(currentStep - 1))}
              disabled={isSubmitting}
              className="hidden"
            >
              <ArrowLeft className="h-4 w-4 mr-2" />
              {currentStep === 1 ? "ダッシュボードに戻る" : "前へ"}
            </Button>

            <div className="space-y-2">
              <div className="flex items-center justify-between text-sm">
                <span className="text-muted-foreground">
                  ステップ {currentStep} / {totalSteps - 1}
                </span>
                <span className="font-medium">{Math.round(progress)}%</span>
              </div>
              <Progress value={progress} />
            </div>
          </>
        )}

        <Card className="border-hidden w-full mx-auto p-4">
          <CardContent className="pt-6">{renderStep()}</CardContent>
        </Card>

        {currentStep < 6 && (
          <div className="flex justify-end">
            <Button onClick={handleNext} disabled={isSubmitting}>
              {isSubmitting ? "送信中..." : currentStep === 5 ? "送信" : "次へ"}
              {currentStep < 5 && <ArrowRight className="h-4 w-4 ml-2" />}
            </Button>
          </div>
        )}
      </div>
    </div>
  );
}

export default ProjectRegister;
