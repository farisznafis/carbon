import { useEffect, useRef, useState, type FormEvent } from 'react';
import { Mail } from 'lucide-react';
import { ApiError } from '@/api/_shared/errors';
import { createUserInvitation } from '@/api/user-invitations';
import { FormActions } from '@/components/forms/form-layout';
import { RequiredBadge } from '@/components/forms/shared';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
} from '@/components/ui/dialog';
import { Input } from '@/components/ui/input';
import { Label } from '@/components/ui/label';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectSeparator,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { useToast } from '@/hooks/use-toast';
import { useSitesQuery } from '@/hooks/sites/useSitesQuery';

const NO_SITE_VALUE = '__no_site__';
const INVITATION_UNAVAILABLE_MESSAGE = 'ユーザー招待機能は現在利用できません。';

type InvitationField = 'email' | 'siteId';

type UserInvitationDialogProps = Readonly<{
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess?: () => void;
}>;

/** Converts one backend validation value into a displayable field message. */
function toFieldMessage(value: unknown): string {
  if (typeof value === 'string') return value;
  if (Array.isArray(value)) {
    return value.map(toFieldMessage).filter(Boolean).join('\n');
  }
  if (value && typeof value === 'object' && 'message' in value) {
    return toFieldMessage((value as { message?: unknown }).message);
  }
  return '';
}

/** Reads only the invitation fields supported by the master invitation form. */
function mapInvitationFieldErrors(
  error: ApiError,
): Partial<Record<InvitationField, string>> {
  const errors = error.payload?.errors;
  if (!errors || typeof errors !== 'object' || Array.isArray(errors)) return {};

  return Object.entries(errors).reduce<Partial<Record<InvitationField, string>>>(
    (mappedErrors, [fieldName, value]) => {
      if (fieldName === 'email' || fieldName === 'siteId') {
        const message = toFieldMessage(value);
        if (message) mappedErrors[fieldName] = message;
      }
      return mappedErrors;
    },
    {},
  );
}

