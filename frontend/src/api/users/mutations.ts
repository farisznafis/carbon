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
  siteId: number | null;
};

type UpdateUserInput = Partial<UserFormData> & {
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
  siteId: number | null;
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

export const registerUser = async (
  userData: UserFormData,
): Promise<{ statusCode: string; user: UserList; site: Partial<Site> }> => {
  const payload: RegisterUserPayload = {
    username: userData.username,
    firstName: userData.firstName,
    firstNameKana: userData.firstNameKana,
    lastName: userData.lastName,
    lastNameKana: userData.lastNameKana,
    email: userData.email,
    phoneNumber: String(userData.phoneNumber ?? "").replace(/-/g, ""),
    userPosition: userData.userPosition ?? "",
    password: userData.password ?? "",
    siteId: userData.siteId ? Number(userData.siteId) : null,
  };

  const response = await apiFetch("/user", {
    method: "POST",
    body: payload,
  });
  const { data: json } = await parseJsonResponse<UserMutationResponse>(response);

  if (!response.ok) {
    throw new Error(formatMutationError(json, "Failed to register user"));
  }

  return json as { statusCode: string; user: UserList; site: Partial<Site> };
};

export const updateUser = async (
  userId: string,
  userData: UpdateUserInput,
  mergedData?: UserList,
  currSite?: Site,
  prevSite?: Site,
  masterUser?: UserList,
): Promise<UpdateUserResponse> => {
  const resolvedSiteId = userData.siteId ?? mergedData?.siteId;

  if (!resolvedSiteId) {
    throw new Error("siteId is required to update user");
  }

  const payload: UpdateUserPayload = {
    userId: Number(userId),
    firstName: userData.firstName ?? mergedData?.firstName ?? "",
    firstNameKana: userData.firstNameKana ?? mergedData?.firstNameKana ?? "",
    lastName: userData.lastName ?? mergedData?.lastName ?? "",
    lastNameKana: userData.lastNameKana ?? mergedData?.lastNameKana ?? "",
    email: userData.email ?? mergedData?.email ?? "",
    phoneNumber: String(userData.phoneNumber ?? mergedData?.phoneNumber ?? "").replace(/-/g, ""),
    userPosition: userData.userPosition ?? userData.position ?? mergedData?.position ?? "",
    siteId: Number(resolvedSiteId),
  };

  const response = await apiFetch("/user", {
    method: "PATCH",
    body: payload,
  });
  const { data: json } = await parseJsonResponse<UserMutationResponse>(response);

  if (!response.ok) {
    throw new Error(formatMutationError(json, "Failed to update user"));
  }

  const updatedUser: UserList =
    json?.user ?? {
      id: String(userId),
      firstName: payload.firstName,
      firstNameKana: payload.firstNameKana,
      lastName: payload.lastName,
      lastNameKana: payload.lastNameKana,
      email: payload.email,
      phoneNumber: payload.phoneNumber,
      role: mergedData?.role ?? "user",
      siteId: String(payload.siteId),
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
