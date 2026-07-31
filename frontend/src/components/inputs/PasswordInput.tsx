import * as React from 'react';
import { Eye, EyeOff } from 'lucide-react';
import { Input } from '@/components/ui/input';
import { cn } from '@/lib/utils';

type PasswordInputProps = Omit<React.ComponentProps<typeof Input>, 'type'>;

const hasInputValue = (value: unknown) =>
  value !== undefined && value !== null && String(value).length > 0;

const PasswordInput = React.forwardRef<HTMLInputElement, PasswordInputProps>(
  (
    {
      className,
      disabled,
      onBlur,
      onChange,
      onFocus,
      value,
      defaultValue,
      ...props
    },
    ref,
  ) => {
    const [isFocused, setIsFocused] = React.useState(false);
    const [isVisible, setIsVisible] = React.useState(false);
    const [hasValue, setHasValue] = React.useState(
      hasInputValue(value) || hasInputValue(defaultValue),
    );
    const shouldShowToggle = !disabled && (isFocused || hasValue);
    const Icon = isVisible ? EyeOff : Eye;

    React.useEffect(() => {
      if (value !== undefined) {
        setHasValue(hasInputValue(value));
      }
    }, [value]);

    const handleFocus = (event: React.FocusEvent<HTMLInputElement>) => {
      setIsFocused(true);
      onFocus?.(event);
    };

    const handleBlur = (event: React.FocusEvent<HTMLInputElement>) => {
      setIsFocused(false);
      setHasValue(hasInputValue(event.currentTarget.value));
      onBlur?.(event);
    };

    const handleChange = (event: React.ChangeEvent<HTMLInputElement>) => {
      setHasValue(hasInputValue(event.currentTarget.value));
      onChange?.(event);
    };

    return (
      <div className='relative'>
        <Input
          ref={ref}
          type={isVisible ? 'text' : 'password'}
          className={cn('password-input-hide-native-toggle pr-10', className)}
          disabled={disabled}
          value={value}
          defaultValue={defaultValue}
          onBlur={handleBlur}
          onChange={handleChange}
          onFocus={handleFocus}
          {...props}
        />
        {shouldShowToggle && (
          <button
            type='button'
            aria-label={isVisible ? 'パスワードを非表示にする' : 'パスワードを表示する'}
            className='absolute right-2 top-1/2 inline-flex h-8 w-8 -translate-y-1/2 items-center justify-center rounded-md text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring focus-visible:ring-offset-2'
            onMouseDown={(event) => event.preventDefault()}
            onClick={() => setIsVisible((current) => !current)}
          >
            <Icon className='h-4 w-4' aria-hidden='true' />
          </button>
        )}
      </div>
    );
  },
);

PasswordInput.displayName = 'PasswordInput';

export { PasswordInput };
