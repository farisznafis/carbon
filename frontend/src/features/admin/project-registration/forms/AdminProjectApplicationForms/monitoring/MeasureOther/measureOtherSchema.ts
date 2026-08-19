import { z } from "zod";
import { measureSpecifiedSchema } from "../MeasureSpecified/measureSpecifiedSchema";

export const measureOtherSchema = measureSpecifiedSchema
    .omit({ validityPeriod: true, methodCollImg: true }) //remove methodCollImg once inserted
    .extend({
        instrumentCalibration: z.string(),
        monitoringPointDiagram: z.string().min(1, '必須項目です')
    })