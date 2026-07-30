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

  it("highlights changed values when originalData differs", () => {
    const { container } = render(
      <FullPageConfirmationModal
        {...baseProps}
        data={{ Name: "John", Age: "31" }}
        originalData={{ Name: "John", Age: "30" }}
      />
    );

    const ageElement = screen.getByText("31");
    const nameElement = screen.getByText("John");

    expect(ageElement.className).toMatch(/bg-secondary\/60/);
    expect(ageElement.className).toMatch(/text-primary/);
    expect(ageElement.className).not.toMatch(/underline/);
    expect(nameElement.className).not.toMatch(/text-primary/);
    expect(screen.getByText('変更あり')).toBeInTheDocument();

    expect(container).toMatchSnapshot();
  });

  it("does not highlight when originalData is missing", () => {
    const value = "31";
    const el = render(
      <FullPageConfirmationModal
        {...baseProps}
        data={{ Age: value }}
      />
    ).getByText(value);

    expect(el.className).not.toMatch(/bg-secondary\/60/);
    expect(screen.queryByText('変更あり')).not.toBeInTheDocument();
  });
});
