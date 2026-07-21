// package com.kct.jcredit.core_backend.dto.request;

// import com.fasterxml.jackson.annotation.JsonProperty;

// import lombok.AllArgsConstructor;
// import lombok.Builder;
// import lombok.Data;
// import lombok.NoArgsConstructor;
 
// import jakarta.validation.Valid;
// import jakarta.validation.constraints.NotBlank;

// import java.util.List;
 
// @Data
// @Builder
// @NoArgsConstructor
// @AllArgsConstructor
// public class EquipmentRequest {
    
//     @NotBlank(message = "deviceNoを入力してください。")
//     @JsonProperty("deviceNo")
//     private String deviceNo;
    
//     @NotBlank(message = "powerCondStandByを入力してください。")
//     @JsonProperty("powerCondStandBy")
//     private String powerCondStandBy;
    
//     @NotBlank(message = "hasBatteryを入力してください。")
//     @JsonProperty("hasBattery")
//     private String hasBattery;
    
//     @JsonProperty("batteryMaker")
//     private String batteryMaker;
    
//     @JsonProperty("batteryModel")
//     private String batteryModel;
    
//     @JsonProperty("batteryCapacity")
//     private String batteryCapacity;
    
//     @JsonProperty("totalInvestment")
//     private String totalInvestment;
    
//     @JsonProperty("subsidyAmount")
//     private String subsidyAmount;
    
//     @JsonProperty("preProjectUnitPrice")
//     private String preProjectUnitPrice;
    
//     @Valid
//     @JsonProperty("files")
//     private List<FileInfoRequest> files;
// }