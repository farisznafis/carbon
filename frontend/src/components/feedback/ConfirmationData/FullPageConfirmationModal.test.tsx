// FullPageConfirmationModal.test.tsx
import { render, screen } from "@testing-library/react";
import { describe, it, expect, vi } from "vitest";
import { FullPageConfirmationModal } from "./FullPageConfirmationModal";

const baseProps = {
  open: true,
  title: "Confirm changes",
  description: "Review your data",
  onBack: vi.fn(),
  onConfirm: vi.fn(),
};

describe("FullPageConfirmationModal", () => {
  it("renders data entries", () => {
    render(
      <FullPageConfirmationModal
        {...baseProps}
        data={{ Name: "John", Age: "30" }}
      />
    );

    expect(screen.getByText("Name")).toBeDefined();
    expect(screen.getByText("John")).toBeDefined();
    expect(screen.getByText("Age")).toBeDefined();
    expect(screen.getByText("30")).toBeDefined();
  });

  it("underlines changed values when originalData differs", () => {
    const { container } = render(
      <FullPageConfirmationModal
        {...baseProps}
        data={{ Name: "John", Age: "31" }}
        originalData={{ Name: "John", Age: "30" }}
      />
    );

    const ageElement = screen.getByText("31");
    const nameElement = screen.getByText("John");

    expect(ageElement.className).toMatch(/underline/);
    expect(nameElement.className).not.toMatch(/underline/);

    expect(container).toMatchSnapshot();
  });

  it("does not underline when originalData is missing", () => {
    const value = "31";
    const el = render(
      <FullPageConfirmationModal
        {...baseProps}
        data={{ Age: value }}
      />
    ).getByText(value);

    expect(el.className).not.toMatch(/underline/);
  });
});