export type RegistrationStatus = "ACTIVE" | "APPROVED" | "REJECTED" | "PENDING";

export type RegistrationHistoryItem = {
  id: string;
  projectName: string;
  siteName: string;
  dateTime: string;
  status: RegistrationStatus;
  cancelReason?: string;
};

export type ProjectDetailMock = {
  name: string;
  description: string;
  methodology: string;
  status: "active" | "inactive";
  createdDate: string;
  location: string;
  devices: Array<{
    id: string;
    name: string;
    type: string;
    status: "online" | "offline";
  }>;
  recentData: Array<{
    date: string;
    parameter: string;
    value: string;
    status: "compliant" | "non-compliant";
  }>;
  documents: Array<{
    id: string;
    name: string;
    type: string;
    uploadDate: string;
    size: string;
  }>;
};

export const mockProjectData: Record<string, ProjectDetailMock> = {
  "1": {
    name: "Factory Site A - Air Quality",
    description:
      "Continuous monitoring of air quality parameters including PM2.5, PM10, CO2, and NOx levels",
    methodology: "EPA Compliance",
    status: "active",
    createdDate: "2023-09-01",
    location: "123 Industrial Park, Manufacturing District",
    devices: [
      {
        id: "D001",
        name: "Main Air Monitor",
        type: "Air Quality Sensor",
        status: "online",
      },
      {
        id: "D002",
        name: "Backup Sensor",
        type: "Air Quality Sensor",
        status: "online",
      },
    ],
    recentData: [
      {
        date: "2024-01-15",
        parameter: "PM2.5",
        value: "12.5 µg/m³",
        status: "compliant",
      },
    ],
    documents: [
      {
        id: "DOC001",
        name: "環境影響評価報告書.pdf",
        type: "PDF",
        uploadDate: "2024-01-10",
        size: "2.4 MB",
      },
      {
        id: "DOC002",
        name: "モニタリング計画書.pdf",
        type: "PDF",
        uploadDate: "2024-01-08",
        size: "1.8 MB",
      },
      {
        id: "DOC003",
        name: "測定データ_2024年1月.xlsx",
        type: "Excel",
        uploadDate: "2024-01-15",
        size: "856 KB",
      },
    ],
  },
};

export const mockRegistrationHistory: RegistrationHistoryItem[] = Array.from(
  { length: 50 },
  (_, index) => {
    const day = String((index % 28) + 1).padStart(2, "0");
    const status =
      index % 4 === 0
        ? "ACTIVE"
        : index % 4 === 1
          ? "PENDING"
          : index % 4 === 2
            ? "APPROVED"
            : "REJECTED";

    return {
      id: `RH-${String(index + 1).padStart(3, "0")}`,
      projectName: "Factory Site A - Air Quality",
      siteName: `Project 1 Site ${index + 1}`,
      dateTime: `2026/04/${day} 10:05:12`,
      status,
      cancelReason: status === "ACTIVE" ? undefined : "Mock reason for pagination test",
    };
  },
);