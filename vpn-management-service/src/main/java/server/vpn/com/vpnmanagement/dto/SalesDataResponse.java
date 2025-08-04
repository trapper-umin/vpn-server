package server.vpn.com.vpnmanagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesDataResponse {

    private LocalDate date;
    private BigDecimal revenue;
    private Integer subscribers;
    private Integer refunds;
}