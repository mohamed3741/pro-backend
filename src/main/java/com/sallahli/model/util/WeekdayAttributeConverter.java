package com.sallahli.model.util;


import com.sallahli.model.Enum.Weekday;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class WeekdayAttributeConverter implements AttributeConverter<Weekday, Short> {
    @Override
    public Short convertToDatabaseColumn(Weekday attribute) {
        return attribute == null ? null : attribute.dbValue;
    }

    @Override
    public Weekday convertToEntityAttribute(Short dbData) {
        return dbData == null ? null : Weekday.fromDb(dbData);
    }
}

