# Form control size standard

Single-line data-entry controls use the shared `md` size (`48px`) by default.
The source of truth is `src/components/ui/form-control-size.ts`.

## Usage

- Use `Input`, `PasswordInput`, or `SelectTrigger` without a size prop in application forms.
- Change `FORM_CONTROL_SIZE_CLASSES.md` to adjust the product-wide form-field height and padding.
- Use `sm` only for intentionally compact utility controls, such as pagination or inline table controls.

## Exclusions

Textarea, checkbox, radio, OTP, file-drop areas, and icon-only controls keep sizes that match their interaction pattern. They are not single-line form controls and should not be forced to `48px`.
