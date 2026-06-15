import { describe, it, expect } from "vitest";

describe("dom", () => {
  it("has document", () => {
    expect(typeof document).toBe("object");
  });
});