import { useNavigate } from "react-router-dom";

import { UserForm, UserFormData } from "@/features/master/forms/UserForm";
import { useDashboardStore } from "@/stores/dashboardStore";
import { useEffect, useState } from "react";
import { registerUser } from "@/api/users/mutations";
import { toast } from "@/hooks/use-toast";
import { userSchema } from "@/lib/validations";
import UserConfirmation from "./UserConfirmation";
import { logger } from "@/lib/logger";
import { useSitesQuery } from "@/hooks/sites/useSitesQuery";

const emptyUserValues = {
  username: "",
  lastName: "",
  firstName: "",
  lastNameKana: "",
  firstNameKana: "",
  userPosition: "",
  phoneNumber: "",
  siteId: "",
  email: "",
  password: "",
};

export default function UserRegister() {
  const [step, setStep] = useState<"form" | "confirm">("form");
  const navigate = useNavigate();
  const { addUser, editSite, setSites } = useDashboardStore();
  const [formData, setFormData] = useState<UserFormData>();
  const sitesQuery = useSitesQuery();

  useEffect(() => {
    if (sitesQuery.data?.sites) {
      setSites(sitesQuery.data.sites);
    }
  }, [setSites, sitesQuery.data?.sites]);

  // Called when the form is submitted
  const handleDraftSubmit = () => {
    setStep("confirm");
  };

  const handleFinalSubmit = async () => {
    if (!formData) return;

    try {
      const res = await registerUser(formData); //should put together in one call in dashboardStore
      addUser(res.user);
      editSite(res.site.id, res.site);

      toast({
        title: "成功",
        description: "ユーザを登録しました",
      });

      navigate("/master/dashboard");
    } catch (err) {
      logger.error("Error creating user:", err); //add toast
    }
  };

  const handleConfirmBack = () => {
    setStep("form");
  };

  const handleFormBack = () => {
    navigate(-1);
  };

  return (
    <>
      {step === "form" && (
        <UserForm
          mode="create"
          onChange={setFormData}
          onNext={handleDraftSubmit}
          onBack={handleFormBack}
          defaultValues={formData || emptyUserValues}
          originalData={emptyUserValues}
          schema={userSchema}
          siteOptions={sitesQuery.data?.sites ?? []}
          isSiteOptionsLoading={sitesQuery.isLoading}
          siteOptionsError={sitesQuery.isError ? "拠点を取得できませんでした" : null}
        />
      )}

      {step === "confirm" && (
        <UserConfirmation
          data={formData}
          originalData={emptyUserValues}
          onBack={handleConfirmBack}
          onSubmit={handleFinalSubmit}
        />
      )}
    </>
  );
}
