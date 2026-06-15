// InlineHelp.test.tsx
import { describe, it, expect } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { InlineHelp } from "@/components/feedback/InlineHelp/InlineHelp";

describe("InlineHelp", () => {
  it("renders the question mark button", () => {
    render(<InlineHelp content="Help text" />);

    const button = screen.getByRole("button");
    expect(button).toBeInTheDocument();
    expect(button).toHaveTextContent("?");
  });

  it("opens dialog box on click", () => {
    render(<InlineHelp content="Help text" />);

    const button = screen.getByRole("button");
    fireEvent.click(button);

    expect(screen.getByRole("dialog")).toBeInTheDocument();
    expect(screen.getByText("Help text")).toBeInTheDocument();
  });

  it("toggles closed when clicking the button again", () => {
    render(<InlineHelp content="Help text" />);

    const button = screen.getByRole("button");
    fireEvent.click(button);
    expect(screen.getByRole("dialog")).toBeInTheDocument();

    fireEvent.click(button);
    expect(screen.queryByRole("dialog")).toBeNull();
  });

  it("closes when clicking outside (window click)", () => {
    render(<InlineHelp content="Help text" />);

    const button = screen.getByRole("button");
    fireEvent.click(button);
    expect(screen.getByRole("dialog")).toBeInTheDocument();

    // simulate outside click
    fireEvent.click(document.body);

    expect(screen.queryByRole("dialog")).toBeNull();
  });
});