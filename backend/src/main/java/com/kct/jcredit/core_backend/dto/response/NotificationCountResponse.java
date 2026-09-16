package com.kct.jcredit.core_backend.dto.response;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import com.kct.jcredit.core_backend.dto.response.base.BaseResponse;


@Data
@EqualsAndHashCode(callSuper = false)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationCountResponse extends BaseResponse{
    private Long unreadCount;
}

