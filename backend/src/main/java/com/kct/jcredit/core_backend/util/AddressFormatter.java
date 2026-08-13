package com.kct.jcredit.core_backend.util;

/**
 * Bridges the company records' address columns to the shape the J-Credit forms use.
 *
 * <p>{@code operator_company} / {@code participating_company} split the address
 * after the city across {@code blocknum} and {@code building_name}, while the
 * official forms have a single 市区町村以降の住所 line.
 */
public final class AddressFormatter {

    private AddressFormatter() {}

    /**
     * Joins 番地 and 建物名 into one 市区町村以降の住所 line.
     *
     * @return the joined address, or {@code null} when both parts are empty
     */
    public static String joinAddressAfterCity(String blocknum, String buildingName) {
        String safeBlock = blocknum == null ? "" : blocknum.trim();
        String safeBuilding = buildingName == null ? "" : buildingName.trim();
        String joined = (safeBlock + " " + safeBuilding).trim();
        return joined.isEmpty() ? null : joined;
    }
}
