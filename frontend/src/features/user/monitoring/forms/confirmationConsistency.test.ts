import { describe, it, expect } from "vitest";
import { formDefinitions } from "./formDefinitions";

describe("Monitoring confirmation consistency", () => {
    const projectTypes = Object.keys(formDefinitions);

    it("all project types are registered", () => {
        expect(projectTypes.length).toBeGreaterThan(0);
    });

    projectTypes.forEach((projectType) => {
        describe(`confirmation for project type: ${projectType}`, () => {
            const { emptyData, getConfirmationItems } =
                formDefinitions[projectType];

            it("returns confirmation items without crashing", () => {
                expect(() => {
                    getConfirmationItems(emptyData, emptyData);
                }).not.toThrow();
            });

            it("returns a non-empty list of confirmation items", () => {
                const items = getConfirmationItems(emptyData, emptyData);
                expect(items.length).toBeGreaterThan(0);
            });

            it("every confirmation item has label and value", () => {
                const items = getConfirmationItems(emptyData, emptyData);

                items.forEach((item) => {
                    expect(item).toHaveProperty("label");
                    expect(item).toHaveProperty("value");

                    expect(typeof item.label).toBe("string");
                    expect(typeof item.value).toBe("string");
                });
            });

            it("does not mark fields as changed when data is identical", () => {
                const items = getConfirmationItems(emptyData, emptyData);

                items.forEach((item) => {
                    expect(item.isChanged).not.toBe(true);
                });
            });

            it("confirmation values reflect modified data", () => {
                const modifiedData = {
                    ...structuredClone(emptyData),
                    dateRecorded: "__changed__",
                };

                const items = getConfirmationItems(modifiedData, emptyData);

                expect(items.some(i => i.value === "__changed__")).toBe(true);
                expect(items.some(i => i.isChanged)).toBe(true);
            });

        });
    });
});
