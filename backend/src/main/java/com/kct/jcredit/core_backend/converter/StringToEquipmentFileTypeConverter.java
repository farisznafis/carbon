package com.kct.jcredit.core_backend.converter;

import org.springframework.stereotype.Component;
import com.kct.jcredit.core_backend.enums.EquipmentFileType;
import org.springframework.core.convert.converter.Converter; 

@Component
public class StringToEquipmentFileTypeConverter
        implements Converter<String, EquipmentFileType> {

    @Override
    public EquipmentFileType convert(String source) {
        return EquipmentFileType.fromValue(source);
    }
}