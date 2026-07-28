import React from "react";
import { render, screen, fireEvent } from "@testing-library/react";
import { FakeGraph, FakeGraphBar } from "./FakeGraphBar";
import { test, expect } from "vitest";

const data: FakeGraphBar[] = [
  { label: "A", value: 10, color: "#4F46E5", description: "Alpha" },
  { label: "B", value: 20, color: "#EC4899", description: "Bravo" },
];

test("renders labels under bars", () => {
  render(<FakeGraph data={data} />);
  expect(screen.getByTestId("bar-label-A")).toBeInTheDocument();
  expect(screen.getByTestId("bar-label-B")).toBeInTheDocument();
});

test("shows modal on hover with correct content", async () => {
  render(<FakeGraph data={data} />);

  // B のバーにホバー
  const barButtons = screen.getAllByRole("button");
  fireEvent.mouseEnter(barButtons[1]);

  const dialog = await screen.findByRole("dialog", {
    name: "B details",
  });

  expect(dialog).toBeInTheDocument();
  expect(dialog).toHaveTextContent("B");
  expect(dialog).toHaveTextContent("20");
  expect(dialog).toHaveTextContent("Bravo");
});

test("hides modal when mouse leaves", () => {
  render(<FakeGraph data={data} />);
  const barButtons = screen.getAllByRole("button");

  fireEvent.mouseEnter(barButtons[0]);
  const dialog = screen.getByRole("dialog", { name: "A details" });
  expect(dialog).toBeInTheDocument();

  fireEvent.mouseLeave(barButtons[0]);
  expect(
    screen.queryByRole("dialog", { name: "A details" })
  ).not.toBeInTheDocument();
});