import { useEffect } from 'react';

interface UseUnsavedChangesWarningProps {
  hasUnsavedChanges: boolean;
  message?: string;
}

export function useUnsavedChangesWarning({
  hasUnsavedChanges,
  message = '保存されていない変更があります。このページを離れてもよろしいですか？',
}: UseUnsavedChangesWarningProps) {
  
  useEffect(() => {
    const handleBeforeUnload = (e: BeforeUnloadEvent) => {
      if (hasUnsavedChanges) {
        e.preventDefault();
        e.returnValue = message;
        return message;
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);

    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [hasUnsavedChanges, message]);

  return { DialogComponent: null };
}
