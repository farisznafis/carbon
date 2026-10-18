import { Component, ErrorInfo, ReactNode } from 'react';
import { logger } from '@/lib/logger';
import { NotFoundPage } from './NotFoundPage';

type ErrorBoundaryProps = {
  children: ReactNode;
  fallback?: ReactNode | ((controls: ErrorBoundaryControls) => ReactNode);
};

export type ErrorBoundaryControls = {
  resetErrorBoundary: () => void;
};

type ErrorBoundaryState = {
  hasError: boolean;
};

export class ErrorBoundary extends Component<
  ErrorBoundaryProps,
  ErrorBoundaryState
> {
  state: ErrorBoundaryState = {
    hasError: false,
  };

  static getDerivedStateFromError(): ErrorBoundaryState {
    return { hasError: true };
  }

  componentDidCatch(error: Error, errorInfo: ErrorInfo): void {
    logger.error('Unhandled React error', error, errorInfo);
  }

  private resetErrorBoundary = () => {
    this.setState({ hasError: false });
  };

  render() {
    if (this.state.hasError) {
      const { fallback } = this.props;

      if (fallback) {
        return typeof fallback === 'function'
          ? fallback({ resetErrorBoundary: this.resetErrorBoundary })
          : fallback;
      }

      return <NotFoundPage onBeforeNavigate={this.resetErrorBoundary} />;
    }

    return this.props.children;
  }
}
