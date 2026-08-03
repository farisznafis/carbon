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
            className="flex cursor-pointer items-center rounded-md bg-secondary px-3 py-1 text-sm text-secondary-foreground shadow-app-sm hover:bg-secondary/80"
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
            <span className="mt-1 text-sm text-foreground">{selectedFile.name}</span>
        )}
        
    </div>
  );
}
