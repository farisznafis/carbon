interface Window {
  AwsWafCaptcha?: {
    renderCaptcha: (
      container: HTMLElement,
      configuration: {
        apiKey: string;
        onSuccess: (wafToken: string) => void;
        onError?: (error: Error) => void;
        defaultLocale?: string;
      },
    ) => void;
  };
}