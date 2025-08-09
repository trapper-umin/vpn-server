package server.vpn.com.servermanagement.dto.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import server.vpn.com.servermanagement.dto.response.SalesDataResponse;
import server.vpn.com.servermanagement.entity.SalesRecord;

import java.util.List;

@Mapper(componentModel = "spring")
public interface SalesRecordMapper {

    @Mapping(target = "date", source = "saleDate")
    @Mapping(target = "subscribers", source = "newSubscribers")
    SalesDataResponse toSalesDataResponse(SalesRecord salesRecord);

    List<SalesDataResponse> toSalesDataResponseList(List<SalesRecord> salesRecords);
}