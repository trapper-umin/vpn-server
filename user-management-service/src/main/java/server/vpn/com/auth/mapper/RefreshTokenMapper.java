package server.vpn.com.auth.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import server.vpn.com.auth.dto.SessionResponse;
import server.vpn.com.auth.entity.RefreshToken;

@Mapper(componentModel = "spring")
public interface RefreshTokenMapper {


    @Mapping(target = "isActive", expression = "java(!refreshToken.getIsRevoked() && !refreshToken.isExpired())")
    @Mapping(target = "isCurrent", source = "isCurrent")
    SessionResponse toSessionResponse(RefreshToken refreshToken, boolean isCurrent);
}
