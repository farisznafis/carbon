# Monitoring Forms Architecture Guide

## Purpose

This system supports **multiple monitoring form types** (e.g. Solar, Aircon, etc.) while sharing:

* a **single registration page**
* a **single confirmation flow**
* consistent validation and UX
* safe navigation between *Form → Confirmation → Back*

This document explains:

1. **How the current setup works**
2. **Why it is designed this way**
3. **How to add a new monitoring form safely**

---

## High-Level Architecture

```
/monitoring/register/:projectType
        │
        ▼
MonitoringRegister (page)
        │
        ├─ loads form definition by projectType
        ├─ owns step state (form / confirm)
        ├─ owns final submitted data
        │
        ▼
<Project>Form (e.g. MonitoringSolarForm)
        │
        ├─ React Hook Form owns ALL input state
        ├─ Parent only receives data on submit
        │
        ▼
ConfirmationLayout
```

---

## Core Design Rules (DO NOT BREAK)

### 1. **React Hook Form owns the form**

* Inputs are NOT controlled by parent state
* Parent does NOT receive updates per keystroke

### 2. **Parent only syncs data on step transitions**

* `onNext(values)`
* `onBack()`
* `onConfirm()`

### 3. **No live syncing with `watch()`**

> ❌ Never push `watch()` values to parent
> ❌ Never reset form on every render

This causes render loops and breaks all input interaction.

---

## MonitoringRegister Responsibilities

### What it does

* Reads `projectType` from the URL
* Selects the correct form definition
* Controls step flow (`form` → `confirm`)
* Stores **submitted** form data
* Passes data back into the form when navigating backward

### What it does NOT do

* Does NOT manage individual field state
* Does NOT know about form internals
* Does NOT validate fields

---

## Example: MonitoringRegister (Simplified)

```tsx
const MonitoringRegister = () => {
  const { projectType } = useParams<{ projectType: ProjectType }>();

  const {
    Form,
    emptyData,
    getConfirmationItems,
  } = formDefinitions[projectType];

  const [step, setStep] = useState<"form" | "confirm">("form");
  const [formData, setFormData] = useState(emptyData);

  return (
    <>
      {step === "form" && (
        <Form
          mode="create"
          value={formData}
          onChange={setFormData}
          onNext={() => setStep("confirm")}
          onBack={() => navigate(-1)}
        />
      )}

      {step === "confirm" && (
        <ConfirmationLayout
          items={getConfirmationItems(formData)}
          onBack={() => setStep("form")}
          onConfirm={handleConfirm}
        />
      )}
    </>
  );
};
```

---

## Form Component Responsibilities

Each form component (e.g. `MonitoringSolarForm`) must:

* Own its own `useForm()`
* Use Shadcn `<Form />` normally
* Submit values **only when Next is pressed**

### Correct form pattern

```tsx
const form = useForm<FormData>({
  resolver: zodResolver(schema),
  defaultValues: value,
});
```

```tsx
<form
  onSubmit={form.handleSubmit((values) => {
    onChange(values); // ONE-TIME sync
    onNext();
  })}
>
```

### ❌ Forbidden patterns inside forms

```ts
// ❌ DO NOT DO THIS
const watched = form.watch();
useEffect(() => onChange(watched), [watched]);
```

```ts
// ❌ DO NOT reset on every render
useEffect(() => {
  form.reset(value);
}, [value]);
```

---

## Adding a New Monitoring Form

### Step 1: Define the form schema

```ts
export const MonitoringWindFormSchema = z.object({
  windPower: z.string().min(1),
  date: z.string().min(1),
});

export type MonitoringWindFormData =
  z.infer<typeof MonitoringWindFormSchema>;
```

---

### Step 2: Create the form component

```tsx
export function MonitoringWindForm({
  value,
  onChange,
  onNext,
  onBack,
}: MonitoringFormProps<MonitoringWindFormData>) {
  const form = useForm({
    resolver: zodResolver(MonitoringWindFormSchema),
    defaultValues: value,
  });

  return (
    <Form {...form}>
      <form
        onSubmit={form.handleSubmit((values) => {
          onChange(values);
          onNext();
        })}
      >
        {/* fields */}
      </form>
    </Form>
  );
}
```

---

### Step 3: Register the form definition

Add to `formDefinitions.ts`:

```ts
export const formDefinitions = {
  solar: {
    Form: MonitoringSolarForm,
    emptyData: emptySolarData,
    getConfirmationItems: solarConfirmationItems,
  },
  wind: {
    Form: MonitoringWindForm,
    emptyData: emptyWindData,
    getConfirmationItems: windConfirmationItems,
  },
};
```

---

### Step 4: Add route

```tsx
<Route
  path="/monitoring/register/:projectType"
  element={<MonitoringRegister />}
/>
```

---

## Confirmation Layout Contract

Each form must supply:

```ts
getConfirmationItems(data): ConfirmationItem[]
```

Example:

```ts
{
  label: "発電電力量",
  value: `${data.electricityUsage} kWh`,
  changed: data.electricityUsage !== original.electricityUsage
}
```

The confirmation screen is **form-agnostic**.

---

## Common Pitfalls (Read This Twice)

| Mistake                     | Result                |
| --------------------------- | --------------------- |
| Syncing watch() to parent   | Inputs freeze         |
| Resetting form in useEffect | Focus loss            |
| Parent controlling inputs   | Broken RHF            |
| Missing defaultValues       | Uncontrolled warnings |
| Form knowing projectType    | Tight coupling        |

---

## Why This Design Works

* Scales to **many project types**
* Keeps forms **independent**
* Prevents subtle React Hook Form bugs
* Makes confirmation reusable
* Supports back/forward navigation safely

---

## TL;DR Rules for Future Devs

> **If inputs stop working, you violated rule #1:
> React Hook Form must own the form.**

> **Only sync data on step transitions.**