/** Presents the reusable master flow for sending a user invitation email. */
export function UserInvitationDialog({
  open,
  onOpenChange,
  onSuccess,
}: UserInvitationDialogProps) {
  const { toast } = useToast();
  const sitesQuery = useSitesQuery();
  const submitStartedRef = useRef(false);
  const [email, setEmail] = useState('');
  const [siteId, setSiteId] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<
    Partial<Record<InvitationField, string>>
  >({});
  const [formError, setFormError] = useState<string | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    if (open) return;
    setEmail('');
    setSiteId(null);
    setFieldErrors({});
    setFormError(null);
  }, [open]);

  /** Keeps the transactional dialog open while its request is running. */
  const handleOpenChange = (nextOpen: boolean) => {
    if (isSubmitting) return;
    onOpenChange(nextOpen);
  };

  /** Sends the raw form values so all validation remains owned by the backend. */
  const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    if (submitStartedRef.current) return;

    submitStartedRef.current = true;
    setIsSubmitting(true);
    setFieldErrors({});
    setFormError(null);

    try {
      const response = await createUserInvitation({ email, siteId });
      toast({
        title: '招待メールを送信しました。',
        description: response.message || undefined,
        variant: 'success',
      });
      onSuccess?.();
      onOpenChange(false);
    } catch (error) {
      if (error instanceof ApiError) {
        const mappedErrors = mapInvitationFieldErrors(error);
        setFieldErrors(mappedErrors);
        setFormError(
          error.status === 404
            ? INVITATION_UNAVAILABLE_MESSAGE
            : error.message,
        );
      } else {
        setFormError(
          error instanceof Error
            ? error.message
            : '招待メールを送信できませんでした。',
        );
      }
    } finally {
      submitStartedRef.current = false;
      setIsSubmitting(false);
    }
  };

  return (
    <Dialog open={open} onOpenChange={handleOpenChange}>
      <DialogContent
        className="max-h-[calc(100dvh-2rem)] w-[calc(100vw-2rem)] max-w-xl overflow-y-auto rounded-lg"
        hideCloseButton={isSubmitting}
        onEscapeKeyDown={(event) => {
          if (isSubmitting) event.preventDefault();
        }}
        onPointerDownOutside={(event) => {
          if (isSubmitting) event.preventDefault();
        }}
      >
        <DialogHeader>
          <div className="mb-2 flex h-10 w-10 items-center justify-center rounded-lg bg-secondary text-primary-strong">
            <Mail className="h-5 w-5" aria-hidden="true" />
          </div>
          <DialogTitle>社員を招待</DialogTitle>
          <DialogDescription className="leading-6">
            招待メールを送信します。氏名やアカウント情報は、招待を受け取ったユーザーが入力します。
          </DialogDescription>
        </DialogHeader>

        <form className="space-y-6" noValidate onSubmit={handleSubmit}>
          {formError && (
            <p
              className="rounded-lg border border-destructive/20 bg-destructive/5 px-4 py-3 text-sm text-destructive-strong"
              role="alert"
            >
              {formError}
            </p>
          )}

          <div className="space-y-2">
            <Label
              htmlFor="invitation-email"
              className="inline-flex items-center gap-1 text-small"
            >
              メールアドレス <RequiredBadge />
            </Label>
            <Input
              id="invitation-email"
              type="email"
              aria-label="メールアドレス"
              value={email}
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={
                fieldErrors.email ? 'invitation-email-error' : undefined
              }
              onChange={(event) => {
                setEmail(event.target.value);
                setFieldErrors((current) => ({ ...current, email: undefined }));
              }}
              placeholder="user@example.com"
            />
            {fieldErrors.email && (
              <p
                id="invitation-email-error"
                className="text-small text-destructive-strong"
              >
                {fieldErrors.email}
              </p>
            )}
          </div>

          <div className="space-y-2">
            <Label htmlFor="invitation-site" className="text-small">
              拠点（任意）
            </Label>
            <Select
              value={siteId ?? NO_SITE_VALUE}
              onValueChange={(value) => {
                setSiteId(value === NO_SITE_VALUE ? null : value);
                setFieldErrors((current) => ({ ...current, siteId: undefined }));
              }}
              disabled={sitesQuery.isLoading || sitesQuery.isError}
            >
              <SelectTrigger
                id="invitation-site"
                aria-invalid={Boolean(fieldErrors.siteId)}
                aria-describedby={
                  fieldErrors.siteId ? 'invitation-site-error' : undefined
                }
              >
                <SelectValue placeholder="拠点を選択してください" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={NO_SITE_VALUE}>拠点を指定しない</SelectItem>
                <SelectSeparator
                  data-testid="invitation-site-separator"
                  aria-hidden="true"
                />
                {(sitesQuery.data?.sites ?? []).map((site) => (
                  <SelectItem key={site.siteId} value={site.siteId}>
                    {site.siteName}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
            {sitesQuery.isLoading && (
              <p className="text-small text-foreground">
                拠点を読み込み中です。
              </p>
            )}
            {sitesQuery.isError && (
              <p className="text-small text-destructive-strong">
                拠点を取得できませんでした。拠点を指定せずに送信できます。
              </p>
            )}
            {fieldErrors.siteId && (
              <p
                id="invitation-site-error"
                className="text-small text-destructive-strong"
              >
                {fieldErrors.siteId}
              </p>
            )}
          </div>

          <FormActions
            actions={[
              {
                id: 'cancel',
                label: 'キャンセル',
                variant: 'outline',
                onClick: () => handleOpenChange(false),
              },
              {
                id: 'submit',
                label: '招待メールを送信',
                pendingLabel: '送信中...',
                type: 'submit',
              },
            ]}
            pendingActionId={isSubmitting ? 'submit' : null}
          />
        </form>
      </DialogContent>
    </Dialog>
  );
}
