package com.citi.audit_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FieldChangeDTO {
    private String fieldName;
    private Object oldValue;
    private Object newValue;

    public static FieldChangeDTO from(String fieldName, Map<String, Object> changeMap) {
        return new FieldChangeDTO(
            fieldName,
            changeMap.get("old"),
            changeMap.get("new")
        );
    }
}

