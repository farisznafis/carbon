import { apiFetch, apiVoidRequest, parseJsonResponse } from "@/api/_shared/httpClient";
import type { Site } from "@/api/site";
import type { UserFormData, UserList } from "./types";

type RegisterUserPayload = {
  username: string;
  firstName: string;
  firstNameKana: string;
  lastName: string;
  lastNameKana: string;
  email: string;
  phoneNumber: string;
  userPosition: string;
  password: string;
  siteId: string | null;
};

type UpdateUserInput = Omit<Partial<UserFormData>, "siteId"> & {
  siteId?: string | null;
  position?: string;
};

type UpdateUserPayload = {
  userId: number;
  firstName: string;
  firstNameKana: string;
  lastName: string;
  lastNameKana: string;
  email: string;
  phoneNumber: string;
  userPosition: string;
  siteId: string | null;
};

type UpdateUserResponse = {
  statusCode: string;
  user: UserList;
  updatedSites?: Site[];
};

type UserMutationResponse = Partial<UpdateUserResponse> & {
  site?: Partial<Site>;
  errors?: Record<string, string>;
  message?: string;
};

function formatMutationError(
  json: UserMutationResponse | null,
  fallbackMessage: string,
): string {
  return json?.errors
    ? Object.values(json.errors).join("\n")
    : json?.message || fallbackMessage;
}

function mapRegisterUserPayload(userData: UserFormData): RegisterUserPayload {
  return {
    username: userData.username,
    firstName: userData.firstName,
    firstNameKana: userData.firstNameKana,
    lastName: userData.lastName,
    lastNameKana: userData.lastNameKana,
    email: userData.email,
    phoneNumber: String(userData.phoneNumber ?? "").replace(/-/g, ""),
    userPosition: userData.userPosition ?? "",
    password: userData.password ?? "",
    siteId: userData.siteId || null,
  };
}

function mapUpdateUserPayload(
  userId: string,
  userData: UpdateUserInput,
  mergedData?: UserList,
): UpdateUserPayload {
  const resolvedSiteId =
    userData.siteId !== undefined ? userData.siteId : mergedData?.siteId ?? null;
  const normalizedSiteId =
    resolvedSiteId === '' || resolvedSiteId === null ? null : resolvedSiteId;

  return {
    userId: Number(userId),
    firstName: userData.firstName ?? mergedData?.firstName ?? "",
    firstNameKana: userData.firstNameKana ?? mergedData?.firstNameKana ?? "",
    lastName: userData.lastName ?? mergedData?.lastName ?? "",
    lastNameKana: userData.lastNameKana ?? mergedData?.lastNameKana ?? "",
    email: userData.email ?? mergedData?.email ?? "",
    phoneNumber: String(userData.phoneNumber ?? mergedData?.phoneNumber ?? "").replace(/-/g, ""),
    userPosition: userData.userPosition ?? userData.position ?? mergedData?.position ?? "",
    siteId: normalizedSiteId,
  };
}

export const validateCreateUser = async (userData: UserFormData) => {
  await apiVoidRequest("/user/validate-create", {
    method: "POST",
    body: mapRegisterUserPayload(userData),
    fallbackErrorMessage: "ユーザー登録内容の検証に失敗しました",
  });
};

export const validateUpdateUser = async (
  userId: string,
  userData: UpdateUserInput,
  mergedData?: UserList,
) => {
  await apiVoidRequest("/user/validate-update", {
    method: "POST",
    body: mapUpdateUserPayload(userId, userData, mergedData),
    fallbackErrorMessage: "ユーザー更新内容の検証に失敗しました",
  });
};

export const registerUser = async (
  userData: UserFormData,
): Promise<{ statusCode: string; user: UserList; site?: Partial<Site> }> => {
  const payload = mapRegisterUserPayload(userData);

  const response = await apiFetch("/user", {
    method: "POST",
    body: payload,
  });
  const { data: json } = await parseJsonResponse<UserMutationResponse>(response);

  if (!response.ok) {
    throw new Error(formatMutationError(json, "Failed to register user"));
  }

  return json as { statusCode: string; user: UserList; site?: Partial<Site> };
};

export const updateUser = async (
  userId: string,
  userData: UpdateUserInput,
  mergedData?: UserList,
  currSite?: Site,
  prevSite?: Site,
  masterUser?: UserList,
): Promise<UpdateUserResponse> => {
  const payload = mapUpdateUserPayload(userId, userData, mergedData);

  const response = await apiFetch("/user", {
    method: "PATCH",
    body: payload,
  });
  const { data: json } = await parseJsonResponse<UserMutationResponse>(response);

  if (!response.ok) {
    throw new Error(formatMutationError(json, "Failed to update user"));
  }

  const updatedUser: UserList =
    json?.user
      ? {
          ...json.user,
          position: json.user.position, // ✅ normalize API response
        }
      : {
          id: String(userId),
          firstName: payload.firstName,
          firstNameKana: payload.firstNameKana,
          lastName: payload.lastName,
          lastNameKana: payload.lastNameKana,
          email: payload.email,
          phoneNumber: payload.phoneNumber,
          role: mergedData?.role ?? "user",
           siteId: payload.siteId,
          position: payload.userPosition,
        };

  const updatedSites =
    currSite && prevSite && masterUser && mergedData
      ? [
          {
            ...currSite,
            picFirstName: updatedUser.firstName,
            picLastName: updatedUser.lastName,
            picPosition: updatedUser.position,
            picEmail: updatedUser.email,
            picPhoneNumber: updatedUser.phoneNumber,
          },
          {
            ...prevSite,
            picFirstName: masterUser.firstName,
            picLastName: masterUser.lastName,
            picPosition: masterUser.position,
            picEmail: masterUser.email,
            picPhoneNumber: masterUser.phoneNumber,
          },
        ]
      : undefined;

  return {
    statusCode: json?.statusCode ?? "OK",
    user: updatedUser,
    updatedSites,
  };
};

export const deleteUser = async (userId: string, site?: Site) => {
  await apiVoidRequest(`/user/${encodeURIComponent(userId)}`, {
    method: "DELETE",
    fallbackErrorMessage: "Failed to delete user",
  });

  return { statusCode: "OK", userId, site };
};
