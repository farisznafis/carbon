import { describe, it, expect } from "vitest";
import { formDefinitions } from "./formDefinitions";
import { emptyFormData } from './solar/emptyFormData';
import { getMonitoringSolarConfirmation } from './solar/solarConfirmation';

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

describe('Solar battery confirmation', () => {
    it('shows battery details without a replacement selection for battery equipment', () => {
        const items = getMonitoringSolarConfirmation({
            ...emptyFormData,
            equipmentHasBattery: true,
            ELPJ_battery_value: 0,
            elpj_battery_evidence: 'battery.pdf',
        });

        expect(items).toEqual(expect.arrayContaining([
            expect.objectContaining({ label: '蓄電池放電量 (ELPJ)', value: '0' }),
            expect.objectContaining({ label: '蓄電池エビデンス', value: 'battery.pdf' }),
        ]));
        expect(items.map((item) => item.label)).not.toContain('蓄電池交換の有無');
    });

    it('omits battery details for equipment without a battery despite stale values', () => {
        const items = getMonitoringSolarConfirmation({
            ...emptyFormData,
            equipmentHasBattery: false,
            ELPJ_battery_value: 8,
            elpj_battery_evidence: 'battery.pdf',
        });

        const labels = items.map((item) => item.label);
        for (const label of ['蓄電池交換の有無', '蓄電池放電量 (ELPJ)', '蓄電池エビデンス']) {
            expect(labels).not.toContain(label);
        }
    });
});
