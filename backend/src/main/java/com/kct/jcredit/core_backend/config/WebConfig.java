package com.kct.jcredit.core_backend.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.kct.jcredit.core_backend.converter.StringToEquipmentFileTypeConverter;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private StringToEquipmentFileTypeConverter stringToEquipmentFileTypeConverter;

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter((Converter<?, ?>) stringToEquipmentFileTypeConverter);
    }
}