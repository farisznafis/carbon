
import { mainActivitySchema } from "../MainActivity/mainActivitySchema";

export const incidentalActivitySchema = 
    mainActivitySchema.omit({ concept: true});