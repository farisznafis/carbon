import { Upload } from "lucide-react";

interface FileUploadButtonProps {
  id: string;
  label?: string;
  fileKey: string;
  files: { [key: string]: File | null };
  onFileChange: (key: string, file: File | null) => void;
}

export function FileUploadButton({
  id,
  label = "ファイルを選択",
  fileKey,
  files,
  onFileChange,
}: FileUploadButtonProps) {
  const selectedFile = files[fileKey] || null;

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0] || null;
    onFileChange(fileKey, file);
  };

  return (
    <div className="flex flex-col items-start border-b pb-4 space-y-2">
        <div className="font-bold text-sm">{label}</div>
        <label
            htmlFor={id}
            className="cursor-pointer text-sm text-gray-800 bg-gray-200 hover:bg-gray-300 py-1 px-3 rounded-md shadow-sm flex items-center"
        >
            <Upload className="mr-2 h-4 w-4" />
            ファイルを選択
        </label>
        <input
            id={id}
            type="file"
            className="hidden"
            onChange={handleChange}
        />
        {selectedFile && (
            <span className="text-sm text-gray-700 mt-1">{selectedFile.name}</span>
        )}
        
    </div>
  );
}
